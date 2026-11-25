const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Alice Tester",
    email: "alice@example.com",
    availableCents: 0,
    lifetimeEarnedCents: 0,
    totalWithdrawnCents: 0,
    totalAdsWatched: 0,
    totalWatchSeconds: 0,
    dailyGoalCents: 200,
    currentStreakDays: 0,
    lastActiveDate: "",
    preferredCategories: [],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("adViews").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dailyStats").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("payouts").get()
  );
});

test("User profile creation enforces zero initial balances and rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Valid zero-balance creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );

  // Reject profile creation with non-zero starting money
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .set(validProfilePayload(BOB_UID))
  );
  await assertFails(
    testEnv
      .authenticatedContext(BOB_UID)
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(validProfilePayload(BOB_UID, { availableCents: 500, lifetimeEarnedCents: 500 }))
  );

  // Reject ghost field like isAdmin
  await assertFails(
    testEnv
      .authenticatedContext(BOB_UID)
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(validProfilePayload(BOB_UID, { isAdmin: true }))
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      dailyGoalCents: 300,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("adViews").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("dailyStats").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payouts").get()
  );
});

test("Daily goal, preferredCategories, and verified ad watch batch succeed while forged jumps fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(userRef.set(validProfilePayload(ALICE_UID)));

  // Goal + preferredCategories update succeeds
  await assertSucceeds(
    userRef.update({
      dailyGoalCents: 350,
      displayName: "Alice Pro",
      preferredCategories: ["Finance", "Gaming", "Technology"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Updating only preferredCategories succeeds
  await assertSucceeds(
    userRef.update({
      preferredCategories: ["Fashion", "Technology"],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Oversized preferredCategories (>10) fails
  await assertFails(
    userRef.update({
      preferredCategories: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
      updatedAt: new Date(Date.now() - 350),
    })
  );

  // Legitimate ad watch batch: user update + adView create + dailyStats create
  const now = new Date(Date.now() - 500);
  const dateKey = "2026-11-23";
  const batch = aliceDb.batch();
  batch.update(userRef, {
    availableCents: 15,
    lifetimeEarnedCents: 15,
    totalAdsWatched: 1,
    totalWatchSeconds: 15,
    currentStreakDays: 1,
    lastActiveDate: dateKey,
    updatedAt: now,
  });
  batch.set(userRef.collection("adViews").doc("view_1"), {
    userId: ALICE_UID,
    campaignId: "camp_fintech_01",
    campaignTitle: "NovaPay Metal Card",
    sponsorName: "NovaPay",
    category: "Finance",
    durationSeconds: 15,
    rewardCents: 15,
    dateKey,
    hourOfDay: 10,
    createdAt: now,
  });
  batch.set(userRef.collection("dailyStats").doc(dateKey), {
    userId: ALICE_UID,
    dateKey,
    earnedCents: 15,
    adsWatched: 1,
    watchSeconds: 15,
    topCategory: "Finance",
    bonusCents: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Second ad watch same day updates dailyStats & user profile
  const batch2 = aliceDb.batch();
  const now2 = new Date(Date.now() - 200);
  batch2.update(userRef, {
    availableCents: 35,
    lifetimeEarnedCents: 35,
    totalAdsWatched: 2,
    totalWatchSeconds: 35,
    currentStreakDays: 1,
    lastActiveDate: dateKey,
    updatedAt: now2,
  });
  batch2.set(userRef.collection("adViews").doc("view_2"), {
    userId: ALICE_UID,
    campaignId: "camp_travel_01",
    campaignTitle: "Fjord Eco Cabins",
    sponsorName: "NordicStay",
    category: "Travel",
    durationSeconds: 20,
    rewardCents: 20,
    dateKey,
    hourOfDay: 11,
    createdAt: now2,
  });
  batch2.update(userRef.collection("dailyStats").doc(dateKey), {
    earnedCents: 35,
    adsWatched: 2,
    watchSeconds: 35,
    topCategory: "Travel",
    bonusCents: 0,
    updatedAt: now2,
  });
  await assertSucceeds(batch2.commit());

  // Reject arbitrary large balance jump (>500 cents per watch)
  await assertFails(
    userRef.update({
      availableCents: 5000,
      lifetimeEarnedCents: 5000,
      totalAdsWatched: 3,
      totalWatchSeconds: 50,
      currentStreakDays: 1,
      lastActiveDate: dateKey,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject @DocumentId "id" key inside adView document
  await assertFails(
    userRef.collection("adViews").doc("view_bad").set({
      id: "view_bad",
      userId: ALICE_UID,
      campaignId: "camp_fintech_01",
      campaignTitle: "NovaPay",
      sponsorName: "NovaPay",
      category: "Finance",
      durationSeconds: 15,
      rewardCents: 15,
      dateKey,
      hourOfDay: 10,
      createdAt: new Date(Date.now() - 100),
    })
  );

  // Reject subcollection list query with future timestamp write
  await assertSucceeds(userRef.collection("adViews").get());
  await assertSucceeds(userRef.collection("dailyStats").get());
});

test("Atomic cashout succeeds when balance is sufficient and fails without matching balance deduction", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        displayName: "Alice Tester",
        email: "alice@example.com",
        availableCents: 250,
        lifetimeEarnedCents: 250,
        totalWithdrawnCents: 0,
        totalAdsWatched: 12,
        totalWatchSeconds: 180,
        dailyGoalCents: 200,
        currentStreakDays: 3,
        lastActiveDate: "2026-11-23",
        preferredCategories: ["Finance"],
        createdAt: created,
        updatedAt: created,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const now = new Date(Date.now() - 500);

  // Creating payout WITHOUT deducting user balance must fail
  await assertFails(
    userRef.collection("payouts").doc("pay_orphan").set({
      userId: ALICE_UID,
      amountCents: 150,
      method: "PAYPAL",
      destinationLabel: "alice***@example.com",
      status: "PROCESSING",
      referenceCode: "ADP-1001",
      dateKey: "2026-11-23",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic batch deducting 150 cents and creating payout succeeds
  const batch = aliceDb.batch();
  batch.update(userRef, {
    availableCents: 100,
    totalWithdrawnCents: 150,
    updatedAt: now,
  });
  batch.set(userRef.collection("payouts").doc("pay_1"), {
    userId: ALICE_UID,
    amountCents: 150,
    method: "PAYPAL",
    destinationLabel: "alice***@example.com",
    status: "PROCESSING",
    referenceCode: "ADP-1002",
    dateKey: "2026-11-23",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Attempting to withdraw more than remaining 100 cents fails
  const overBatch = aliceDb.batch();
  overBatch.update(userRef, {
    availableCents: -50,
    totalWithdrawnCents: 300,
    updatedAt: new Date(Date.now() - 200),
  });
  overBatch.set(userRef.collection("payouts").doc("pay_over"), {
    userId: ALICE_UID,
    amountCents: 150,
    method: "PAYPAL",
    destinationLabel: "alice***@example.com",
    status: "PROCESSING",
    referenceCode: "ADP-1003",
    dateKey: "2026-11-23",
    createdAt: new Date(Date.now() - 200),
    updatedAt: new Date(Date.now() - 200),
  });
  await assertFails(overBatch.commit());
});

test("User ad watches credit owner revenue atomically while blocking non-owner reads and spoofed emails", async () => {
  const OWNER_UID = "owner_kyle";
  const OWNER_EMAIL = "kyleruewall128@gmail.com";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(userRef.set(validProfilePayload(ALICE_UID)));

  const now = new Date(Date.now() - 500);
  const dateKey = "2026-11-23";
  const viewId = "view_owner_rev_1";

  // Alice watches an ad: credits Alice's balance (40% = 20c) AND credits ownerRevenueSummary/global (60% = 30c) + ownerRevenueEntries/{viewId}
  const batch = aliceDb.batch();
  batch.update(userRef, {
    availableCents: 20,
    lifetimeEarnedCents: 20,
    totalAdsWatched: 1,
    totalWatchSeconds: 15,
    currentStreakDays: 1,
    lastActiveDate: dateKey,
    updatedAt: now,
  });
  batch.set(userRef.collection("adViews").doc(viewId), {
    userId: ALICE_UID,
    campaignId: "camp_novapay_metal",
    campaignTitle: "NovaPay Titanium Cash Card",
    sponsorName: "NovaPay Financial",
    category: "Finance",
    durationSeconds: 15,
    rewardCents: 20,
    dateKey,
    hourOfDay: 10,
    createdAt: now,
  });
  batch.set(userRef.collection("dailyStats").doc(dateKey), {
    userId: ALICE_UID,
    dateKey,
    earnedCents: 20,
    adsWatched: 1,
    watchSeconds: 15,
    topCategory: "Finance",
    bonusCents: 0,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("ownerRevenueEntries").doc(viewId), {
    viewerUserId: ALICE_UID,
    campaignId: "camp_novapay_metal",
    campaignTitle: "NovaPay Titanium Cash Card",
    category: "Finance",
    grossSponsorCents: 50,
    userRewardCents: 20,
    ownerIncomeCents: 30,
    preferenceMatched: true,
    dateKey,
    createdAt: now,
  });
  batch.set(aliceDb.collection("ownerRevenueSummary").doc("global"), {
    availableIncomeCents: 30,
    lifetimeIncomeCents: 30,
    withdrawnIncomeCents: 0,
    grossSponsorCents: 50,
    totalUserRewardsCents: 20,
    totalVerifiedViews: 1,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Attempting to credit a non-40/60 split (e.g., 25c user / 25c owner on 50c gross) MUST be rejected
  const badSplitBatch = aliceDb.batch();
  const badViewId = "view_bad_split";
  badSplitBatch.set(userRef.collection("adViews").doc(badViewId), {
    userId: ALICE_UID,
    campaignId: "camp_novapay_metal",
    campaignTitle: "NovaPay Titanium Cash Card",
    sponsorName: "NovaPay Financial",
    category: "Finance",
    durationSeconds: 15,
    rewardCents: 25,
    dateKey,
    hourOfDay: 10,
    createdAt: now,
  });
  badSplitBatch.set(aliceDb.collection("ownerRevenueEntries").doc(badViewId), {
    viewerUserId: ALICE_UID,
    campaignId: "camp_novapay_metal",
    campaignTitle: "NovaPay Titanium Cash Card",
    category: "Finance",
    grossSponsorCents: 50,
    userRewardCents: 25,
    ownerIncomeCents: 25,
    preferenceMatched: true,
    dateKey,
    createdAt: now,
  });
  await assertFails(badSplitBatch.commit());

  // Non-owner Alice CANNOT read ownerRevenueSummary, ownerRevenueEntries, ownerSettings, or ownerPayouts
  await assertFails(aliceDb.collection("ownerRevenueSummary").doc("global").get());
  await assertFails(aliceDb.collection("ownerRevenueEntries").get());
  await assertFails(aliceDb.collection("ownerSettings").doc("payoutConfig").get());
  await assertFails(aliceDb.collection("ownerPayouts").get());

  // Spoofed email (email_verified: false) MUST be denied owner access
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: OWNER_EMAIL, email_verified: false })
    .firestore();
  await assertFails(spoofDb.collection("ownerRevenueSummary").doc("global").get());
  await assertFails(spoofDb.collection("ownerRevenueEntries").get());

  // Verified owner CAN read summary & entries, save payout config, and withdraw income once >= 100 cents
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("ownerRevenueSummary").doc("global").update({
      availableIncomeCents: 320,
      lifetimeIncomeCents: 320,
    });
  });

  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();

  await assertSucceeds(ownerDb.collection("ownerRevenueSummary").doc("global").get());
  await assertSucceeds(ownerDb.collection("ownerRevenueEntries").get());

  await assertSucceeds(
    ownerDb.collection("ownerSettings").doc("payoutConfig").set({
      ownerEmail: OWNER_EMAIL,
      method: "BANK_ACH",
      destinationLabel: "Checking •••• 8842",
      autoSweepEnabled: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Verified owner atomic withdrawal of $2.00 (200 cents)
  const withdrawBatch = ownerDb.batch();
  const withdrawNow = new Date(Date.now() - 100);
  withdrawBatch.update(ownerDb.collection("ownerRevenueSummary").doc("global"), {
    availableIncomeCents: 120,
    withdrawnIncomeCents: 200,
    updatedAt: withdrawNow,
  });
  withdrawBatch.set(ownerDb.collection("ownerPayouts").doc("opay_1"), {
    ownerEmail: OWNER_EMAIL,
    amountCents: 200,
    method: "BANK_ACH",
    destinationLabel: "Checking •••• 8842",
    status: "PROCESSING",
    referenceCode: "OWN-991122",
    dateKey,
    createdAt: withdrawNow,
    updatedAt: withdrawNow,
  });
  await assertSucceeds(withdrawBatch.commit());
});

test("User can securely link, update, and unlink their preferred payment withdrawal method while cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const alicePrefRef = aliceDb.collection("users").doc(ALICE_UID).collection("paymentSettings").doc("preferredMethod");
  const createdAt = new Date(Date.now() - 2000);

  // Link PayPal withdrawal method
  await assertSucceeds(
    alicePrefRef.set({
      userId: ALICE_UID,
      method: "PAYPAL",
      accountHolderName: "Alice Tester",
      maskedDestination: "alice@paypal.com",
      bankName: "",
      accountType: "",
      accountLast4: "",
      routingLast4: "",
      autoPayoutEnabled: true,
      createdAt,
      updatedAt: createdAt,
    })
  );

  await assertSucceeds(alicePrefRef.get());

  // Cross-user Bob cannot read, modify, or list Alice's paymentSettings
  const bobViewOfAliceRef = bobDb.collection("users").doc(ALICE_UID).collection("paymentSettings").doc("preferredMethod");
  await assertFails(bobViewOfAliceRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("paymentSettings").get());
  await assertFails(
    bobViewOfAliceRef.set({
      userId: ALICE_UID,
      method: "PAYPAL",
      accountHolderName: "Bob Hijack",
      maskedDestination: "bob@evil.com",
      bankName: "",
      accountType: "",
      accountLast4: "",
      routingLast4: "",
      autoPayoutEnabled: false,
      createdAt,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid BANK_ACH missing last4 digits MUST fail
  await assertFails(
    alicePrefRef.update({
      method: "BANK_ACH",
      accountHolderName: "Alice Tester",
      maskedDestination: "Chase Checking",
      bankName: "Chase Bank",
      accountType: "CHECKING",
      accountLast4: "",
      routingLast4: "",
      autoPayoutEnabled: true,
      createdAt,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid BANK_ACH update preserving createdAt succeeds
  await assertSucceeds(
    alicePrefRef.update({
      method: "BANK_ACH",
      accountHolderName: "Alice Tester",
      maskedDestination: "Chase Bank Checking •••• 4821",
      bankName: "Chase Bank",
      accountType: "CHECKING",
      accountLast4: "4821",
      routingLast4: "0210",
      autoPayoutEnabled: true,
      createdAt,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Unlinking (delete) by owner succeeds; cross-user delete fails
  await assertFails(bobViewOfAliceRef.delete());
  await assertSucceeds(alicePrefRef.delete());
});

test("Referral system: unique code registration, non-self redemption, 3-ad milestone, and atomic dual rewards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();

  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);
  const bobRef = bobDb.collection("users").doc(BOB_UID);
  await assertSucceeds(aliceRef.set(validProfilePayload(ALICE_UID, { displayName: "Alice" })));
  await assertSucceeds(bobRef.set(validProfilePayload(BOB_UID, { displayName: "Bob", email: "bob@example.com" })));

  const now = new Date(Date.now() - 5000);
  const ALICE_CODE = "SHALIC88";

  // 1. Alice registers her unique code; collection listing is denied; non-owner spoof fails
  await assertSucceeds(
    aliceDb.collection("referralCodes").doc(ALICE_CODE).set({
      code: ALICE_CODE,
      ownerUserId: ALICE_UID,
      ownerFirstName: "Alice",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("referralCodes").get());
  await assertFails(
    bobDb.collection("referralCodes").doc("SHSPOOF1").set({
      code: "SHSPOOF1",
      ownerUserId: ALICE_UID,
      ownerFirstName: "Alice",
      createdAt: now,
    })
  );

  // Exact lookup by Bob succeeds
  await assertSucceeds(bobDb.collection("referralCodes").doc(ALICE_CODE).get());

  // 2. Self-referral by Alice MUST fail; invalid code MUST fail
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referralCode: ALICE_CODE,
      referrerUserId: ALICE_UID,
      referrerFirstName: "Alice",
      inviteeUserId: ALICE_UID,
      inviteeFirstName: "Alice",
      adsCompleted: 0,
      requiredAds: 3,
      bonusCents: 50,
      status: "PENDING",
      inviteeCredited: false,
      referrerCredited: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    bobDb.collection("referrals").doc(BOB_UID).set({
      referralCode: "SHNOPE99",
      referrerUserId: ALICE_UID,
      referrerFirstName: "Alice",
      inviteeUserId: BOB_UID,
      inviteeFirstName: "Bob",
      adsCompleted: 0,
      requiredAds: 3,
      bonusCents: 50,
      status: "PENDING",
      inviteeCredited: false,
      referrerCredited: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Valid redemption by Bob succeeds; unfiltered list fails; Alice filtered query succeeds
  const bobReferralRef = bobDb.collection("referrals").doc(BOB_UID);
  await assertSucceeds(
    bobReferralRef.set({
      referralCode: ALICE_CODE,
      referrerUserId: ALICE_UID,
      referrerFirstName: "Alice",
      inviteeUserId: BOB_UID,
      inviteeFirstName: "Bob",
      adsCompleted: 0,
      requiredAds: 3,
      bonusCents: 50,
      status: "PENDING",
      inviteeCredited: false,
      referrerCredited: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("referrals").get());
  await assertFails(aliceDb.collection("referrals").get());
  await assertSucceeds(
    aliceDb.collection("referrals").where("referrerUserId", "==", ALICE_UID).get()
  );

  // Jumping straight from 0 -> 3 ads MUST fail
  await assertFails(
    bobReferralRef.update({
      adsCompleted: 3,
      status: "COMPLETED",
      inviteeCredited: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Referrer Alice cannot claim before Bob finishes 3 ads
  const orphanClaimBatch = aliceDb.batch();
  orphanClaimBatch.update(aliceDb.collection("referrals").doc(BOB_UID), {
    referrerCredited: true,
    updatedAt: new Date(Date.now() - 900),
  });
  orphanClaimBatch.update(aliceRef, {
    availableCents: 50,
    lifetimeEarnedCents: 50,
    lastReferralClaimId: BOB_UID,
    updatedAt: new Date(Date.now() - 900),
  });
  await assertFails(orphanClaimBatch.commit());

  // 4. Bob progresses ads 1 -> 2 -> 3, unlocking COMPLETED status and +50c on ad #3
  await assertSucceeds(
    bobReferralRef.update({
      adsCompleted: 1,
      status: "PENDING",
      inviteeCredited: false,
      updatedAt: new Date(Date.now() - 800),
    })
  );
  await assertSucceeds(
    bobReferralRef.update({
      adsCompleted: 2,
      status: "PENDING",
      inviteeCredited: false,
      updatedAt: new Date(Date.now() - 600),
    })
  );
  await assertSucceeds(
    bobReferralRef.update({
      adsCompleted: 3,
      status: "COMPLETED",
      inviteeCredited: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // 5. Charlie cannot hijack or claim Alice's referral bonus
  await assertFails(charlieDb.collection("referrals").doc(BOB_UID).get());
  await assertFails(
    charlieDb.collection("referrals").doc(BOB_UID).update({
      referrerCredited: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // 6. Alice atomically claims her +50c referrer bonus paired with referral transition
  const claimTime = new Date(Date.now() - 200);
  const aliceClaimBatch = aliceDb.batch();
  aliceClaimBatch.update(aliceDb.collection("referrals").doc(BOB_UID), {
    referrerCredited: true,
    updatedAt: claimTime,
  });
  aliceClaimBatch.update(aliceRef, {
    availableCents: 50,
    lifetimeEarnedCents: 50,
    lastReferralClaimId: BOB_UID,
    updatedAt: claimTime,
  });
  await assertSucceeds(aliceClaimBatch.commit());

  // 7. Double-claiming MUST fail
  const doubleClaimBatch = aliceDb.batch();
  const claimTime2 = new Date(Date.now() - 100);
  doubleClaimBatch.update(aliceDb.collection("referrals").doc(BOB_UID), {
    referrerCredited: true,
    updatedAt: claimTime2,
  });
  doubleClaimBatch.update(aliceRef, {
    availableCents: 100,
    lifetimeEarnedCents: 100,
    lastReferralClaimId: BOB_UID,
    updatedAt: claimTime2,
  });
  await assertFails(doubleClaimBatch.commit());
});


