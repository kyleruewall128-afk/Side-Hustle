package com.example.data

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val availableCents: Long = 0L,
    val lifetimeEarnedCents: Long = 0L,
    val totalWithdrawnCents: Long = 0L,
    val totalAdsWatched: Long = 0L,
    val totalWatchSeconds: Long = 0L,
    val dailyGoalCents: Long = 200L,
    val currentStreakDays: Long = 0L,
    val lastActiveDate: String = "",
    val preferredCategories: List<String> = emptyList(),
    val lastReferralClaimId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdViewRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val sponsorName: String = "",
    val category: String = "",
    val durationSeconds: Long = 0L,
    val rewardCents: Long = 0L,
    val dateKey: String = "",
    val hourOfDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DailyEarningsStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val earnedCents: Long = 0L,
    val adsWatched: Long = 0L,
    val watchSeconds: Long = 0L,
    val topCategory: String = "",
    val bonusCents: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PayoutRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amountCents: Long = 0L,
    val method: String = PayoutMethod.PAYPAL.name,
    val destinationLabel: String = "",
    val status: String = PayoutStatus.PROCESSING.name,
    val referenceCode: String = "",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedMethod(): PayoutMethod =
        enumValues<PayoutMethod>().firstOrNull { it.name == method } ?: PayoutMethod.PAYPAL

    fun parsedStatus(): PayoutStatus =
        enumValues<PayoutStatus>().firstOrNull { it.name == status } ?: PayoutStatus.PROCESSING
}

enum class PayoutMethod(
    val label: String,
    val subtitle: String,
    val destinationHint: String,
    val minCents: Long,
) {
    PAYPAL(
        label = "PayPal Instant",
        subtitle = "Zero fee • Arrives in minutes",
        destinationHint = "you@example.com",
        minCents = 100L,
    ),
    BANK_ACH(
        label = "Direct Bank (ACH)",
        subtitle = "Zero fee • 1–2 business days",
        destinationHint = "Checking •••• 4821",
        minCents = 500L,
    ),
    VENMO(
        label = "Venmo Transfer",
        subtitle = "Instant US transfer",
        destinationHint = "@your-handle",
        minCents = 100L,
    ),
    GIFT_CARD(
        label = "Digital Gift Card",
        subtitle = "+3% bonus value • Instant code",
        destinationHint = "Delivery email address",
        minCents = 100L,
    ),
}

enum class PayoutStatus(val label: String) {
    PROCESSING("Processing"),
    COMPLETED("Settled"),
    CANCELLED("Cancelled"),
}

enum class BankAccountType(val label: String) {
    CHECKING("Checking"),
    SAVINGS("Savings"),
}

@Keep
@IgnoreExtraProperties
data class LinkedPaymentMethod(
    @DocumentId val id: String = "",
    val userId: String = "",
    val method: String = PayoutMethod.PAYPAL.name,
    val accountHolderName: String = "",
    val maskedDestination: String = "",
    val bankName: String = "",
    val accountType: String = "",
    val accountLast4: String = "",
    val routingLast4: String = "",
    val autoPayoutEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedMethod(): PayoutMethod =
        enumValues<PayoutMethod>().firstOrNull { it.name == method } ?: PayoutMethod.PAYPAL

    fun parsedBankAccountType(): BankAccountType =
        enumValues<BankAccountType>().firstOrNull { it.name == accountType } ?: BankAccountType.CHECKING
}

const val APP_OWNER_EMAIL = "kyleruewall128@gmail.com"
const val USER_REWARD_SHARE_PERCENT = 40L
const val OWNER_REVENUE_SHARE_PERCENT = 60L

