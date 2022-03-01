package org.slorinc.dayzservermanager.configurations.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "app.discord")
@ConstructorBinding
data class DiscordProperties(
    val apiUrl: String,
    val channelId: String,
    val botToken: String
)
