package org.slorinc.dayzservermanager.services

import org.apache.logging.log4j.kotlin.Logging
import org.slorinc.dayzservermanager.integrations.DiscordGateway
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DiscordUpdaterService(
    val battlEyeRconClient: BattleyeRconClient,
    val discordGateway: DiscordGateway
): Logging {

    @Scheduled(cron = "0 */5 * * * *")
    fun updatePlayerCount(){
        val playerCount = battlEyeRconClient.getPlayerCount();
        discordGateway.updatePlayerCount(playerCount = playerCount)
        logger.debug("Discord updated.")
    }
}