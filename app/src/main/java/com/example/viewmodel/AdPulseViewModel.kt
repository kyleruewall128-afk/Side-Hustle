package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.APP_OWNER_EMAIL
import com.example.data.AdCampaign
import com.example.data.AdCampaignCatalog
import com.example.data.AdPulseRepository
import com.example.data.AdViewRecord
import com.example.data.AdWatchRewardOutcome
import com.example.data.BankAccountType
import com.example.data.DailyEarningsStat
import com.example.data.Formatters
import com.example.data.LinkedPaymentMethod
import com.example.data.OwnerPayoutConfig
import com.example.data.OwnerPayoutRecord
import com.example.data.OwnerRevenueEntry
import com.example.data.OwnerRevenueSummary
import com.example.data.PayoutMethod
import com.example.data.PayoutRecord
import com.example.data.ReferralConstants
import com.example.data.ReferralRecord
import com.example.data.UiState
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AnalyticsRange(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7D"),
    FOURTEEN_DAYS(14, "14D"),
    THIRTY_DAYS(30, "30D"),
}

data class DailyChartPoint(
    val dateKey: String,
    val shortLabel: String,
    val friendlyLabel: String,
    val earnedCents: Long,
    val bonusCents: Long,
    val adsWatched: Long,
    val watchSeconds: Long,
    val topCategory: String,
)

data class CategoryBreakdownItem(
    val category: String,
    val earnedCents: Long,
    val adsWatched: Int,
    val shareFraction: Float,
)

data class TimeWindowStat(
    val label: String,
    val windowHours: String,
    val earnedCents: Long,
    val adsWatched: Int,
)

data class DailyAnalyticsSummary(
    val todayDateKey: String,
    val todayEarnedCents: Long,
    val todayBonusCents: Long,
    val todayAdsWatched: Long,
    val todayWatchSeconds: Long,
    val dailyGoalCents: Long,
    val dailyGoalProgress: Float,
    val rangeTotalCents: Long,
    val rangeDailyAvgCents: Long,
    val effectiveHourlyCents: Long,
    val avgRewardPerAdCents: Long,
    val bestDay: DailyChartPoint?,
    val chartPoints: List<DailyChartPoint>,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val timeWindows: List<TimeWindowStat>,
    val grossSponsorCents: Long,
    val netPlatformMarginCents: Long,
    val targetedMatchRatePercent: Int,
)

data class DashboardData(
    val profile: UserProfile,
    val adViews: List<AdViewRecord>,
    val dailyStats: List<DailyEarningsStat>,
    val payouts: List<PayoutRecord>,
    val todayViewsByCampaign: Map<String, Int>,
    val linkedPaymentMethod: LinkedPaymentMethod? = null,
    val myReferralCode: String = "",
    val incomingReferral: ReferralRecord? = null,
    val outgoingReferrals: List<ReferralRecord> = emptyList(),
)

private data class CoreDashboardSnapshot(
    val profile: UserProfile?,
    val views: List<AdViewRecord>,
    val stats: List<DailyEarningsStat>,
    val payouts: List<PayoutRecord>,
    val linkedPaymentMethod: LinkedPaymentMethod?,
)

data class OwnerConsoleData(
    val summary: OwnerRevenueSummary,
    val entries: List<OwnerRevenueEntry>,
    val payoutConfig: OwnerPayoutConfig,
    val payouts: List<OwnerPayoutRecord>,
)

data class ActiveWatchSession(
    val campaign: AdCampaign,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isReadyToClaim: Boolean,
    val isSubmitting: Boolean,
)

