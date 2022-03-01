package org.slorinc.dayzservermanager.configurations.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "app.rcon")
@ConstructorBinding
data class RconProperties(
    val host: String,
    val port: Int,
    val password: String
)