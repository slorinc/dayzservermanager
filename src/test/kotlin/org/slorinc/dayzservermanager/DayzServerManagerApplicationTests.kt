package org.slorinc.dayzservermanager

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.slorinc.dayzservermanager.services.BattlEyeRconClient
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DayzServerManagerApplicationTests {

    @MockkBean
    lateinit var battlEyeRconClient: BattlEyeRconClient

    @Test
    fun contextLoads() {
    }

}
