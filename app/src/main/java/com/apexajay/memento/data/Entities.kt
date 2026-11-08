package com.apexajay.memento.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_missions")
data class DailyMission(
    @PrimaryKey val dayKey: String,
    val title: String,
    val nextAction: String,
    val plannedFocusMinutes: Int,
    val status: String = MissionStatus.OPEN,
    val createdAt: Long,
    val completedAt: Long? = null
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayKey: String,
    val missionDayKey: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val plannedMinutes: Int,
    val actualSeconds: Int = 0,
    val status: String = FocusSessionStatus.RUNNING,
    val interruptionCount: Int = 0,
    val productive: Boolean? = null
)

@Entity(tableName = "daily_reviews")
data class DailyReview(
    @PrimaryKey val dayKey: String,
    val missionOutcome: String,
    val mainTimeLeak: String,
    val tomorrowCommitment: String,
    val reviewedAt: Long
)

object MissionStatus {
    const val OPEN = "OPEN"
    const val COMPLETED = "COMPLETED"
}

object FocusSessionStatus {
    const val RUNNING = "RUNNING"
    const val AWAITING_REVIEW = "AWAITING_REVIEW"
    const val COMPLETE = "COMPLETE"
}
