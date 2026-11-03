package com.example.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnGateManagerTest {
    @Test
    fun parsesOnlyRowsWithRealNumericFeedValues() {
        val csv = """
            #HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
            valid.example,203.0.113.1,10,25,1200000,Example,EX,2,100,20,2000,2w,operator,message,Y2xpZW50
            invalid.example,203.0.113.2,10,unknown,1200000,Example,EX,2,100,20,2000,2w,operator,message,Y2xpZW50
        """.trimIndent()

        val servers = VpnGateManager.parseCsv(csv)

        assertEquals(1, servers.size)
        assertEquals("valid.example", servers.single().hostName)
        assertEquals(1.2, servers.single().speedMbps, 0.0)
    }

    @Test
    fun returnsNoInventedProfilesForEmptyFeed() {
        assertEquals(emptyList<VpnServer>(), VpnGateManager.parseCsv(""))
    }
}
