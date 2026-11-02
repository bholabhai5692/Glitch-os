package com.example.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockDomainMatcherTest {
    @Test
    fun blocksDomainsAndTheirSubdomainsWithoutMatchingLookalikes() {
        val matcher = AdBlockDomainMatcher.fromRules(listOf("||ads.example^"))

        assertTrue(matcher.shouldBlock("ads.example"))
        assertTrue(matcher.shouldBlock("cdn.ads.example."))
        assertFalse(matcher.shouldBlock("notads.example"))
        assertFalse(matcher.shouldBlock("example"))
    }

    @Test
    fun allowRulesTakePrecedenceForTheirDomainAndChildren() {
        val matcher = AdBlockDomainMatcher.fromRules(
            blockRules = listOf("||ads.example^"),
            allowRules = listOf("@@||news.ads.example^")
        )

        assertFalse(matcher.shouldBlock("news.ads.example"))
        assertFalse(matcher.shouldBlock("images.news.ads.example"))
        assertTrue(matcher.shouldBlock("cdn.ads.example"))
    }

    @Test
    fun normalizesInternationalizedDomainsAndIgnoresInvalidHosts() {
        val matcher = AdBlockDomainMatcher.fromRules(listOf("xn--bcher-kva.example"))

        assertTrue(matcher.shouldBlock("BÜCHER.example"))
        assertFalse(matcher.shouldBlock("bad host"))
        assertFalse(matcher.shouldBlock("127.0.0.1"))
    }

    @Test
    fun acceptsHostsFileEntries() {
        val matcher = AdBlockDomainMatcher.fromRules(
            listOf("0.0.0.0 ads.example", "127.0.0.1 tracker.example # tracker")
        )

        assertTrue(matcher.shouldBlock("ads.example"))
        assertTrue(matcher.shouldBlock("tracker.example"))
    }
}
