package com.pathsathi.app.data.repository

import com.pathsathi.app.data.model.DataSource
import com.pathsathi.app.data.model.EmergencyInfo
import com.pathsathi.app.data.model.ExplorePlace
import com.pathsathi.app.data.model.FoodOption
import com.pathsathi.app.data.model.StayOption
import com.pathsathi.app.data.model.TransportOption

/**
 * All content here is bundled sample/offline data, clearly tagged DataSource.DEMO.
 * The app must never present this as live/current information (rule: no fabricated
 * live weather/transport/availability/emergency data). When an online layer is added
 * later, it should replace these calls behind the same function signatures and tag
 * results DataSource.LIVE.
 */
object DemoDataProvider {

    fun explorePlaces(destination: String): List<ExplorePlace> = listOf(
        ExplorePlace("e1", "$destination Old Town Viewpoint", "attraction",
            "A scenic overlook popular with visitors for sunrise and sunset views.",
            1.2, 1.5, 0, "Best visited early morning to avoid crowds.", DataSource.DEMO),
        ExplorePlace("e2", "$destination Nature Trail", "nature",
            "A gentle walking trail through local forest and streams.",
            3.5, 2.0, 0, "Carry water; trail can be muddy after rain.", DataSource.DEMO),
        ExplorePlace("e3", "$destination Heritage Temple", "religious",
            "A historic temple with local architectural significance.",
            2.0, 1.0, 0, "Modest clothing recommended.", DataSource.DEMO),
        ExplorePlace("e4", "$destination Local Market", "market",
            "Bustling market for local handicrafts and street food.",
            0.8, 1.5, 200, "Bargaining is common; carry small cash.", DataSource.DEMO),
        ExplorePlace("e5", "$destination Hidden Waterfall", "hidden",
            "A lesser-known waterfall reached by a short trek.",
            5.0, 3.0, 0, "Trail can be slippery; not recommended in heavy rain.", DataSource.DEMO),
        ExplorePlace("e6", "$destination Sunset Photography Point", "photography",
            "A ridge point known for panoramic sunset shots.",
            2.7, 1.0, 0, "Golden hour is the best time to visit.", DataSource.DEMO),
    )

    fun stayOptions(destination: String): List<StayOption> = listOf(
        StayOption("s1", "$destination Comfort Stay", "hotel", 1800, 1.0,
            "Mid-range hotel, sample listing.", DataSource.DEMO),
        StayOption("s2", "$destination Budget Guest House", "guest house", 700, 2.2,
            "Simple rooms, good for backpackers.", DataSource.DEMO),
        StayOption("s3", "$destination Backpacker Hostel", "hostel", 400, 1.5,
            "Dorm beds available, shared kitchen.", DataSource.DEMO),
        StayOption("s4", "$destination Riverside Camping", "camping", 300, 4.0,
            "Basic camping ground, bring your own gear.", DataSource.DEMO),
    )

    fun foodOptions(destination: String): List<FoodOption> = listOf(
        FoodOption("f1", "$destination Local Thali House", "Local", 150,
            listOf("Regional thali", "Seasonal sabzi"), true, DataSource.DEMO),
        FoodOption("f2", "$destination Street Food Corner", "Street food", 80,
            listOf("Local snacks", "Chai"), true, DataSource.DEMO),
        FoodOption("f3", "$destination Multi-Cuisine Restaurant", "Multi-cuisine", 400,
            listOf("North Indian", "Chinese"), false, DataSource.DEMO),
    )

    fun transportOptions(destination: String): List<TransportOption> = listOf(
        TransportOption("t1", "bus", "Nearest town → $destination", 150, 90,
            "Sample fare/duration; check locally for current schedules.", DataSource.DEMO),
        TransportOption("t2", "taxi", "Nearest town → $destination", 900, 60,
            "Shared taxis may be cheaper; negotiate fare beforehand.", DataSource.DEMO),
        TransportOption("t3", "walking", "Town center → $destination viewpoint", 0, 25,
            "Wear comfortable footwear.", DataSource.DEMO),
        TransportOption("t4", "local", "Within $destination", 30, 15,
            "Auto-rickshaws / local transit, sample estimate.", DataSource.DEMO),
    )

    fun emergencyInfo(destination: String): List<EmergencyInfo> = listOf(
        EmergencyInfo("em1", "helpline", "National Emergency Number", "112",
            "Works across India for police/fire/medical emergencies.", DataSource.OFFLINE_SAVED),
        EmergencyInfo("em2", "helpline", "Tourist Helpline", "1363",
            "24x7 multilingual tourist assistance helpline (India).", DataSource.OFFLINE_SAVED),
        EmergencyInfo("em3", "hospital", "$destination Nearest Hospital (sample)", "N/A",
            "Add the real local hospital contact once you arrive — this is a placeholder.", DataSource.DEMO),
        EmergencyInfo("em4", "police", "$destination Police Station (sample)", "N/A",
            "Add the real local police contact once you arrive — this is a placeholder.", DataSource.DEMO),
    )
}
