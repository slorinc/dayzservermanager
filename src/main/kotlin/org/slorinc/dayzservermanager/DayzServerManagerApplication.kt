package org.slorinc.dayzservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
class DayzServerManagerApplication

fun main(args: Array<String>) {
    runApplication<DayzServerManagerApplication>(*args)
}
