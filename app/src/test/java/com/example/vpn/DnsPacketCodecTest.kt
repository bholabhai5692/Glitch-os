package com.example.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DnsPacketCodecTest {
    @Test
    fun parsesDnsQueryAndBuildsNxdomainResponse() {
        val queryPacket = createQueryPacket("ads.example")
        val query = DnsPacketCodec.parseIpv4DnsQuery(queryPacket, queryPacket.size)

        assertNotNull(query)
        query!!
        assertEquals(listOf("ads.example"), query.questionNames)

        val response = DnsPacketCodec.blockedResponse(query)
        val ipHeaderLength = (response[0].toInt() and 0x0f) * 4
        val dnsOffset = ipHeaderLength + 8
        val responseFlags = ((response[dnsOffset + 2].toInt() and 0xff) shl 8) or
            (response[dnsOffset + 3].toInt() and 0xff)

        assertEquals(0x03, responseFlags and 0x0f)
        assertTrue(responseFlags and 0x8000 != 0)
        assertEquals(0, response[dnsOffset + 7].toInt() and 0xff)
        assertEquals(0xffff, ipv4Checksum(response, 20))
    }

    @Test
    fun rejectsPacketsNotDirectedAtTheDnsGateway() {
        val packet = createQueryPacket("ads.example")
        packet[19] = 2

        assertNull(DnsPacketCodec.parseIpv4DnsQuery(packet, packet.size))
    }

    private fun createQueryPacket(domain: String): ByteArray {
        val dns = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN)
        dns.putShort(0x1234)
        dns.putShort(0x0100)
        dns.putShort(1)
        dns.putShort(0)
        dns.putShort(0)
        dns.putShort(0)
        domain.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            dns.put(bytes.size.toByte())
            dns.put(bytes)
        }
        dns.put(0)
        dns.putShort(1)
        dns.putShort(1)
        val dnsMessage = dns.array().copyOf(dns.position())

        val packet = ByteArray(20 + 8 + dnsMessage.size)
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x45.toByte())
        buffer.put(0)
        buffer.putShort(packet.size.toShort())
        buffer.putShort(0x2468)
        buffer.putShort(0x4000)
        buffer.put(64)
        buffer.put(17)
        buffer.putShort(0)
        buffer.put(byteArrayOf(10, 111, 0xDE.toByte(), 2))
        buffer.put(byteArrayOf(10, 111, 0xDE.toByte(), 1))
        buffer.putShort(53000.toShort())
        buffer.putShort(53)
        buffer.putShort((8 + dnsMessage.size).toShort())
        buffer.putShort(0)
        buffer.put(dnsMessage)
        return packet
    }

    private fun ipv4Checksum(packet: ByteArray, headerLength: Int): Int {
        var sum = 0L
        for (index in 0 until headerLength step 2) {
            sum += ((packet[index].toInt() and 0xff) shl 8) or
                (packet[index + 1].toInt() and 0xff)
        }
        while (sum ushr 16 != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.toInt() and 0xffff
    }
}
