package org.slorinc.dayzservermanager.integrations

import org.apache.logging.log4j.kotlin.Logging
import org.slorinc.dayzservermanager.configurations.properties.DiscordProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class DiscordGateway(val discordProperties: DiscordProperties, val restTemplateBuilder: RestTemplateBuilder) : Logging {

    private val restTemplate: RestTemplate = restTemplateBuilder
        .requestFactory { OkHttp3ClientHttpRequestFactory() }
        .build()

    fun updatePlayerCount(playerCount: Int) {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, "Bot ${discordProperties.botToken}")
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

        val discordChannelUpdateRequest = DiscordChannelUpdateRequest("Namalsk: $playerCount")
        val httpRequestEntity: HttpEntity<DiscordChannelUpdateRequest> =
            HttpEntity(discordChannelUpdateRequest, headers)

        restTemplate.exchange(
            "${discordProperties.apiUrl}/channels/{channelId}",
            HttpMethod.PATCH,
            httpRequestEntity,
            DiscordChannelUpdateResponse::class.java,
            mapOf("channelId" to discordProperties.channelId)
        )

        logger.debug("Discord Modify Channel call successful")
    }


}

data class DiscordChannelUpdateResponse(val id: String)

data class DiscordChannelUpdateRequest(val name: String)