class AdPulseViewModel(
    private val repository: AdPulseRepository,
    private val currentUserId: String,
    private val isVerifiedAppOwner: Boolean = false,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _analyticsRange = MutableStateFlow(AnalyticsRange.SEVEN_DAYS)
    val analyticsRange: StateFlow<AnalyticsRange> = _analyticsRange.asStateFlow()

    private val _selectedDateKey = MutableStateFlow(Formatters.todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _activeWatchSession = MutableStateFlow<ActiveWatchSession?>(null)
    val activeWatchSession: StateFlow<ActiveWatchSession?> = _activeWatchSession.asStateFlow()

    private val _rewardOutcome = MutableStateFlow<AdWatchRewardOutcome?>(null)
    val rewardOutcome: StateFlow<AdWatchRewardOutcome?> = _rewardOutcome.asStateFlow()

    private val _isPayoutSubmitting = MutableStateFlow(false)
    val isPayoutSubmitting: StateFlow<Boolean> = _isPayoutSubmitting.asStateFlow()

    private val _isSavingPaymentSettings = MutableStateFlow(false)
    val isSavingPaymentSettings: StateFlow<Boolean> = _isSavingPaymentSettings.asStateFlow()

    private val _isApplyingReferralCode = MutableStateFlow(false)
    val isApplyingReferralCode: StateFlow<Boolean> = _isApplyingReferralCode.asStateFlow()

    private val _isOwnerConfigSaving = MutableStateFlow(false)
    val isOwnerConfigSaving: StateFlow<Boolean> = _isOwnerConfigSaving.asStateFlow()

    private val _isOwnerWithdrawing = MutableStateFlow(false)
    val isOwnerWithdrawing: StateFlow<Boolean> = _isOwnerWithdrawing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val claimingReferralIds = mutableSetOf<String>()
    private var watchTimerJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                if (user.uid == currentUserId) {
                    repository.ensureUserProfile(user).onFailure { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to sync user profile."
                    }
                    val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Member"
                    repository.ensureReferralCode(currentUserId, displayName).onFailure { err ->
                        Log.w(TAG, "Deferred referral code sync", err)
                    }
                }
            }
        }
    }

    private val coreDashboardFlow = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeAdViews(currentUserId),
        repository.observeDailyStats(currentUserId),
        repository.observePayouts(currentUserId),
        repository.observeLinkedPaymentMethod(currentUserId),
    ) { profile, views, stats, payouts, linkedPaymentMethod ->
        CoreDashboardSnapshot(
            profile = profile,
            views = views,
            stats = stats,
            payouts = payouts,
            linkedPaymentMethod = linkedPaymentMethod,
        )
    }

    val dashboardState: StateFlow<UiState<DashboardData>> = combine(
        coreDashboardFlow,
        repository.observeIncomingReferral(currentUserId),
        repository.observeOutgoingReferrals(currentUserId),
    ) { core, incomingReferral, outgoingReferrals ->
        val todayKey = Formatters.todayDateKey()
        val sortedViews = core.views.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        val sortedPayouts = core.payouts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        val sortedStats = core.stats.sortedByDescending { it.dateKey }
        val sortedOutgoing = outgoingReferrals.sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }

        val todayByCampaign = sortedViews
            .filter { it.dateKey == todayKey }
            .groupingBy { it.campaignId }
            .eachCount()

        val resolvedProfile = core.profile ?: UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = Firebase.auth.currentUser?.displayName ?: "Side Hustle Member",
            email = Firebase.auth.currentUser?.email ?: "",
        )

        DashboardData(
            profile = resolvedProfile,
            adViews = sortedViews,
            dailyStats = sortedStats,
            payouts = sortedPayouts,
            todayViewsByCampaign = todayByCampaign,
            linkedPaymentMethod = core.linkedPaymentMethod,
            myReferralCode = ReferralConstants.codeForUser(currentUserId),
            incomingReferral = incomingReferral,
            outgoingReferrals = sortedOutgoing,
        )
    }
        .onEach { data ->
            data.outgoingReferrals
                .filter { it.isCompleted() && !it.referrerCredited }
                .forEach { completedRef ->
                    claimPendingReferrerBonus(completedRef, isAutomatic = true)
                }
        }
        .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Side Hustle dashboard", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load earnings data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val ownerConsoleState: StateFlow<UiState<OwnerConsoleData>?> = if (!isVerifiedAppOwner) {
        MutableStateFlow<UiState<OwnerConsoleData>?>(null).asStateFlow()
    } else {
        combine(
            repository.observeOwnerSummary(),
            repository.observeOwnerRevenueEntries(),
            repository.observeOwnerPayoutConfig(),
            repository.observeOwnerPayouts(),
        ) { summary, entries, config, payouts ->
            val sortedEntries = entries.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            val sortedPayouts = payouts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            OwnerConsoleData(
                summary = summary ?: OwnerRevenueSummary(id = "global"),
                entries = sortedEntries,
                payoutConfig = config ?: OwnerPayoutConfig(
                    id = "payoutConfig",
                    ownerEmail = APP_OWNER_EMAIL,
                    destinationLabel = APP_OWNER_EMAIL,
                ),
                payouts = sortedPayouts,
            )
        }
            .map<OwnerConsoleData, UiState<OwnerConsoleData>?> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing owner income console", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load owner income console."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    }

    fun prioritizedCampaigns(
        preferredCategories: List<String>,
        selectedFilterCategory: String,
        todayViewsByCampaign: Map<String, Int>,
    ): List<AdCampaign> {
        val prefSet = preferredCategories.toSet()
        val filtered = if (selectedFilterCategory == "All") {
            AdCampaignCatalog.campaigns
        } else {
            AdCampaignCatalog.campaigns.filter { it.category == selectedFilterCategory }
        }
        return filtered.sortedWith(
            compareByDescending<AdCampaign> { (todayViewsByCampaign[it.id] ?: 0) < it.dailyCap }
                .thenByDescending { it.category in prefSet }
                .thenByDescending { it.rewardCents }
        )
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectAnalyticsRange(range: AnalyticsRange) {
        _analyticsRange.value = range
    }

    fun selectDayForDrilldown(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissRewardOutcome() {
        _rewardOutcome.value = null
    }

    fun updatePreferredCategories(categories: List<String>) {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
            val result = repository.updatePreferredCategories(
                userId = currentUserId,
                categories = categories,
            )
            result.onSuccess {
                _snackbarMessage.value = if (categories.isEmpty()) {
                    "Showing all advert categories."
                } else {
                    "Prioritizing ${categories.joinToString(", ")} adverts."
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save ad preferences."
            }
        }
    }

    fun startWatchingCampaign(campaign: AdCampaign, todayCountForCampaign: Int) {
        if (todayCountForCampaign >= campaign.dailyCap) {
            _snackbarMessage.value = "Daily cap reached for ${campaign.sponsorName}. Try another campaign!"
            return
        }
        watchTimerJob?.cancel()
        _activeWatchSession.value = ActiveWatchSession(
            campaign = campaign,
            remainingSeconds = campaign.durationSeconds,
            totalSeconds = campaign.durationSeconds,
            isReadyToClaim = false,
            isSubmitting = false,
        )
        watchTimerJob = viewModelScope.launch {
            for (remaining in (campaign.durationSeconds - 1) downTo 0) {
                delay(1000L)
                _activeWatchSession.value = _activeWatchSession.value?.copy(
                    remainingSeconds = remaining,
                    isReadyToClaim = remaining == 0,
                )
            }
        }
    }

    fun cancelWatching() {
        watchTimerJob?.cancel()
        watchTimerJob = null
        _activeWatchSession.value = null
    }

    fun verifyAndClaimReward() {
        val session = _activeWatchSession.value ?: return
        if (!session.isReadyToClaim || session.isSubmitting) return

        _activeWatchSession.value = session.copy(isSubmitting = true)
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
            val result = repository.recordCompletedAdWatch(
                userId = currentUserId,
                campaign = session.campaign,
                currentDateKey = Formatters.todayDateKey(),
                yesterdayDateKey = Formatters.yesterdayDateKey(),
                hourOfDay = Formatters.currentHourOfDay(),
            )
            result.onSuccess { outcome ->
                _activeWatchSession.value = null
                _rewardOutcome.value = outcome
                _selectedDateKey.value = Formatters.todayDateKey()
            }.onFailure { err ->
                _activeWatchSession.value = session.copy(isSubmitting = false)
                _snackbarMessage.value = err.localizedMessage ?: "Could not verify reward right now."
            }
        }
    }

    fun updateDailyGoal(newGoalCents: Long, displayName: String) {
        viewModelScope.launch {
            val result = repository.updateDailyGoal(
                userId = currentUserId,
                dailyGoalCents = newGoalCents,
                displayName = displayName,
            )
            result.onSuccess {
                _snackbarMessage.value = "Daily goal updated to ${Formatters.formatCents(newGoalCents)}."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update daily goal."
            }
        }
    }

    fun requestPayout(
        amountCents: Long,
        method: PayoutMethod,
        destination: String,
        onSuccess: () -> Unit,
    ) {
        if (_isPayoutSubmitting.value) return
        _isPayoutSubmitting.value = true
        viewModelScope.launch {
            val result = repository.requestPayout(
                userId = currentUserId,
                amountCents = amountCents,
                method = method,
                destinationLabel = destination,
                currentDateKey = Formatters.todayDateKey(),
            )
            _isPayoutSubmitting.value = false
            result.onSuccess { payout ->
                _snackbarMessage.value =
                    "Cashout ${Formatters.formatCents(payout.amountCents)} submitted (${payout.referenceCode})."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Cashout request failed."
            }
        }
    }

    fun saveLinkedPaymentMethod(
        method: PayoutMethod,
        accountHolderName: String,
        destinationOrEmail: String,
        bankName: String,
        accountType: BankAccountType,
        routingNumber: String,
        accountNumber: String,
        autoPayoutEnabled: Boolean,
        onSaved: () -> Unit = {},
    ) {
        if (_isSavingPaymentSettings.value) return
        _isSavingPaymentSettings.value = true
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
            val result = repository.saveLinkedPaymentMethod(
                userId = currentUserId,
                method = method,
                accountHolderName = accountHolderName,
                destinationOrEmail = destinationOrEmail,
                bankName = bankName,
                accountType = accountType,
                routingNumber = routingNumber,
                accountNumber = accountNumber,
                autoPayoutEnabled = autoPayoutEnabled,
            )
            _isSavingPaymentSettings.value = false
            result.onSuccess { saved ->
                _snackbarMessage.value = "Linked ${saved.parsedMethod().label} (${saved.maskedDestination})."
                onSaved()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to link payment method."
            }
        }
    }

    fun unlinkPaymentMethod() {
        if (_isSavingPaymentSettings.value) return
        _isSavingPaymentSettings.value = true
        viewModelScope.launch {
            val result = repository.unlinkPaymentMethod(currentUserId)
            _isSavingPaymentSettings.value = false
            result.onSuccess {
                _snackbarMessage.value = "Linked withdrawal method removed."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to unlink payment method."
            }
        }
    }

    fun applyReferralCode(
        rawCode: String,
        displayName: String,
        onSuccess: () -> Unit = {},
    ) {
        if (_isApplyingReferralCode.value) return
        _isApplyingReferralCode.value = true
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
            val result = repository.applyReferralCode(
                userId = currentUserId,
                displayName = displayName,
                rawCode = rawCode,
            )
            _isApplyingReferralCode.value = false
            result.onSuccess { record ->
                _snackbarMessage.value =
                    "Code ${record.referralCode} linked! Watch ${record.requiredAds} ads to unlock +${Formatters.formatCents(record.bonusCents)} for both of you."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not apply referral code."
            }
        }
    }

    fun claimPendingReferrerBonus(
        referral: ReferralRecord,
        isAutomatic: Boolean = false,
    ) {
        if (!referral.isCompleted() || referral.referrerCredited) return
        if (!claimingReferralIds.add(referral.id)) return

        viewModelScope.launch {
            val result = repository.claimReferrerBonus(
                referrerUserId = currentUserId,
                referralId = referral.id,
            )
            result.onSuccess { creditedCents ->
                if (creditedCents > 0L) {
                    _snackbarMessage.value =
                        "Referral bonus! +${Formatters.formatCents(creditedCents)} added because ${referral.inviteeFirstName} completed ${referral.requiredAds} ads."
                }
            }.onFailure { err ->
                claimingReferralIds.remove(referral.id)
                if (!isAutomatic) {
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to claim referral bonus."
                } else {
                    Log.w(TAG, "Automatic referrer bonus claim failed for ${referral.id}", err)
                }
            }
        }
    }

    fun notifyMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun saveOwnerPayoutConfig(
        method: PayoutMethod,
        destination: String,
        autoSweep: Boolean,
    ) {
        if (!isVerifiedAppOwner || _isOwnerConfigSaving.value) return
        _isOwnerConfigSaving.value = true
        viewModelScope.launch {
            val result = repository.saveOwnerPayoutConfig(
                method = method,
                destinationLabel = destination,
                autoSweepEnabled = autoSweep,
            )
            _isOwnerConfigSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = "Owner payout destination saved (${method.label})."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save owner payout settings."
            }
        }
    }

    fun withdrawOwnerIncome(
        amountCents: Long,
        method: PayoutMethod,
        destination: String,
        onSuccess: () -> Unit,
    ) {
        if (!isVerifiedAppOwner || _isOwnerWithdrawing.value) return
        _isOwnerWithdrawing.value = true
        viewModelScope.launch {
            val result = repository.withdrawOwnerIncome(
                amountCents = amountCents,
                method = method,
                destinationLabel = destination,
                currentDateKey = Formatters.todayDateKey(),
            )
            _isOwnerWithdrawing.value = false
            result.onSuccess { payout ->
                _snackbarMessage.value =
                    "Owner payout ${Formatters.formatCents(payout.amountCents)} dispatched (${payout.referenceCode})."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Owner withdrawal failed."
            }
        }
    }

    fun computeAnalytics(data: DashboardData, range: AnalyticsRange): DailyAnalyticsSummary {
        val todayKey = Formatters.todayDateKey()
        val statsByDate = data.dailyStats.associateBy { it.dateKey }
        val todayStat = statsByDate[todayKey]
        val dailyGoal = data.profile.dailyGoalCents.coerceAtLeast(10L)

        val dateKeys = Formatters.recentDateKeys(range.days)
        val chartPoints = dateKeys.map { dateKey ->
            val stat = statsByDate[dateKey]
            DailyChartPoint(
                dateKey = dateKey,
                shortLabel = Formatters.shortDayLabel(dateKey),
                friendlyLabel = Formatters.friendlyDateLabel(dateKey),
                earnedCents = stat?.earnedCents ?: 0L,
                bonusCents = stat?.bonusCents ?: 0L,
                adsWatched = stat?.adsWatched ?: 0L,
                watchSeconds = stat?.watchSeconds ?: 0L,
                topCategory = stat?.topCategory ?: "",
            )
        }

        val rangeTotal = chartPoints.sumOf { it.earnedCents }
        val activeDaysCount = chartPoints.count { it.earnedCents > 0L }.coerceAtLeast(1)
        val rangeDailyAvg = if (rangeTotal == 0L) 0L else rangeTotal / activeDaysCount

        val rangeKeySet = dateKeys.toSet()
        val rangeViews = data.adViews.filter { it.dateKey in rangeKeySet }

        val totalWatchSecs = rangeViews.sumOf { it.durationSeconds }
        val totalViewCents = rangeViews.sumOf { it.rewardCents }
        val grossSponsorCents = rangeViews.sumOf { AdCampaignCatalog.grossBidForCampaignId(it.campaignId) }
        val netPlatformMarginCents = (grossSponsorCents - totalViewCents).coerceAtLeast(0L)
        val prefSet = data.profile.preferredCategories.toSet()
        val targetedMatchRatePercent = if (rangeViews.isNotEmpty() && prefSet.isNotEmpty()) {
            ((rangeViews.count { it.category in prefSet } * 100f) / rangeViews.size).toInt()
        } else {
            0
        }

        val effectiveHourlyCents = if (totalWatchSecs > 0L) {
            (totalViewCents * 3600L) / totalWatchSecs
        } else {
            0L
        }
        val avgPerAd = if (rangeViews.isNotEmpty()) {
            totalViewCents / rangeViews.size
        } else {
            0L
        }

        val categoryBreakdown = if (rangeViews.isEmpty()) {
            emptyList()
        } else {
            val totalForCats = totalViewCents.coerceAtLeast(1L)
            rangeViews
                .groupBy { it.category.ifBlank { "General" } }
                .map { (category, views) ->
                    val earned = views.sumOf { it.rewardCents }
                    CategoryBreakdownItem(
                        category = category,
                        earnedCents = earned,
                        adsWatched = views.size,
                        shareFraction = earned.toFloat() / totalForCats.toFloat(),
                    )
                }
                .sortedByDescending { it.earnedCents }
        }

        val morning = rangeViews.filter { it.hourOfDay in 6..11 }
        val midday = rangeViews.filter { it.hourOfDay in 12..16 }
        val evening = rangeViews.filter { it.hourOfDay in 17..21 }
        val night = rangeViews.filter { it.hourOfDay >= 22 || it.hourOfDay <= 5 }

        val timeWindows = listOf(
            TimeWindowStat("Morning", "6a–12p", morning.sumOf { it.rewardCents }, morning.size),
            TimeWindowStat("Midday", "12p–5p", midday.sumOf { it.rewardCents }, midday.size),
            TimeWindowStat("Evening", "5p–10p", evening.sumOf { it.rewardCents }, evening.size),
            TimeWindowStat("Late Night", "10p–6a", night.sumOf { it.rewardCents }, night.size),
        )

        val bestDay = chartPoints.filter { it.earnedCents > 0L }.maxByOrNull { it.earnedCents }
        val todayEarned = todayStat?.earnedCents ?: 0L

        return DailyAnalyticsSummary(
            todayDateKey = todayKey,
            todayEarnedCents = todayEarned,
            todayBonusCents = todayStat?.bonusCents ?: 0L,
            todayAdsWatched = todayStat?.adsWatched ?: 0L,
            todayWatchSeconds = todayStat?.watchSeconds ?: 0L,
            dailyGoalCents = dailyGoal,
            dailyGoalProgress = (todayEarned.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f),
            rangeTotalCents = rangeTotal,
            rangeDailyAvgCents = rangeDailyAvg,
            effectiveHourlyCents = effectiveHourlyCents,
            avgRewardPerAdCents = avgPerAd,
            bestDay = bestDay,
            chartPoints = chartPoints,
            categoryBreakdown = categoryBreakdown,
            timeWindows = timeWindows,
            grossSponsorCents = grossSponsorCents,
            netPlatformMarginCents = netPlatformMarginCents,
            targetedMatchRatePercent = targetedMatchRatePercent,
        )
    }

    override fun onCleared() {
        super.onCleared()
        watchTimerJob?.cancel()
    }

    private companion object {
        const val TAG = "AdPulseVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