@Keep
@IgnoreExtraProperties
data class OwnerRevenueSummary(
    @DocumentId val id: String = "",
    val availableIncomeCents: Long = 0L,
    val lifetimeIncomeCents: Long = 0L,
    val withdrawnIncomeCents: Long = 0L,
    val grossSponsorCents: Long = 0L,
    val totalUserRewardsCents: Long = 0L,
    val totalVerifiedViews: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OwnerRevenueEntry(
    @DocumentId val id: String = "",
    val viewerUserId: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val category: String = "",
    val grossSponsorCents: Long = 0L,
    val userRewardCents: Long = 0L,
    val ownerIncomeCents: Long = 0L,
    val preferenceMatched: Boolean = false,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OwnerPayoutConfig(
    @DocumentId val id: String = "",
    val ownerEmail: String = APP_OWNER_EMAIL,
    val method: String = PayoutMethod.PAYPAL.name,
    val destinationLabel: String = APP_OWNER_EMAIL,
    val autoSweepEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedMethod(): PayoutMethod =
        enumValues<PayoutMethod>().firstOrNull { it.name == method } ?: PayoutMethod.PAYPAL
}

@Keep
@IgnoreExtraProperties
data class OwnerPayoutRecord(
    @DocumentId val id: String = "",
    val ownerEmail: String = APP_OWNER_EMAIL,
    val amountCents: Long = 0L,
    val method: String = PayoutMethod.PAYPAL.name,
    val destinationLabel: String = "",
    val status: String = PayoutStatus.PROCESSING.name,
    val referenceCode: String = "",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedMethod(): PayoutMethod =
        enumValues<PayoutMethod>().firstOrNull { it.name == method } ?: PayoutMethod.PAYPAL

    fun parsedStatus(): PayoutStatus =
        enumValues<PayoutStatus>().firstOrNull { it.name == status } ?: PayoutStatus.PROCESSING
}

data class AdCampaign(
    val id: String,
    val title: String,
    val sponsorName: String,
    val category: String,
    val durationSeconds: Int,
    val rewardCents: Long,
    val grossSponsorCents: Long,
    val dailyCap: Int,
    val headline: String,
    val highlights: List<String>,
    val ctaText: String,
    val badgeTag: String,
    @DrawableRes val imageRes: Int,
) {
    val platformMarginCents: Long
        get() = (grossSponsorCents - rewardCents).coerceAtLeast(0L)

    val effectiveHourlyCents: Long
        get() = if (durationSeconds > 0) (rewardCents * 3600L) / durationSeconds else 0L
}

data class AdWatchRewardOutcome(
    val campaignTitle: String,
    val rewardCents: Long,
    val bonusCents: Long,
    val referralBonusCents: Long = 0L,
    val newAvailableCents: Long,
    val goalReachedNow: Boolean,
)

object ReferralConstants {
    const val REQUIRED_ADS = 3L
    const val BONUS_CENTS = 50L
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val CODE_REGEX = Regex("^[A-Z0-9]{6,12}$")

    fun normalizeCode(raw: String): String =
        raw.trim().uppercase(Locale.US).replace(Regex("\\s+"), "")

    fun isValidCodeFormat(code: String): Boolean = CODE_REGEX.matches(code)

    fun codeForUser(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("sidehustle:$uid".toByteArray(Charsets.UTF_8))
        val suffix = buildString(6) {
            for (i in 0 until 6) {
                val idx = (digest[i].toInt() and 0xFF) % CODE_ALPHABET.length
                append(CODE_ALPHABET[idx])
            }
        }
        return "SH$suffix"
    }

    fun safeFirstName(rawName: String?, fallback: String = "Friend"): String {
        val cleaned = rawName.orEmpty()
            .trim()
            .substringBefore(" ")
            .replace(Regex("[^\\p{L}\\p{N}_\\-']"), "")
            .take(30)
        return cleaned.ifEmpty { fallback.take(30) }
    }
}

@Keep
@IgnoreExtraProperties
data class ReferralCode(
    @DocumentId val id: String = "",
    val code: String = "",
    val ownerUserId: String = "",
    val ownerFirstName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referralCode: String = "",
    val referrerUserId: String = "",
    val referrerFirstName: String = "",
    val inviteeUserId: String = "",
    val inviteeFirstName: String = "",
    val adsCompleted: Long = 0L,
    val requiredAds: Long = ReferralConstants.REQUIRED_ADS,
    val bonusCents: Long = ReferralConstants.BONUS_CENTS,
    val status: String = "PENDING",
    val inviteeCredited: Boolean = false,
    val referrerCredited: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isCompleted(): Boolean = status == "COMPLETED"
}

object AdCampaignCatalog {
    val preferenceCategories = listOf(
        "Finance",
        "Technology",
        "Gaming",
        "Fashion",
        "Travel",
        "Wellness",
    )

    val filterCategories = listOf("All") + preferenceCategories

    // Every campaign pays exactly 40% to the viewer and retains 60% net income for the app owner.
    val campaigns: List<AdCampaign> = listOf(
        AdCampaign(
            id = "camp_nordic_cabins",
            title = "Fjord Glass Eco Retreats",
            sponsorName = "NordicStay",
            category = "Travel",
            durationSeconds = 20,
            rewardCents = 24L,
            grossSponsorCents = 60L,
            dailyCap = 3,
            headline = "Solar-powered glass sanctuaries tucked deep inside pine valleys.",
            highlights = listOf(
                "100% off-grid geothermal heated suites",
                "Flexible 48-hour free cancellation",
                "Complimentary electric shuttle from rail hubs",
            ),
            ctaText = "View Winter Dates",
            badgeTag = "HIGH CPM",
            imageRes = R.drawable.img_ad_travel,
        ),
        AdCampaign(
            id = "camp_hyperarena_pass",
            title = "HyperArena Cloud Gaming Ultra",
            sponsorName = "HyperArena Interactive",
            category = "Gaming",
            durationSeconds = 18,
            rewardCents = 22L,
            grossSponsorCents = 55L,
            dailyCap = 4,
            headline = "Stream 144Hz ray-traced AAA titles directly on mobile with zero downloads.",
            highlights = listOf(
                "Ultra-low 12ms edge-server latency worldwide",
                "Full controller, keyboard & touch mapping",
                "Includes 140+ blockbuster & indie titles",
            ),
            ctaText = "Claim 14-Day Trial",
            badgeTag = "GAMING CPM",
            imageRes = R.drawable.img_hero_rewards,
        ),
        AdCampaign(
            id = "camp_novapay_metal",
            title = "NovaPay Titanium Cash Card",
            sponsorName = "NovaPay Financial",
            category = "Finance",
            durationSeconds = 15,
            rewardCents = 20L,
            grossSponsorCents = 50L,
            dailyCap = 4,
            headline = "Earn 3.5% APY on every dollar with zero monthly maintenance fees.",
            highlights = listOf(
                "Instant virtual card ready in 60 seconds",
                "Real-time spending notifications & vaults",
                "FDIC-insured partner balances up to $250K",
            ),
            ctaText = "Explore NovaPay Vaults",
            badgeTag = "TOP PAYOUT",
            imageRes = R.drawable.img_ad_fintech,
        ),
        AdCampaign(
            id = "camp_pulse_index",
            title = "VaultLine Micro-Investing",
            sponsorName = "VaultLine Capital",
            category = "Finance",
            durationSeconds = 18,
            rewardCents = 20L,
            grossSponsorCents = 50L,
            dailyCap = 4,
            headline = "Automate fractional ETF portfolios starting from $1 per deposit.",
            highlights = listOf(
                "Commission-free recurring index rebalancing",
                "Automated tax-loss harvesting built in",
                "Withdraw settled cash anytime with ACH",
            ),
            ctaText = "Start With $1",
            badgeTag = "SPONSORED",
            imageRes = R.drawable.img_hero_rewards,
        ),
        AdCampaign(
            id = "camp_atelier_outerwear",
            title = "Atelier Nord Merino Capsule",
            sponsorName = "Atelier Nord",
            category = "Fashion",
            durationSeconds = 15,
            rewardCents = 18L,
            grossSponsorCents = 45L,
            dailyCap = 4,
            headline = "Tailored recycled cashmere & weatherproof merino coats crafted in Oslo.",
            highlights = listOf(
                "Traceable zero-waste wool certification",
                "Lifetime seam & hardware repair guarantee",
                "Express carbon-neutral worldwide shipping",
            ),
            ctaText = "Shop Winter Drop",
            badgeTag = "RUNWAY PICK",
            imageRes = R.drawable.img_ad_travel,
        ),
        AdCampaign(
            id = "camp_vortex_esports",
            title = "Vortex Pro Haptic Controller",
            sponsorName = "Vortex Gear",
            category = "Gaming",
            durationSeconds = 14,
            rewardCents = 18L,
            grossSponsorCents = 45L,
            dailyCap = 5,
            headline = "Hall-effect zero-drift thumbsticks and 1000Hz tournament wireless polling.",
            highlights = listOf(
                "4 remappable rear aluminum paddles",
                "Direct USB-C passthrough charging dock",
                "Works natively on Android, PC & console",
            ),
            ctaText = "Configure Controller",
            badgeTag = "PRO GEAR",
            imageRes = R.drawable.img_ad_studio,
        ),
        AdCampaign(
            id = "camp_sonora_studio",
            title = "Sonora ANC Reference Studio",
            sponsorName = "Sonora Acoustics",
            category = "Technology",
            durationSeconds = 12,
            rewardCents = 16L,
            grossSponsorCents = 40L,
            dailyCap = 5,
            headline = "48-hour battery life with lossless spatial audio and machined aluminum.",
            highlights = listOf(
                "Adaptive hybrid noise cancellation (-45 dB)",
                "USB-C lossless DAC wired studio mode",
                "Replaceable memory-foam acoustic cushions",
            ),
            ctaText = "Hear the Difference",
            badgeTag = "QUICK WATCH",
            imageRes = R.drawable.img_ad_studio,
        ),
        AdCampaign(
            id = "camp_lumiere_chronograph",
            title = "Lumiere Solar Sapphire Watch",
            sponsorName = "Lumiere Horology",
            category = "Fashion",
            durationSeconds = 14,
            rewardCents = 16L,
            grossSponsorCents = 40L,
            dailyCap = 4,
            headline = "Swiss solar movement inside brushed grade-5 titanium and Italian leather.",
            highlights = listOf(
                "6-month reserve from a single hour of light",
                "Scratch-proof domed sapphire crystal",
                "Quick-release vegan & calfskin strap options",
            ),
            ctaText = "Preview Collection",
            badgeTag = "LUXURY",
            imageRes = R.drawable.img_ad_fintech,
        ),
        AdCampaign(
            id = "camp_keyforge_pro",
            title = "KeyForge Wireless Pro 75",
            sponsorName = "KeyForge Hardware",
            category = "Technology",
            durationSeconds = 12,
            rewardCents = 14L,
            grossSponsorCents = 35L,
            dailyCap = 5,
            headline = "Gasket-mounted acoustic switches engineered for creators and developers.",
            highlights = listOf(
                "Tri-mode Bluetooth 5.3, 2.4GHz & USB-C",
                "Hot-swappable lubricated tactile switches",
                "CNC anodized chassis in sage & obsidian",
            ),
            ctaText = "Customize Layout",
            badgeTag = "TECH PICK",
            imageRes = R.drawable.img_ad_studio,
        ),
        AdCampaign(
            id = "camp_zenith_sleep",
            title = "Circadia Recovery Ring",
            sponsorName = "Circadia Labs",
            category = "Wellness",
            durationSeconds = 10,
            rewardCents = 12L,
            grossSponsorCents = 30L,
            dailyCap = 6,
            headline = "Track HRV, deep sleep stages, and readiness without bulky wrist straps.",
            highlights = listOf(
                "2.9g aerospace titanium shell, 7-day battery",
                "Sub-degree skin temperature trend alerts",
                "No mandatory subscription for daily metrics",
            ),
            ctaText = "Check Sizing Kit",
            badgeTag = "10s EXPRESS",
            imageRes = R.drawable.img_ad_fintech,
        ),
    )

    fun findById(id: String): AdCampaign? = campaigns.firstOrNull { it.id == id }

    fun grossBidForCampaignId(campaignId: String): Long =
        findById(campaignId)?.grossSponsorCents ?: 40L
}

object Formatters {
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.US)
    private val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    fun formatCents(cents: Long): String {
        val negative = cents < 0
        val abs = kotlin.math.abs(cents)
        val dollars = abs / 100
        val rem = abs % 100
        val prefix = if (negative) "-$" else "$"
        return "$prefix$dollars.${rem.toString().padStart(2, '0')}"
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun todayDateKey(): String = dateKeyFormat.format(Date())

    fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateKeyFormat.format(cal.time)
    }

    fun currentHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    fun recentDateKeys(days: Int): List<String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        return List(days) {
            val key = dateKeyFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            key
        }
    }

    fun shortDayLabel(dateKey: String): String {
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: return dateKey.takeLast(2)
        return shortDayFormat.format(parsed)
    }

    fun friendlyDateLabel(dateKey: String): String {
        if (dateKey == todayDateKey()) return "Today"
        if (dateKey == yesterdayDateKey()) return "Yesterday"
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        return monthDayFormat.format(parsed)
    }

    fun formatTimestampTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        return timeFormat.format(date)
    }
}
