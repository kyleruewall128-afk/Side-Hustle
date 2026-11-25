package com.example

import com.example.data.AdCampaign
import com.example.data.AdCampaignCatalog
import com.example.data.Formatters
import com.example.data.OWNER_REVENUE_SHARE_PERCENT
import com.example.data.ReferralConstants
import com.example.data.USER_REWARD_SHARE_PERCENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPulseAnalyticsTest {

    @Test
    fun `formatCents formats dollar and cent values cleanly`() {
        assertEquals("$0.00", Formatters.formatCents(0L))
        assertEquals("$0.25", Formatters.formatCents(25L))
        assertEquals("$12.05", Formatters.formatCents(1205L))
    }

    @Test
    fun `preferred categories float matching campaigns to top of feed`() {
        val preferred = listOf("Gaming", "Fashion")
        val prefSet = preferred.toSet()
        val sorted = AdCampaignCatalog.campaigns.sortedWith(
            compareByDescending<AdCampaign> { it.category in prefSet }
                .thenByDescending { it.rewardCents }
        )

        val topFourCategories = sorted.take(4).map { it.category }
        assertTrue(topFourCategories.all { it in prefSet })
        assertEquals("Gaming", sorted.first().category)
    }

    @Test
    fun `every campaign pays 40 percent to viewer and retains 60 percent owner profit`() {
        AdCampaignCatalog.campaigns.forEach { campaign ->
            assertTrue(
                "Campaign ${campaign.id} should have positive platform margin",
                campaign.platformMarginCents > 0L
            )
            assertEquals(
                "Campaign ${campaign.id} should give users exactly 40% of gross revenue",
                campaign.grossSponsorCents * USER_REWARD_SHARE_PERCENT,
                campaign.rewardCents * 100L
            )
            assertEquals(
                "Campaign ${campaign.id} should retain exactly 60% owner profit",
                campaign.grossSponsorCents * OWNER_REVENUE_SHARE_PERCENT,
                campaign.platformMarginCents * 100L
            )
        }
    }

    @Test
    fun `referral code generation is deterministic unique and rule-compliant`() {
        val codeA1 = ReferralConstants.codeForUser("user_alpha_123")
        val codeA2 = ReferralConstants.codeForUser("user_alpha_123")
        val codeB = ReferralConstants.codeForUser("user_beta_456")

        assertEquals(codeA1, codeA2)
        assertNotEquals(codeA1, codeB)
        assertTrue(codeA1.startsWith("SH"))
        assertEquals(8, codeA1.length)
        assertTrue(ReferralConstants.isValidCodeFormat(codeA1))
        assertTrue(ReferralConstants.isValidCodeFormat(codeB))
    }

    @Test
    fun `referral code normalization and safe first name enforce constraints`() {
        assertEquals("SH9K2P4M", ReferralConstants.normalizeCode(" sh9k 2p4m "))
        assertTrue(ReferralConstants.isValidCodeFormat("SH9K2P4M"))
        assertFalse(ReferralConstants.isValidCodeFormat("SH12"))
        assertFalse(ReferralConstants.isValidCodeFormat("SH-1234"))

        assertEquals("Alice", ReferralConstants.safeFirstName("  Alice Johnson  "))
        assertEquals("Friend", ReferralConstants.safeFirstName("   ", fallback = "Friend"))
        assertEquals(30, ReferralConstants.safeFirstName("SuperLongFirstNameExceedingThirtyCharacters").length)
    }
}
