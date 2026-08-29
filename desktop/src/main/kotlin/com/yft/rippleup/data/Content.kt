package com.yft.rippleup.data

/** All static catalog data with texts copied verbatim from the RippleUp MVP PDF. */

data class DiscoverAction(
    val emoji: String,
    val title: String,
    val partner: String,          // empty on self-report tab rows
    val impact: String,           // teal line
    val note: String,             // muted line
    val difficulty: String,       // Easy | Medium | Hard
    val points: Int,
    val ptsTone: Int,             // 0 lime, 1 gold, 2 purple
    val actionKey: String,
    val done: Boolean = false,
)

data class Partner(
    val emoji: String,
    val name: String,
    val offer: String,            // "+500 pts · Recycle plastic waste"
    val promo: String,            // "Unlock 2 x Ripple Points"
    val chip: String,             // " Visiting today!" / " Visit today!"
    val chipOrange: Boolean,
)

data class RewardPartner(
    val emoji: String,
    val name: String,
    val progress: Int,            // x/5
    val promo: String,
    val ready: Boolean,
)

data class Badge(
    val emoji: String,
    val name: String,
    val desc: String,
    val earned: Boolean,
    val remaining: String = "",
)

data class Event(
    val emoji: String,
    val name: String,
    val date: String,
    val place: String,
    val going: Int,
    val registered: Boolean,
)

data class Notif(
    val emoji: String,
    val bgTone: Int,              // 0 orange, 1 mint, 2 lavender, 3 green, 4 cream
    val title: String,
    val body: String,
    val button: String = "",
    val buttonOrange: Boolean = false,
    val time: String = "",
)

data class Faq(val q: String, val a: String)

object Content {
    // ---- Onboarding (p20-22) ----
    val onboarding = listOf(
        Triple("Sustainability,\nMade Fun",
            "RippleUp turns climate positive habits into a daily game. Take small actions, earn points, and watch your impact grow.",
            "✦ 20g of CO₂ can be avoided per action on average"),
        Triple("Log Actions,\nEarn Points",
            "Self-report with a photo, or scan a Ripple QR at a partner location for verified bonus points. Every ripple counts.",
            "✦ Partner-verified actions earn 10× more points"),
        Triple("Real Rewards,\nReal Impact",
            "Compete on campus leaderboards, join community events, and redeem points for discounts at local eco-partners.",
            "✦ Earn 10% off after every 5 donations"),
    )

    // ---- Personalisation chips (p04/p29) ----
    val interests = listOf(
        "💧 Hydration", "🌱 Zero Waste", "♻️ Recycling", "🚲 Transport", "🥗 Food",
        "👕 Fashion", "🧑‍🤝‍🧑 Community", "🛒 Local Shopping", "⚡ Energy", "📚 Education",
    )
    val defaultInterests = setOf(1, 9) // Zero Waste + Education selected in the PDF

    // ---- Discover · self-report (p05) ----
    val selfReport = listOf(
        DiscoverAction("🍱", "Meal Prep", "", "Avoids packaging waste", "Instead of ordering food", "Easy", 20, 0, "food"),
        DiscoverAction("🍽️", "Finish Your Meal", "", "Saves ~0.5 kg CO₂", "Avoid food waste entirely", "Easy", 20, 0, "food", done = true),
        DiscoverAction("👜", "Reusable Bag", "", "1 plastic bag avoided", "Carry one instead of plastic", "Easy", 20, 0, "refill"),
        DiscoverAction("☕", "Own Cup or Bottle", "", "1 disposable cup avoided", "Skip single-use cups", "Easy", 20, 0, "refill"),
        DiscoverAction("🌳", "Make Compost", "", "Give your leftovers a second life.", "Make your food scraps count", "Medium", 100, 1, "compost"),
        DiscoverAction("🗑️", "Sort Recyclables", "", "Keep waste in the loop", "Sort household recyclables", "Medium", 100, 1, "recycle"),
    )

    // ---- Discover · partner-verified (p06) ----
    val partnerVerified = listOf(
        DiscoverAction("♻️", "Recycle Plastic", "EcoRecycle Hub", "Drop at partner recycling bin", "Scan the Ripple QR in-store", "Easy", 200, 0, "recycle"),
        DiscoverAction("👕", "Donate Clothing", "ThriftUp Store", "At a partner thrift store", "Drop off unused clothing", "Medium", 500, 1, "donate"),
        DiscoverAction("🥕", "Buy Local Produce", "Green Market", "From verified local vendors", "Bring your own bag", "Easy", 200, 0, "food", done = true),
        DiscoverAction("🌍", "Clean-Up Drive", "City Clean Team", "Join a local clean-up event", "Gloves on, grabbers ready", "Hard", 800, 2, "cleanup", done = true),
        DiscoverAction("🌳", "Plantation Drive", "Green Earth Initiative", "Plant a tree with the community", "Saplings provided on site", "Hard", 800, 2, "cleanup"),
    )

