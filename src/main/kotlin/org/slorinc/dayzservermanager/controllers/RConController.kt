package org.slorinc.dayzservermanager.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.apache.logging.log4j.kotlin.Logging
import org.slorinc.dayzservermanager.services.BattlEyeRconClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@CrossOrigin
@RequestMapping("/rcon")
class RConController(val battlEyeRconClient: BattlEyeRconClient) : Logging {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Get player count", description = "Get player count")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "401", description = "You are not authorized access the resource", content = [Content(mediaType = MediaType.TEXT_HTML_VALUE)]),
            ApiResponse(responseCode = "404", description = "The resource not found", content = [Content(mediaType = MediaType.TEXT_HTML_VALUE)])]
    )
    fun getPlayerCount(): ResponseEntity<Int> {
        return ResponseEntity.ok().body(battlEyeRconClient.getPlayerCount())
    }

    fun littleEndianConversion(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() shl 8 * i)
        }
        return result
    }
}