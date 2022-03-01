package org.slorinc.dayzservermanager

import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test
import org.slorinc.dayzservermanager.services.BattleyeRconClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.event.annotation.BeforeTestClass

@SpringBootTest
class DayzServerManagerApplicationTests {

    @MockK
    lateinit var battleyeRconClient: BattleyeRconClient

    @BeforeTestClass
    internal fun setUp() {
        every { battleyeRconClient.onStartUp() } returns Unit
    }

    @Test
    fun contextLoads() {
    }

}