    // ---- Discover · partners (p07) ----
    val partners = listOf(
        Partner("♻️", "EcoRecycle Hub", "+500 pts · Recycle plastic waste", "Unlock 2 x Ripple Points", " Visiting today!", true),
        Partner("👕", "ThriftUp Store", "+500 pts · Donate unused clothing", "10% off next purchase", " Visit today!", false),
        Partner("🥗", "Green Market", "+200 pts · Buy fresh local produce", "10% off after 5 purchases", " Visit today!", false),
    )

    // ---- Rewards tab (p08) ----
    val rewards = listOf(
        RewardPartner("☕", "Green Brew", 5, "10% off on your next order", ready = true),
        RewardPartner("♻️", "EcoRecycle Hub", 2, "Unlock 2 x Ripple Points", ready = false),
        RewardPartner("🥗", "Waves Market", 1, "10% after 5 purchases", ready = false),
        RewardPartner("👕", "ThriftUp store", 1, "10% off next purchase", ready = false),
    )

    // ---- Badges tab (p39) ----
    val badges = listOf(
        Badge("🌱", "Ripple Starter", "Complete your first action", earned = true),
        Badge("⭐", "First Refill", "Refill your bottle for the first time", earned = true),
        Badge("🌊", "Plastic Saver", "Avoid 10 plastic bottles", earned = true),
        Badge("♻️", "Zero Waste Hero", "Complete 5 recycling actions", earned = false, remaining = "+150 pts remaining"),
        Badge("🏆", "Community Champion", "Attend 3 community events", earned = false, remaining = "+200 pts remaining"),
        Badge("🌍", "Ripple Ambassador", "Reach 5,000 total points", earned = false, remaining = "+500 pts remaining"),
    )

    // ---- Home · upcoming events (p03) ----
    val events = listOf(
        Event("🌍", "City Park Clean-Up", "Sat, Sept 22", "Narendra Park", 92, registered = false),
        Event("🌳", "Tree Plantation Drive", "Sun, Sept 23", "GMC Campus", 68, registered = true),
        Event("👕", "Clothing Swap Meet", "Sat, Sept 24", "Riverside Park", 51, registered = false),
    )

    // ---- Notifications sheet (p34) ----
    val notifications = listOf(
        Notif("🔥", 0, "Streak at risk!",
            "Your 6-day streak ends at midnight. Complete one action to keep it going.",
            "Log an action", buttonOrange = true, time = "now"),
        Notif("📋", 1, "1 ripple left today",
            "Weekly meal prep is still pending. You're so close to completing your list!",
            "See list", time = "2m ago"),
        Notif("🎖️", 2, "Hydration Hero milestone",
            "You've done 3 of 5 refills this week — 2 more for your reward!",
            time = "1h ago"),
        Notif("🌍", 3, "City Park Clean-Up in 2 days",
            "92 people are going. You haven't registered yet — spots are filling up!",
            "View event", time = "3h ago"),
        Notif("🎉", 4, "Coffee Discount unlocked!",
            "You earned a free coffee at EcoCafé. Show this at the counter to redeem.",
            "View reward", buttonOrange = true, time = "Yesterday"),
    )

    // ---- Event detail (p31/32) ----
    val eventDetail = Event(
        "🌍", "City Park Clean-Up", "Sat, Jun 22", "Narendra Park", 47, registered = false,
    )
    val eventDetailSteps = listOf(
        "Register your spot below",
        "Show up on the day , participate & scan Ripple QR!",
        "Earn your points and badge instantly",
    )

    // ---- Help & Support FAQ (p43) ----
    val faqs = listOf(
        Faq("How do I earn Ripple Points?",
            "Complete sustainability actions — self-reported or QR-verified at partner locations. Partner-verified actions earn more points and a Verified badge."),
        Faq("What is QR verification?",
            "Partners display a RippleUp QR code in-store. Scan it with the in-app camera while you're at the location and your action is verified instantly for bonus points."),
        Faq("How do partner rewards work?",
            "Complete actions at a partner to fill their progress ring. At 5/5 your reward unlocks — show the reward screen at the counter to redeem it."),
        Faq("Can I undo a logged action?",
            "Yes — open Today's Ripples list, tap the edit icon and remove an action before midnight. Points from removed actions are deducted from your balance."),
        Faq("Why is my streak broken?",
            "A streak counts days with at least one verified ripple. If a day passes with no action, the streak resets — but your longest streak is kept safe."),
    )

    // ---- Edit list · addable actions (p15) ----
    val addableActions = listOf(
        Triple("👜", "Own Cup or Bottle", "+20"),
        Triple("🌳", "Make Compost", "+100"),
    )
}
