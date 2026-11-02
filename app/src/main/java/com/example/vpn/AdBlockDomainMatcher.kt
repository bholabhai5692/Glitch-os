package com.example.vpn

import java.net.IDN
import java.util.Locale

internal class AdBlockDomainMatcher private constructor(
    private val blockedDomains: DomainTrie,
    private val allowedDomains: DomainTrie
) {
    fun shouldBlock(host: String): Boolean {
        val normalized = normalizeDomain(host) ?: return false
        if (allowedDomains.matches(normalized)) return false
        return blockedDomains.matches(normalized)
    }

    companion object {
        fun fromRules(
            blockRules: Iterable<String>,
            allowRules: Iterable<String> = emptyList()
        ): AdBlockDomainMatcher {
            val blocked = DomainTrie()
            val allowed = DomainTrie()
            blockRules.mapNotNull(::parseRuleDomain).forEach(blocked::insert)
            allowRules.mapNotNull(::parseRuleDomain).forEach(allowed::insert)
            return AdBlockDomainMatcher(blocked, allowed)
        }

        fun default(): AdBlockDomainMatcher = fromRules(
            blockRules = listOf(
                "doubleclick.net",
                "googlesyndication.com",
                "googleadservices.com",
                "adservice.google.com",
                "adnxs.com",
                "adsrvr.org",
                "adform.net",
                "taboola.com",
                "outbrain.com",
                "scorecardresearch.com"
            )
        )

        private fun parseRuleDomain(rawRule: String): String? {
            var rule = rawRule.trim()
            if (rule.isEmpty() || rule.startsWith('!') || rule.startsWith('#')) return null

            if (rule.startsWith("0.0.0.0 ") || rule.startsWith("127.0.0.1 ")) {
                rule = rule.substringAfter(' ').trim().substringBefore(' ')
            } else if (rule.startsWith("@@||")) {
                rule = rule.substring(4)
            } else if (rule.startsWith("||")) {
                rule = rule.substring(2)
            } else if (rule.contains('/') || rule.contains('^') || rule.contains('$')) {
                return null
            }

            rule = rule.substringBefore('^')
            if (rule.contains('/') || rule.contains(':') || rule.contains('*')) return null
            return normalizeDomain(rule)
        }

        private fun normalizeDomain(host: String): String? {
            val candidate = host.trim().trimEnd('.')
            if (candidate.isEmpty() || candidate.length > 253 || candidate.contains(':')) return null
            if (candidate.matches(Regex("""\d{1,3}(?:\.\d{1,3}){3}"""))) return null

            val ascii = try {
                IDN.toASCII(candidate, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
            } catch (_: IllegalArgumentException) {
                return null
            }
            if (ascii.length > 253 || ascii.split('.').any { it.isEmpty() || it.length > 63 }) return null
            return ascii
        }
    }

    private class DomainTrie {
        private class Node {
            val children = mutableMapOf<String, Node>()
            var terminal = false
        }

        private val root = Node()

        fun insert(domain: String) {
            var node = root
            for (label in domain.split('.').asReversed()) {
                node = node.children.getOrPut(label) { Node() }
            }
            node.terminal = true
        }

        fun matches(domain: String): Boolean {
            var node = root
            for (label in domain.split('.').asReversed()) {
                node = node.children[label] ?: return false
                if (node.terminal) return true
            }
            return false
        }
    }
}
