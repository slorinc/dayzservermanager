package org.slorinc.dayzservermanager.services

import org.apache.logging.log4j.kotlin.Logging
import org.apache.logging.log4j.util.Strings
import org.slorinc.dayzservermanager.configurations.properties.RconProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.retry.RetryCallback
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32


@Service
class BattlEyeRconClient(
    val rconProperties: RconProperties
) : Logging {

    private final val socket = DatagramSocket()
    private final val sequenceNumber = AtomicInteger(-1);
    val address: InetAddress = InetAddress.getByName(rconProperties.host)
    val crc = CRC32()
    val packetBuffer: ByteBuffer = ByteBuffer.allocate(32768)!!
    val receivePacketBuffer: ByteBuffer = ByteBuffer.allocate(32768)!!

    val commandResponses: MutableMap<Int, ByteArray> = hashMapOf()
    val messageResponses: MutableMap<Int, String> = hashMapOf()
    var loginCommandResponse: ByteArray = byteArrayOf()
    val retryTemplate: RetryTemplate = RetryTemplate.builder()
        .exponentialBackoff(
            250,
            2.5,
            5000
        )
        .maxAttempts(5)
        .retryOn(NoSuchElementException::class.java)
        .build();


    @EventListener(ApplicationReadyEvent::class)
    fun onStartUp() {
        login(rconProperties.password)
    }

    /**
     * An empty 2-byte command packet (without actual command string) has to be sent every 45 seconds (or less)
     * to keep the connection/login alive, if there are no other command packets being sent.
     * If there are no command packets coming from the client for more than 45 seconds,
     * it will be removed from BE RCon's list of authenticated clients and will no longer be able to issue any commands.
     */
    @Scheduled(initialDelay = 40000, fixedDelay = 40000)
    fun keepAlive() {
        logger.debug("Sending Keep Alive command.")
        val keepAliveSeqNum = incrementAndGetSequenceNumber()
        executeCommand(Strings.EMPTY, keepAliveSeqNum)
        try {
            retryTemplate.execute(
                RetryCallback<ByteArray, NoSuchElementException> {
                    getCommandResponse(keepAliveSeqNum)
                }
            )
        } catch (e: NoSuchElementException) {
            logger.warn("Keep Alive call failed. Trying to reconnect.")
            login(rconProperties.password)
            return
        }
        logger.debug("Keep Alive packet confirmed by server.")
    }

    @Scheduled(fixedDelay = 200)
    fun receivePacket() {
        receivePacketBuffer.clear()
        val receivedResponsePacket = DatagramPacket(receivePacketBuffer.array(), receivePacketBuffer.array().size)

        logger.debug("Fetching packet...")
        try {
            socket.receive(receivedResponsePacket)
        } catch (e: SocketTimeoutException) {
            return
        }

        val packedSeqNumber = receivedResponsePacket.data[8].toInt()

        if (receivedResponsePacket.data[7] == 0x02.toByte()) { // message
            logger.debug("Message received.")
            messageResponses[packedSeqNumber] =
                String(
                    receivedResponsePacket.data.copyOfRange(9, receivedResponsePacket.length),
                    StandardCharsets.US_ASCII
                )

            logger.debug("Messages added with sequence number $packedSeqNumber and content  ${messageResponses[packedSeqNumber]}.")

            acknowledgeMessage(packedSeqNumber)

        } else if (receivedResponsePacket.data[7] == 0x01.toByte()) { // command response received
            logger.debug("Command packet received")
            commandResponses[packedSeqNumber] = receivedResponsePacket.data.copyOfRange(0, receivedResponsePacket.length)

        } else if (receivedResponsePacket.data[7] == 0x00.toByte()) { // login response received
            logger.debug("Login response received")
            loginCommandResponse = receivedResponsePacket.data.copyOfRange(0, receivedResponsePacket.length)
        } else {
            logger.debug("Unexpected packet received")
        }
    }

    fun getPlayerCount(): Int {
        val seqNum = incrementAndGetSequenceNumber()
        executeCommand("players", seqNum)

        val response: ByteArray = retryTemplate.execute(
            RetryCallback<ByteArray, NoSuchElementException> {
                getCommandResponse(seqNum)
            }
        )

        val regex = "^\\((\\d+)".toRegex(RegexOption.MULTILINE)
        val payload = String(
            response.copyOfRange(9, response.size), StandardCharsets.US_ASCII
        )
        val matchResult = regex.find(payload)
        val (playerCount) = matchResult!!.destructured
        logger.trace("Player count command response received: $payload")
        return Integer.parseInt(playerCount)
    }

    fun executeCommand(command: String, sequenceNumber: Int) {
        val commandBuffer: ByteBuffer = ByteBuffer.allocate(
            7 + // header
                    1 + // sequence number
                    1 + // Command command
                    command.length // payload
        )
        commandBuffer.order(ByteOrder.LITTLE_ENDIAN)
        commandBuffer.put('B'.code.toByte()) // header starts
        commandBuffer.put('E'.code.toByte())
        commandBuffer.position(6) // skip checksum
        commandBuffer.put(0xFF.toByte()) // header end
        commandBuffer.put(0x01.toByte()) // command
        commandBuffer.put(sequenceNumber.toByte()) // sequence number

        commandBuffer.put(command.toByteArray(StandardCharsets.US_ASCII)) // command (ASCII string without null-terminator)
        crc.reset()
        crc.update(commandBuffer.array(), 6, commandBuffer.position() - 6)
        val commandChecksum = crc.value.toInt()
        commandBuffer.putInt(2, commandChecksum)
        commandBuffer.flip()
        logger.debug("Command package prepared, sequenceNumber: $sequenceNumber, length: ${commandBuffer.array().size} body: ${String(commandBuffer.array(), StandardCharsets.US_ASCII)}")

        val commandPacket = DatagramPacket(commandBuffer.array(), commandBuffer.array().size, address, rconProperties.port)
        socket.send(commandPacket)
        logger.debug("Command \"$command\" sent...")
    }

    fun acknowledgeMessage(sequenceNumber: Int) {
        logger.debug("Sending acknowledgement for sequence number $sequenceNumber")
        val commandBuffer: ByteBuffer = ByteBuffer.allocate(
            7 + // header
                    1 + // sequence number
                    1 // command command
        )
        commandBuffer.order(ByteOrder.LITTLE_ENDIAN)
        commandBuffer.put('B'.code.toByte()) // header starts
        commandBuffer.put('E'.code.toByte())
        commandBuffer.position(6) // skip checksum
        commandBuffer.put(0xFF.toByte()) // header end
        commandBuffer.put(0x02.toByte()) // ack command
        commandBuffer.put(sequenceNumber.toByte()) // sequence number

        crc.reset()
        crc.update(commandBuffer.array(), 6, commandBuffer.position() - 6)
        val commandChecksum = crc.value.toInt()
        commandBuffer.putInt(2, commandChecksum)
        commandBuffer.flip()
        logger.debug("Acknowledgement package prepared, sequenceNumber: $sequenceNumber, length: ${commandBuffer.array().size} body: ${String(commandBuffer.array(), StandardCharsets.US_ASCII)}")

        val commandPacket = DatagramPacket(commandBuffer.array(), commandBuffer.array().size, address, rconProperties.port)
        socket.send(commandPacket)
        logger.debug("Acknowledgement is sent...")
    }

    final fun login(password: String) {
        packetBuffer.clear()
        packetBuffer.order(ByteOrder.LITTLE_ENDIAN)
        packetBuffer.put('B'.code.toByte()) // header starts
        packetBuffer.put('E'.code.toByte())
        packetBuffer.position(6) // skip checksum
        packetBuffer.put(0xFF.toByte()) // header end
        packetBuffer.put(0x00.toByte()) //login

        packetBuffer.put(password.toByteArray(StandardCharsets.US_ASCII)) // password (ASCII string without null-terminator)

        packetBuffer.putInt(2, calculateChecksum(packetBuffer.array(), 6, packetBuffer.position() - 6))
        packetBuffer.flip()

        val packet = DatagramPacket(packetBuffer.array(), packetBuffer.limit(), address, rconProperties.port)
        logger.debug("Login package prepared, length: ${packetBuffer.limit()} body: ${String(packet.data.copyOfRange(0, packet.length))}")

        socket.send(packet)
        logger.debug("Login package sent.")

        val response: ByteArray = retryTemplate.execute(
            RetryCallback<ByteArray, NoSuchElementException> {
                getLoginResponse()
            }
        )

        val calculatedCRC = calculateChecksum(response, 6, response.size - 6)

        val crcBytes = response.copyOfRange(2, 6)
        crcBytes.reverse()
        val receivedCRC = ByteBuffer.wrap(crcBytes).int

        if (calculatedCRC != receivedCRC) {
            logger.warn("CRC does not match.")
        } else {
            logger.debug("CRC correct.")
        }

        if (response[8] == 0x01.toByte()) {
            logger.debug("Successfully logged in")
        }
        sequenceNumber.set(0)
        // login end
    }

    private fun calculateChecksum(payload: ByteArray, payloadOffset: Int, payloadLength: Int): Int {
        crc.reset()
        crc.update(payload, payloadOffset, payloadLength)
        return crc.value.toInt()
    }

    fun incrementAndGetSequenceNumber(): Int {
        val seqNumber = sequenceNumber.getAndIncrement()
        if ( seqNumber == 0x0F) {
            logger.debug("Sequence number limit reached, resetting to zero.")
            sequenceNumber.set(0)
        }
        return seqNumber
    }

    fun getCommandResponse(sequenceNumber: Int): ByteArray? {
        logger.debug("Getting command response for $sequenceNumber.")
        if (commandResponses.contains(sequenceNumber)) {
            val responseBytes = commandResponses[sequenceNumber]
            commandResponses.remove(sequenceNumber)
            return responseBytes
        } else {
            throw NoSuchElementException()
        }
    }

    fun getMessageResponse(sequenceNumber: Int): String {
        logger.debug("Getting message response for $sequenceNumber.")
        if (messageResponses.containsKey(sequenceNumber)) {
            val message = messageResponses[sequenceNumber]
            messageResponses.remove(sequenceNumber)
            return message!!

        } else {
            throw NoSuchElementException()
        }
    }

    fun getLoginResponse(): ByteArray {
        logger.debug("Getting login response.")
        if (loginCommandResponse.isNotEmpty()) {
            val response = loginCommandResponse
            loginCommandResponse = byteArrayOf()
            return response
        } else {
            throw NoSuchElementException()
        }
    }


    enum class ResponseType {
        MESSAGE,
        COMMAND
    }
}