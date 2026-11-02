package com.example.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class DnsPacketQuery(
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray,
    val sourcePort: Int,
    val ipIdentification: Int,
    val dnsMessage: ByteArray,
    val questionNames: List<String>,
    val questionEnd: Int
)

internal object DnsPacketCodec {
    private const val IPV4_UDP = 17
    private const val DNS_PORT = 53
    private const val MAX_DNS_PAYLOAD = 1_472
    private val DNS_GATEWAY = byteArrayOf(10, 111, 0xDE.toByte(), 1)

    fun parseIpv4DnsQuery(packet: ByteArray, length: Int): DnsPacketQuery? {
        if (length < 28 || length > packet.size) return null
        if ((packet[0].toInt() ushr 4 and 0x0f) != 4) return null

        val ipHeaderLength = (packet[0].toInt() and 0x0f) * 4
        if (ipHeaderLength < 20 || length < ipHeaderLength + 8) return null

        val totalLength = readU16(packet, 2)
        if (totalLength < ipHeaderLength + 8 || totalLength > length) return null
        if ((packet[9].toInt() and 0xff) != IPV4_UDP) return null

        val fragmentFlags = readU16(packet, 6)
        if (fragmentFlags and 0x3fff != 0) return null

        val sourceAddress = packet.copyOfRange(12, 16)
        val destinationAddress = packet.copyOfRange(16, 20)
        if (!destinationAddress.contentEquals(DNS_GATEWAY)) return null

        val udpOffset = ipHeaderLength
        val sourcePort = readU16(packet, udpOffset)
        if (readU16(packet, udpOffset + 2) != DNS_PORT) return null
        val udpLength = readU16(packet, udpOffset + 4)
        if (udpLength < 20 || udpOffset + udpLength > totalLength) return null

        val dnsMessage = packet.copyOfRange(udpOffset + 8, udpOffset + udpLength)
        if (dnsMessage.size !in 12..MAX_DNS_PAYLOAD) return null
        if ((readU16(dnsMessage, 2) and 0x8000) != 0) return null
        val questionCount = readU16(dnsMessage, 4)
        if (questionCount == 0 || questionCount > 32) return null

        val names = ArrayList<String>(questionCount)
        var cursor = 12
        repeat(questionCount) {
            val parsedName = readName(dnsMessage, cursor) ?: return null
            val questionEnd = parsedName.second + 4
            if (questionEnd > dnsMessage.size) return null
            names += parsedName.first
            cursor = questionEnd
        }
        if (cursor > MAX_DNS_PAYLOAD) return null

        return DnsPacketQuery(
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            sourcePort = sourcePort,
            ipIdentification = readU16(packet, 4),
            dnsMessage = dnsMessage,
            questionNames = names,
            questionEnd = cursor
        )
    }

    fun blockedResponse(query: DnsPacketQuery): ByteArray = dnsResponsePacket(
        query,
        createErrorDnsMessage(query, responseCode = 3)
    )

    fun servfailResponse(query: DnsPacketQuery): ByteArray = dnsResponsePacket(
        query,
        createErrorDnsMessage(query, responseCode = 2)
    )

    fun responsePacket(query: DnsPacketQuery, dnsMessage: ByteArray): ByteArray =
        dnsResponsePacket(query, dnsMessage)

    fun isValidDnsResponse(query: DnsPacketQuery, response: ByteArray): Boolean =
        response.size in 12..MAX_DNS_PAYLOAD &&
            response[0] == query.dnsMessage[0] &&
            response[1] == query.dnsMessage[1] &&
            (response[2].toInt() and 0x80) != 0 &&
            readU16(response, 4) == readU16(query.dnsMessage, 4)

    private fun createErrorDnsMessage(query: DnsPacketQuery, responseCode: Int): ByteArray {
        val response = query.dnsMessage.copyOf(query.questionEnd)
        val requestFlags = readU16(query.dnsMessage, 2)
        val flags = (requestFlags and 0x7900) or 0x8080 or responseCode
        writeU16(response, 2, flags)
        writeU16(response, 6, 0)
        writeU16(response, 8, 0)
        writeU16(response, 10, 0)
        return response
    }

    private fun dnsResponsePacket(query: DnsPacketQuery, dnsMessage: ByteArray): ByteArray {
        val udpLength = 8 + dnsMessage.size
        val totalLength = 20 + udpLength
        require(totalLength <= 65_535) { "DNS response exceeds IPv4 packet size" }

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x45.toByte())
        buffer.put(0)
        buffer.putShort(totalLength.toShort())
        buffer.putShort(query.ipIdentification.toShort())
        buffer.putShort(0x4000.toShort())
        buffer.put(64)
        buffer.put(IPV4_UDP.toByte())
        buffer.putShort(0)
        buffer.put(query.destinationAddress)
        buffer.put(query.sourceAddress)
        writeU16(packet, 10, ipv4Checksum(packet, 20))

        writeU16(packet, 20, DNS_PORT)
        writeU16(packet, 22, query.sourcePort)
        writeU16(packet, 24, udpLength)
        writeU16(packet, 26, 0)
        dnsMessage.copyInto(packet, destinationOffset = 28)
        return packet
    }

    private fun readName(message: ByteArray, start: Int): Pair<String, Int>? {
        val labels = mutableListOf<String>()
        val visited = mutableSetOf<Int>()
        var cursor = start
        var endCursor = -1

        while (cursor < message.size && visited.add(cursor)) {
            val length = message[cursor].toInt() and 0xff
            when {
                length == 0 -> {
                    if (endCursor < 0) endCursor = cursor + 1
                    return labels.joinToString(".") to endCursor
                }
                length and 0xc0 == 0xc0 -> {
                    if (cursor + 1 >= message.size) return null
                    val pointer = ((length and 0x3f) shl 8) or (message[cursor + 1].toInt() and 0xff)
                    if (pointer >= message.size) return null
                    if (endCursor < 0) endCursor = cursor + 2
                    cursor = pointer
                }
                length and 0xc0 != 0 || length > 63 -> return null
                else -> {
                    if (cursor + 1 + length > message.size) return null
                    labels += message.copyOfRange(cursor + 1, cursor + 1 + length)
                        .toString(Charsets.US_ASCII)
                    cursor += length + 1
                }
            }
            if (labels.sumOf { it.length + 1 } > 253) return null
        }
        return null
    }

    private fun ipv4Checksum(packet: ByteArray, headerLength: Int): Int {
        var sum = 0L
        var index = 0
        while (index < headerLength) {
            sum += readU16(packet, index)
            index += 2
        }
        while (sum ushr 16 != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }
}
