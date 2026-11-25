package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdPulseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireVerifiedOwnerEmail(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("User must be signed in before accessing owner income.")
        check(user.email == APP_OWNER_EMAIL && user.isEmailVerified) {
            "Owner income access requires verified account $APP_OWNER_EMAIL."
        }
        return APP_OWNER_EMAIL
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user changed; refusing cross-user profile creation." }
            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().await()
            if (!existing.exists()) {
                val safeDisplayName = (user.displayName?.trim()?.take(60))
                    ?.takeIf { it.isNotEmpty() }
                    ?: user.email?.substringBefore("@")?.take(60)?.takeIf { it.isNotEmpty() }
                    ?: "Side Hustle Member"
                val safeEmail = (user.email ?: "").take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "availableCents" to 0L,
                    "lifetimeEarnedCents" to 0L,
                    "totalWithdrawnCents" to 0L,
                    "totalAdsWatched" to 0L,
                    "totalWatchSeconds" to 0L,
                    "dailyGoalCents" to 200L,
                    "currentStreakDays" to 0L,
                    "lastActiveDate" to "",
                    "preferredCategories" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                userRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeAdViews(userId: String): Flow<List<AdViewRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/adViews"
        emitAll(
            db.collection("users").document(uid).collection("adViews")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdViewRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeDailyStats(userId: String): Flow<List<DailyEarningsStat>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/dailyStats"
        emitAll(
            db.collection("users").document(uid).collection("dailyStats")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyEarningsStat::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observePayouts(userId: String): Flow<List<PayoutRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/payouts"
        emitAll(
            db.collection("users").document(uid).collection("payouts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeLinkedPaymentMethod(userId: String): Flow<LinkedPaymentMethod?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/paymentSettings/preferredMethod"
        emitAll(
            db.collection("users").document(uid)
                .collection("paymentSettings").document("preferredMethod")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LinkedPaymentMethod::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeOwnerSummary(): Flow<OwnerRevenueSummary?> = flow {
        val uid = requireUserId()
        requireVerifiedOwnerEmail()
        val path = "ownerRevenueSummary/global"
        emitAll(
            db.collection("ownerRevenueSummary").document("global")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            OwnerRevenueSummary::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeOwnerRevenueEntries(): Flow<List<OwnerRevenueEntry>> = flow {
        val uid = requireUserId()
        requireVerifiedOwnerEmail()
        val path = "ownerRevenueEntries"
        emitAll(
            db.collection("ownerRevenueEntries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OwnerRevenueEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeOwnerPayoutConfig(): Flow<OwnerPayoutConfig?> = flow {
        val uid = requireUserId()
        requireVerifiedOwnerEmail()
        val path = "ownerSettings/payoutConfig"
        emitAll(
            db.collection("ownerSettings").document("payoutConfig")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            OwnerPayoutConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeOwnerPayouts(): Flow<List<OwnerPayoutRecord>> = flow {
        val uid = requireUserId()
        requireVerifiedOwnerEmail()
        val path = "ownerPayouts"
        emitAll(
            db.collection("ownerPayouts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OwnerPayoutRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeIncomingReferral(userId: String): Flow<ReferralRecord?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "referrals/$uid"
        emitAll(
            db.collection("referrals").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ReferralRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeOutgoingReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun ensureReferralCode(
        userId: String,
        displayName: String,
    ): Result<String> {
        val code = ReferralConstants.codeForUser(userId)
        val path = "referralCodes/$code"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user referral setup." }
            val codeRef = db.collection("referralCodes").document(code)
            val existing = codeRef.get().await()
            if (!existing.exists()) {
                val firstName = ReferralConstants.safeFirstName(displayName, "Member")
                codeRef.set(
                    mapOf(
                        "code" to code,
                        "ownerUserId" to uid,
                        "ownerFirstName" to firstName,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(
        userId: String,
        displayName: String,
        rawCode: String,
    ): Result<ReferralRecord> {
        val path = "referrals/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user referral redemption." }

            val normalizedCode = ReferralConstants.normalizeCode(rawCode)
            require(ReferralConstants.isValidCodeFormat(normalizedCode)) {
                "Enter a valid 6–12 character referral code (letters & numbers)."
            }
            require(normalizedCode != ReferralConstants.codeForUser(uid)) {
                "You cannot redeem your own referral code."
            }

            val codeSnap = db.collection("referralCodes").document(normalizedCode).get().await()
            if (!codeSnap.exists()) {
                throw IllegalArgumentException("Referral code \"$normalizedCode\" was not found.")
            }

            val referrerUid = codeSnap.getString("ownerUserId")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Invalid referral code metadata.")
            if (referrerUid == uid) {
                throw IllegalArgumentException("You cannot redeem your own referral code.")
            }

            val referrerFirstName = ReferralConstants.safeFirstName(
                codeSnap.getString("ownerFirstName"),
                fallback = "Friend"
            )
            val inviteeFirstName = ReferralConstants.safeFirstName(displayName, fallback = "Friend")
            val referralRef = db.collection("referrals").document(uid)

            val created = db.runTransaction { transaction ->
                val existingReferral = transaction.get(referralRef)
                if (existingReferral.exists()) {
                    throw IllegalStateException("You have already linked a referral code to your account.")
                }

                transaction.set(
                    referralRef,
                    mapOf(
                        "referralCode" to normalizedCode,
                        "referrerUserId" to referrerUid,
                        "referrerFirstName" to referrerFirstName,
                        "inviteeUserId" to uid,
                        "inviteeFirstName" to inviteeFirstName,
                        "adsCompleted" to 0L,
                        "requiredAds" to ReferralConstants.REQUIRED_ADS,
                        "bonusCents" to ReferralConstants.BONUS_CENTS,
                        "status" to "PENDING",
                        "inviteeCredited" to false,
                        "referrerCredited" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                ReferralRecord(
                    id = uid,
                    referralCode = normalizedCode,
                    referrerUserId = referrerUid,
                    referrerFirstName = referrerFirstName,
                    inviteeUserId = uid,
                    inviteeFirstName = inviteeFirstName,
                    adsCompleted = 0L,
                    requiredAds = ReferralConstants.REQUIRED_ADS,
                    bonusCents = ReferralConstants.BONUS_CENTS,
                    status = "PENDING",
                    inviteeCredited = false,
                    referrerCredited = false,
                )
            }.await()

            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimReferrerBonus(
        referrerUserId: String,
        referralId: String,
    ): Result<Long> {
        val path = "referrals/$referralId"
        return try {
            val uid = requireUserId()
            check(uid == referrerUserId) { "Signed-in user changed; refusing cross-user claim." }
            val referralRef = db.collection("referrals").document(referralId)
            val userRef = db.collection("users").document(uid)

            val creditedCents = db.runTransaction { transaction ->
                val refSnap = transaction.get(referralRef)
                val userSnap = transaction.get(userRef)

                if (!refSnap.exists()) {
                    throw IllegalStateException("Referral record not found.")
                }
                if (refSnap.getString("referrerUserId") != uid) {
                    throw IllegalStateException("Referral does not belong to this account.")
                }
                if (refSnap.getString("status") != "COMPLETED") {
                    return@runTransaction 0L
                }
                if (refSnap.getBoolean("referrerCredited") == true) {
                    return@runTransaction 0L
                }

                val bonusCents = refSnap.getLong("bonusCents") ?: ReferralConstants.BONUS_CENTS
                val prevAvailable = userSnap.getLong("availableCents") ?: 0L
                val prevLifetime = userSnap.getLong("lifetimeEarnedCents") ?: 0L

                transaction.update(
                    referralRef,
                    mapOf(
                        "referrerCredited" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "availableCents" to (prevAvailable + bonusCents),
                        "lifetimeEarnedCents" to (prevLifetime + bonusCents),
                        "lastReferralClaimId" to referralId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                bonusCents
            }.await()

            Result.success(creditedCents)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredCategories(
        userId: String,
        categories: List<String>,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val cleaned = categories
                .map { it.trim().take(30) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(10)

            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "preferredCategories" to cleaned,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDailyGoal(
        userId: String,
        dailyGoalCents: Long,
        displayName: String,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(dailyGoalCents in 10L..50000L) { "Daily goal must be between $0.10 and $500.00" }
            val trimmedName = displayName.trim().take(60)
            require(trimmedName.isNotEmpty()) { "Display name cannot be blank" }

            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "dailyGoalCents" to dailyGoalCents,
                        "displayName" to trimmedName,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedAdWatch(
        userId: String,
        campaign: AdCampaign,
        currentDateKey: String,
        yesterdayDateKey: String,
        hourOfDay: Int,
    ): Result<AdWatchRewardOutcome> {
        val path = "users/$userId/adViews"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(campaign.durationSeconds in 5..120) { "Invalid ad duration" }
            require(campaign.rewardCents in 1L..500L) { "Invalid reward amount" }
            require(campaign.platformMarginCents in 1L..500L) { "Invalid owner margin amount" }
            require(campaign.rewardCents * 100L == campaign.grossSponsorCents * USER_REWARD_SHARE_PERCENT) {
                "User reward must equal $USER_REWARD_SHARE_PERCENT% of gross sponsor revenue."
            }
            require(campaign.platformMarginCents * 100L == campaign.grossSponsorCents * OWNER_REVENUE_SHARE_PERCENT) {
                "Owner income must equal $OWNER_REVENUE_SHARE_PERCENT% of gross sponsor revenue."
            }
            require(hourOfDay in 0..23) { "Invalid hour of day" }

            val userRef = db.collection("users").document(uid)
            val dailyRef = userRef.collection("dailyStats").document(currentDateKey)
            val referralRef = db.collection("referrals").document(uid)
            val viewId = "view_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val viewRef = userRef.collection("adViews").document(viewId)
            val ownerEntryRef = db.collection("ownerRevenueEntries").document(viewId)
            val ownerSummaryRef = db.collection("ownerRevenueSummary").document("global")

            val outcome = db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val dailySnap = transaction.get(dailyRef)
                val referralSnap = transaction.get(referralRef)

                val prevAvailable = (userSnap.getLong("availableCents") ?: 0L)
                val prevLifetime = (userSnap.getLong("lifetimeEarnedCents") ?: 0L)
                val prevAdsWatched = (userSnap.getLong("totalAdsWatched") ?: 0L)
                val prevWatchSecs = (userSnap.getLong("totalWatchSeconds") ?: 0L)
                val dailyGoal = (userSnap.getLong("dailyGoalCents") ?: 200L).coerceIn(10L, 50000L)
                val prevStreak = (userSnap.getLong("currentStreakDays") ?: 0L)
                val lastActiveDate = userSnap.getString("lastActiveDate") ?: ""
                val rawPrefs = userSnap.get("preferredCategories") as? List<*>
                val preferenceMatched = rawPrefs?.any { it == campaign.category } == true

                val prevDailyEarned = if (dailySnap.exists()) (dailySnap.getLong("earnedCents") ?: 0L) else 0L
                val prevDailyAds = if (dailySnap.exists()) (dailySnap.getLong("adsWatched") ?: 0L) else 0L
                val prevDailySecs = if (dailySnap.exists()) (dailySnap.getLong("watchSeconds") ?: 0L) else 0L
                val prevDailyBonus = if (dailySnap.exists()) (dailySnap.getLong("bonusCents") ?: 0L) else 0L

                val hasActiveReferral = referralSnap.exists() &&
                    referralSnap.getString("status") == "PENDING" &&
                    referralSnap.getBoolean("inviteeCredited") == false
                val prevRefAds = if (hasActiveReferral) {
                    (referralSnap.getLong("adsCompleted") ?: 0L).coerceAtLeast(0L)
                } else {
                    0L
                }
                val requiredRefAds = if (hasActiveReferral) {
                    (referralSnap.getLong("requiredAds") ?: ReferralConstants.REQUIRED_ADS)
                } else {
                    ReferralConstants.REQUIRED_ADS
                }
                val canAdvanceReferral = hasActiveReferral && prevRefAds < requiredRefAds
                val nextRefAds = if (canAdvanceReferral) (prevRefAds + 1L).coerceAtMost(requiredRefAds) else prevRefAds
                val referralCompletedNow = canAdvanceReferral && nextRefAds >= requiredRefAds
                val referralBonusCents = if (referralCompletedNow) {
                    referralSnap.getLong("bonusCents") ?: ReferralConstants.BONUS_CENTS
                } else {
                    0L
                }

                val baseReward = campaign.rewardCents
                val goalBonus = if (
                    (prevDailyBonus == 0L || prevDailyBonus == ReferralConstants.BONUS_CENTS) &&
                    prevDailyEarned < dailyGoal &&
                    (prevDailyEarned + baseReward) >= dailyGoal
                ) {
                    25L
                } else {
                    0L
                }
                val totalBonusDelta = goalBonus + referralBonusCents
                val totalCredit = baseReward + totalBonusDelta

                val newStreak = when {
                    lastActiveDate == currentDateKey -> prevStreak.coerceAtLeast(1L)
                    lastActiveDate == yesterdayDateKey -> prevStreak + 1L
                    else -> 1L
                }

                val newAvailable = prevAvailable + totalCredit

                transaction.set(
                    viewRef,
                    mapOf(
                        "userId" to uid,
                        "campaignId" to campaign.id,
                        "campaignTitle" to campaign.title.take(100),
                        "sponsorName" to campaign.sponsorName.take(60),
                        "category" to campaign.category.take(40),
                        "durationSeconds" to campaign.durationSeconds.toLong(),
                        "rewardCents" to baseReward,
                        "dateKey" to currentDateKey,
                        "hourOfDay" to hourOfDay.toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (dailySnap.exists()) {
                    transaction.update(
                        dailyRef,
                        mapOf(
                            "earnedCents" to (prevDailyEarned + totalCredit),
                            "adsWatched" to (prevDailyAds + 1L),
                            "watchSeconds" to (prevDailySecs + campaign.durationSeconds.toLong()),
                            "topCategory" to campaign.category.take(40),
                            "bonusCents" to (prevDailyBonus + totalBonusDelta),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    transaction.set(
                        dailyRef,
                        mapOf(
                            "userId" to uid,
                            "dateKey" to currentDateKey,
                            "earnedCents" to totalCredit,
                            "adsWatched" to 1L,
                            "watchSeconds" to campaign.durationSeconds.toLong(),
                            "topCategory" to campaign.category.take(40),
                            "bonusCents" to totalBonusDelta,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "availableCents" to newAvailable,
                        "lifetimeEarnedCents" to (prevLifetime + totalCredit),
                        "totalAdsWatched" to (prevAdsWatched + 1L),
                        "totalWatchSeconds" to (prevWatchSecs + campaign.durationSeconds.toLong()),
                        "currentStreakDays" to newStreak,
                        "lastActiveDate" to currentDateKey,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (canAdvanceReferral) {
                    transaction.update(
                        referralRef,
                        mapOf(
                            "adsCompleted" to nextRefAds,
                            "status" to if (referralCompletedNow) "COMPLETED" else "PENDING",
                            "inviteeCredited" to referralCompletedNow,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }

                // Atomically record immutable owner revenue ledger entry & credit owner income summary
                val ownerIncomeCents = campaign.platformMarginCents
                transaction.set(
                    ownerEntryRef,
                    mapOf(
                        "viewerUserId" to uid,
                        "campaignId" to campaign.id,
                        "campaignTitle" to campaign.title.take(100),
                        "category" to campaign.category.take(40),
                        "grossSponsorCents" to campaign.grossSponsorCents,
                        "userRewardCents" to baseReward,
                        "ownerIncomeCents" to ownerIncomeCents,
                        "preferenceMatched" to preferenceMatched,
                        "dateKey" to currentDateKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )

                transaction.set(
                    ownerSummaryRef,
                    mapOf(
                        "availableIncomeCents" to FieldValue.increment(ownerIncomeCents),
                        "lifetimeIncomeCents" to FieldValue.increment(ownerIncomeCents),
                        "withdrawnIncomeCents" to FieldValue.increment(0L),
                        "grossSponsorCents" to FieldValue.increment(campaign.grossSponsorCents),
                        "totalUserRewardsCents" to FieldValue.increment(baseReward),
                        "totalVerifiedViews" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )

                AdWatchRewardOutcome(
                    campaignTitle = campaign.title,
                    rewardCents = baseReward,
                    bonusCents = goalBonus,
                    referralBonusCents = referralBonusCents,
                    newAvailableCents = newAvailable,
                    goalReachedNow = goalBonus > 0L,
                )
            }.await()

            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun requestPayout(
        userId: String,
        amountCents: Long,
        method: PayoutMethod,
        destinationLabel: String,
        currentDateKey: String,
    ): Result<PayoutRecord> {
        val path = "users/$userId/payouts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user payout." }
            require(amountCents >= method.minCents && amountCents <= 100000L) {
                "Minimum cashout for ${method.label} is ${Formatters.formatCents(method.minCents)}."
            }
            val cleanedDestination = destinationLabel.trim().take(120)
            require(cleanedDestination.length >= 3) {
                "Please enter a valid payout destination."
            }

            val userRef = db.collection("users").document(uid)
            val payoutId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val payoutRef = userRef.collection("payouts").document(payoutId)
            val referenceCode = "SHS-${UUID.randomUUID().toString().uppercase().take(6)}"

            val record = db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val available = userSnap.getLong("availableCents") ?: 0L
                val totalWithdrawn = userSnap.getLong("totalWithdrawnCents") ?: 0L
                if (available < amountCents) {
                    throw IllegalStateException("Insufficient available balance for cashout.")
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "availableCents" to (available - amountCents),
                        "totalWithdrawnCents" to (totalWithdrawn + amountCents),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                transaction.set(
                    payoutRef,
                    mapOf(
                        "userId" to uid,
                        "amountCents" to amountCents,
                        "method" to method.name,
                        "destinationLabel" to cleanedDestination,
                        "status" to PayoutStatus.PROCESSING.name,
                        "referenceCode" to referenceCode,
                        "dateKey" to currentDateKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                PayoutRecord(
                    id = payoutId,
                    userId = uid,
                    amountCents = amountCents,
                    method = method.name,
                    destinationLabel = cleanedDestination,
                    status = PayoutStatus.PROCESSING.name,
                    referenceCode = referenceCode,
                    dateKey = currentDateKey,
                )
            }.await()

            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveLinkedPaymentMethod(
        userId: String,
        method: PayoutMethod,
        accountHolderName: String,
        destinationOrEmail: String,
        bankName: String,
        accountType: BankAccountType,
        routingNumber: String,
        accountNumber: String,
        autoPayoutEnabled: Boolean,
    ): Result<LinkedPaymentMethod> {
        val path = "users/$userId/paymentSettings/preferredMethod"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user payment update." }

            val cleanedHolder = accountHolderName.trim().take(80)
            require(cleanedHolder.length >= 2) { "Please enter the account holder name." }

            val cleanedBankName: String
            val cleanedAccountType: String
            val accountLast4: String
            val routingLast4: String
            val maskedDestination: String

            when (method) {
                PayoutMethod.BANK_ACH -> {
                    cleanedBankName = bankName.trim().take(60)
                    require(cleanedBankName.length >= 2) { "Please enter your bank or institution name." }
                    val routingDigits = routingNumber.filter { it.isDigit() }
                    val accountDigits = accountNumber.filter { it.isDigit() }
                    require(routingDigits.length == 9) { "Routing number must be exactly 9 digits." }
                    require(accountDigits.length in 4..17) { "Account number must be 4 to 17 digits." }
                    cleanedAccountType = accountType.name
                    routingLast4 = routingDigits.takeLast(4)
                    accountLast4 = accountDigits.takeLast(4)
                    maskedDestination = "$cleanedBankName ${accountType.label} •••• $accountLast4".take(120)
                }

                PayoutMethod.PAYPAL, PayoutMethod.GIFT_CARD -> {
                    val email = destinationOrEmail.trim().take(120)
                    require(email.contains("@") && email.contains(".") && email.length >= 5) {
                        "Please enter a valid email address."
                    }
                    cleanedBankName = ""
                    cleanedAccountType = ""
                    accountLast4 = ""
                    routingLast4 = ""
                    maskedDestination = email
                }

                PayoutMethod.VENMO -> {
                    val handle = destinationOrEmail.trim().take(120)
                    require(handle.length >= 3) { "Please enter a valid Venmo handle or phone number." }
                    cleanedBankName = ""
                    cleanedAccountType = ""
                    accountLast4 = ""
                    routingLast4 = ""
                    maskedDestination = handle
                }
            }

            val prefRef = db.collection("users").document(uid)
                .collection("paymentSettings").document("preferredMethod")

            val saved = db.runTransaction { transaction ->
                val existing = transaction.get(prefRef)
                val commonFields = mapOf(
                    "userId" to uid,
                    "method" to method.name,
                    "accountHolderName" to cleanedHolder,
                    "maskedDestination" to maskedDestination,
                    "bankName" to cleanedBankName,
                    "accountType" to cleanedAccountType,
                    "accountLast4" to accountLast4,
                    "routingLast4" to routingLast4,
                    "autoPayoutEnabled" to autoPayoutEnabled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (existing.exists()) {
                    transaction.update(prefRef, commonFields)
                } else {
                    transaction.set(
                        prefRef,
                        commonFields + ("createdAt" to FieldValue.serverTimestamp())
                    )
                }
                LinkedPaymentMethod(
                    id = "preferredMethod",
                    userId = uid,
                    method = method.name,
                    accountHolderName = cleanedHolder,
                    maskedDestination = maskedDestination,
                    bankName = cleanedBankName,
                    accountType = cleanedAccountType,
                    accountLast4 = accountLast4,
                    routingLast4 = routingLast4,
                    autoPayoutEnabled = autoPayoutEnabled,
                )
            }.await()

            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unlinkPaymentMethod(userId: String): Result<Unit> {
        val path = "users/$userId/paymentSettings/preferredMethod"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            db.collection("users").document(uid)
                .collection("paymentSettings").document("preferredMethod")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveOwnerPayoutConfig(
        method: PayoutMethod,
        destinationLabel: String,
        autoSweepEnabled: Boolean,
    ): Result<Unit> {
        val path = "ownerSettings/payoutConfig"
        return try {
            val ownerEmail = requireVerifiedOwnerEmail()
            val cleanedDestination = destinationLabel.trim().take(120)
            require(cleanedDestination.length >= 3) {
                "Please enter a valid payout destination."
            }

            db.collection("ownerSettings").document("payoutConfig")
                .set(
                    mapOf(
                        "ownerEmail" to ownerEmail,
                        "method" to method.name,
                        "destinationLabel" to cleanedDestination,
                        "autoSweepEnabled" to autoSweepEnabled,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun withdrawOwnerIncome(
        amountCents: Long,
        method: PayoutMethod,
        destinationLabel: String,
        currentDateKey: String,
    ): Result<OwnerPayoutRecord> {
        val path = "ownerPayouts"
        return try {
            val ownerEmail = requireVerifiedOwnerEmail()
            require(amountCents >= 100L && amountCents <= 10000000L) {
                "Minimum owner withdrawal is $1.00."
            }
            val cleanedDestination = destinationLabel.trim().take(120)
            require(cleanedDestination.length >= 3) {
                "Please enter a valid payout destination."
            }

            val summaryRef = db.collection("ownerRevenueSummary").document("global")
            val payoutId = "opay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val payoutRef = db.collection("ownerPayouts").document(payoutId)
            val referenceCode = "OWN-${UUID.randomUUID().toString().uppercase().take(6)}"

            val record = db.runTransaction { transaction ->
                val summarySnap = transaction.get(summaryRef)
                val available = summarySnap.getLong("availableIncomeCents") ?: 0L
                val withdrawn = summarySnap.getLong("withdrawnIncomeCents") ?: 0L
                if (available < amountCents) {
                    throw IllegalStateException("Insufficient owner income balance.")
                }

                transaction.update(
                    summaryRef,
                    mapOf(
                        "availableIncomeCents" to (available - amountCents),
                        "withdrawnIncomeCents" to (withdrawn + amountCents),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                transaction.set(
                    payoutRef,
                    mapOf(
                        "ownerEmail" to ownerEmail,
                        "amountCents" to amountCents,
                        "method" to method.name,
                        "destinationLabel" to cleanedDestination,
                        "status" to PayoutStatus.PROCESSING.name,
                        "referenceCode" to referenceCode,
                        "dateKey" to currentDateKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                OwnerPayoutRecord(
                    id = payoutId,
                    ownerEmail = ownerEmail,
                    amountCents = amountCents,
                    method = method.name,
                    destinationLabel = cleanedDestination,
                    status = PayoutStatus.PROCESSING.name,
                    referenceCode = referenceCode,
                    dateKey = currentDateKey,
                )
            }.await()

            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
