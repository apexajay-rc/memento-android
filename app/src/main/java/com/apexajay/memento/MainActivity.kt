package com.apexajay.memento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.apexajay.memento.data.DailyMission
import com.apexajay.memento.data.DailyReview
import com.apexajay.memento.data.FocusSession
import com.apexajay.memento.data.FocusSessionStatus
import com.apexajay.memento.data.MementoDatabase
import com.apexajay.memento.data.MissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Ink = Color(0xFF08090C)
private val Panel = Color(0xFF17171D)
private val Bone = Color(0xFFF0EEEE)
private val Ash = Color(0xFFA5A0A8)
private val Blood = Color(0xFFD64A4A)
private val Green = Color(0xFF8BC59A)

class MainActivity : ComponentActivity() {
    private val database by lazy { MementoDatabase.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                background = Ink,
                surface = Panel,
                primary = Blood,
                onBackground = Bone,
                onSurface = Bone
            )) {
                MementoScreen(database)
            }
        }
    }
}

@Composable
private fun MementoScreen(database: MementoDatabase) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember(database) { database.mementoDao() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        migrateLegacyMission(context, dao)
        while (true) {
            delay(30_000)
            today = LocalDate.now()
        }
    }

    val dayKey = today.toString()
    val weekStartMillis = remember(dayKey) {
        today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val mission by dao.observeMissionForDay(dayKey).collectAsState(initial = null)
    val runningSession by dao.observeRunningSession().collectAsState(initial = null)
    val pendingReview by dao.observePendingSessionReview().collectAsState(initial = null)
    val sessions by dao.observeSessionsSince(weekStartMillis).collectAsState(initial = emptyList())
    val missions by dao.observeMissionsSince(weekStartMillis).collectAsState(initial = emptyList())
    val dailyReview by dao.observeReview(dayKey).collectAsState(initial = null)

    var missionTitle by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var plannedMinutes by remember { mutableStateOf(120) }
    var focusMinutes by remember { mutableStateOf(25) }
    var reviewOutcome by remember { mutableStateOf("PARTIAL") }
    var mainTimeLeak by remember { mutableStateOf("") }
    var tomorrowCommitment by remember { mutableStateOf("") }
    var clockNow by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(dayKey, mission?.dayKey, mission?.title, mission?.nextAction, mission?.plannedFocusMinutes) {
        if (mission == null) {
            missionTitle = ""
            nextAction = ""
            plannedMinutes = 120
        } else {
            mission?.let {
                missionTitle = it.title
                nextAction = it.nextAction
                plannedMinutes = it.plannedFocusMinutes
            }
        }
    }
    LaunchedEffect(dailyReview?.dayKey, dailyReview?.missionOutcome, dailyReview?.mainTimeLeak, dailyReview?.tomorrowCommitment) {
        dailyReview?.let {
            reviewOutcome = it.missionOutcome
            mainTimeLeak = it.mainTimeLeak
            tomorrowCommitment = it.tomorrowCommitment
        }
    }
    LaunchedEffect(runningSession?.id) {
        while (runningSession != null) {
            clockNow = System.currentTimeMillis()
            val active = runningSession
            if (active != null) {
                val elapsed = ((clockNow - active.startedAt) / 1000L).toInt()
                if (elapsed >= active.plannedMinutes * 60) {
                    dao.finishFocusSession(
                        active.id,
                        clockNow,
                        elapsed.coerceAtLeast(1)
                    )
                    MementoWidget().updateAll(context)
                    break
                }
            }
            delay(1_000)
        }
    }

    val daysAlive = ChronoUnit.DAYS.between(LocalDate.of(2006, 3, 20), today)
    val yearDays = if (today.isLeapYear) 366 else 365
    val yearProgress = (today.dayOfYear.toDouble() / yearDays.toDouble() * 100.0).toInt()
    val plannedFocus = missions.sumOf { it.plannedFocusMinutes }
    val trackedFocus = sessions
        .filter { it.status == FocusSessionStatus.COMPLETE }
        .sumOf { it.actualSeconds } / 60
    val completedMissions = missions.count { it.status == MissionStatus.COMPLETED }
    val commitmentRate = if (missions.isEmpty()) 0 else (completedMissions * 100 / missions.size)
    val focusGap = plannedFocus - trackedFocus

    val activeSession = runningSession
    val remainingSeconds = activeSession?.let {
        (it.plannedMinutes * 60 - ((clockNow - it.startedAt) / 1000L).toInt()).coerceAtLeast(0)
    } ?: 0

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("☠  MEMENTO", color = Bone, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("PERSONAL EXECUTION SYSTEM", color = Blood, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Color(0xFF393039))

            SectionLabel("TIME IS NOT REPLENISHED")
            Text("%,d DAYS".format(Locale.US, daysAlive), color = Bone, fontSize = 43.sp, fontWeight = FontWeight.Black)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("WEEKS LIVED", "%,d".format(Locale.US, daysAlive / 7), Modifier.weight(1f))
                StatCard("YEAR ELAPSED", "$yearProgress%", Modifier.weight(1f))
            }

            DarkCard {
                SectionLabel("THE CENTRAL QUESTION")
                Text("WHAT DID YOU ACTUALLY DO WITH THE TIME YOU WERE GIVEN?", color = Bone, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black)
                Text("MEMENTO MORI. MEMENTO VIVERE.", color = Blood, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            SectionLabel("01 / TODAY'S DIRECTIVE")
            OutlinedTextField(
                value = missionTitle,
                onValueChange = { missionTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What outcome matters today?") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Next physical action (specific)") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors()
            )
            Text("PLANNED FOCUS FOR THIS DIRECTIVE", color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { minutes ->
                    ChoiceButton(
                        label = "${minutes}m",
                        selected = plannedMinutes == minutes,
                        onClick = { plannedMinutes = minutes }
                    )
                }
            }
            ActionButton(
                text = if (mission == null) "COMMIT TODAY'S DIRECTIVE" else "SAVE DIRECTIVE",
                onClick = {
                    val title = missionTitle.trim()
                    val action = nextAction.trim()
                    if (title.isBlank() || action.isBlank()) {
                        scope.launch { snackbar.showSnackbar("Add an outcome and its next physical action.") }
                    } else {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            dao.upsertMission(
                                DailyMission(
                                    dayKey = dayKey,
                                    title = title,
                                    nextAction = action,
                                    plannedFocusMinutes = plannedMinutes,
                                    status = mission?.status ?: MissionStatus.OPEN,
                                    createdAt = mission?.createdAt ?: now,
                                    completedAt = mission?.completedAt
                                )
                            )
                            MementoWidget().updateAll(context)
                            snackbar.showSnackbar("Directive committed to today's record.")
                        }
                    }
                }
            )

            mission?.let { current ->
                DarkCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("CURRENT STATUS")
                        Text(
                            if (current.status == MissionStatus.COMPLETED) "COMPLETED" else "OPEN",
                            color = if (current.status == MissionStatus.COMPLETED) Green else Blood,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(current.title, color = Bone, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("NEXT: ${current.nextAction}", color = Ash, fontSize = 12.sp)
                    Text("PLAN: ${current.plannedFocusMinutes} MINUTES", color = Ash, fontSize = 10.sp)
                    if (current.status != MissionStatus.COMPLETED) {
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.completeMission(dayKey, System.currentTimeMillis())
                                    MementoWidget().updateAll(context)
                                    snackbar.showSnackbar("Mission marked complete. The record has been saved.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28543A), contentColor = Bone)
                        ) { Text("MARK DIRECTIVE COMPLETE", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            SectionLabel("02 / FOCUS SESSION")
            if (activeSession != null) {
                DarkCard {
                    Text("EXECUTION ACTIVE", color = Blood, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(formatTime(remainingSeconds), color = Bone, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text("TARGET: ${activeSession.plannedMinutes} MIN  ·  INTERRUPTIONS: ${activeSession.interruptionCount}", color = Ash, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.recordInterruption(activeSession.id)
                                    snackbar.showSnackbar("Interruption recorded.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34313A), contentColor = Bone)
                        ) { Text("LOG DISTRACTION") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val seconds = ((now - activeSession.startedAt) / 1000L).toInt().coerceAtLeast(1)
                                    dao.finishFocusSession(activeSession.id, now, seconds)
                                    MementoWidget().updateAll(context)
                                    snackbar.showSnackbar("Session ended. Record whether it produced progress.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = Color.White)
                        ) { Text("END SESSION") }
                    }
                }
            } else if (pendingReview != null) {
                PendingSessionReview(
                    session = pendingReview!!,
                    onOutcome = { productive ->
                        scope.launch {
                            dao.reviewFocusSession(pendingReview!!.id, productive)
                            snackbar.showSnackbar(if (productive) "Progress recorded." else "Session recorded without claimed progress.")
                        }
                    }
                )
            } else {
                Text("A timer is not proof of work. End each session by recording whether it produced progress.", color = Ash, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton("25 MIN", focusMinutes == 25) { focusMinutes = 25 }
                    ChoiceButton("50 MIN", focusMinutes == 50) { focusMinutes = 50 }
                }
                ActionButton(
                    text = "START ${focusMinutes}-MINUTE FOCUS",
                    enabled = mission != null && mission.status != MissionStatus.COMPLETED,
                    onClick = {
                        scope.launch {
                            val current = mission
                            if (current != null && current.status != MissionStatus.COMPLETED) {
                                dao.insertFocusSession(
                                    FocusSession(
                                        dayKey = dayKey,
                                        missionDayKey = current.dayKey,
                                        startedAt = System.currentTimeMillis(),
                                        plannedMinutes = focusMinutes
                                    )
                                )
                                MementoWidget().updateAll(context)
                                snackbar.showSnackbar("Focus session started. Do the next action, not another round of planning.")
                            }
                        }
                    }
                )
                if (mission == null) Text("Commit today's directive before starting a focus session.", color = Blood, fontSize = 11.sp)
            }

            SectionLabel("03 / EXECUTION GAP · LAST 7 DAYS")
            DarkCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("MISSIONS", "${completedMissions}/${missions.size}", Modifier.weight(1f))
                    StatCard("COMPLETION", "$commitmentRate%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("PLANNED FOCUS", "${plannedFocus}m", Modifier.weight(1f))
                    StatCard("TRACKED FOCUS", "${trackedFocus}m", Modifier.weight(1f))
                }
                Text(
                    when {
                        missions.isEmpty() -> "NO COMMITMENTS RECORDED YET."
                        focusGap > 0 -> "PLAN-TO-TRACKED GAP: ${focusGap} MINUTES"
                        else -> "TRACKED FOCUS HAS MET OR EXCEEDED THE PLAN."
                    },
                    color = if (focusGap > 0 && missions.isNotEmpty()) Blood else Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Tracked focus is elapsed timer time. Productive progress is self-reported after each session.", color = Ash, fontSize = 10.sp)
            }

            SectionLabel("04 / END-OF-DAY AUDIT")
            DarkCard {
                Text("Did you complete today's primary mission?", color = Bone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("YES", "PARTIAL", "NO").forEach { outcome ->
                        ChoiceButton(outcome, reviewOutcome == outcome) { reviewOutcome = outcome }
                    }
                }
                OutlinedTextField(
                    value = mainTimeLeak,
                    onValueChange = { mainTimeLeak = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What consumed time? (optional)") },
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = tomorrowCommitment,
                    onValueChange = { tomorrowCommitment = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tomorrow's next concrete action") },
                    colors = fieldColors()
                )
                ActionButton(
                    text = if (dailyReview == null) "SAVE DAILY AUDIT" else "UPDATE DAILY AUDIT",
                    onClick = {
                        scope.launch {
                            if (mission == null && reviewOutcome == "YES") {
                                snackbar.showSnackbar("There is no committed mission to mark as completed.")
                            } else {
                                val reviewedAt = System.currentTimeMillis()
                                dao.upsertReview(
                                    DailyReview(
                                        dayKey = dayKey,
                                        missionOutcome = reviewOutcome,
                                        mainTimeLeak = mainTimeLeak.trim(),
                                        tomorrowCommitment = tomorrowCommitment.trim(),
                                        reviewedAt = reviewedAt
                                    )
                                )
                                if (reviewOutcome == "YES" && mission != null) {
                                    dao.completeMission(dayKey, reviewedAt)
                                    MementoWidget().updateAll(context)
                                }
                                snackbar.showSnackbar("Daily audit saved to your local record.")
                            }
                        }
                    }
                )
            }

            SectionLabel("05 / RECENT RECORD")
            if (missions.isEmpty() && sessions.isEmpty()) {
                Text("No execution history yet. Start with one honest commitment.", color = Ash, fontSize = 12.sp)
            } else {
                missions.take(5).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Bone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.dayKey, color = Ash, fontSize = 10.sp)
                        }
                        Text(item.status, color = if (item.status == MissionStatus.COMPLETED) Green else Blood, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                sessions.take(5).forEach { item ->
                    Text(
                        "FOCUS · ${item.plannedMinutes}m planned · ${item.actualSeconds / 60}m tracked · ${item.interruptionCount} interruptions · ${item.productive?.let { if (it) "PROGRESS" else "NO PROGRESS" } ?: item.status}",
                        color = Ash,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF393039))
            Text("MEMENTO MORI.", color = Bone, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("MEMENTO VIVERE.", color = Blood, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("THIS IS AN ELAPSED-TIME COUNTER, NOT A DEATH PREDICTION.", color = Ash, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PendingSessionReview(session: FocusSession, onOutcome: (Boolean) -> Unit) {
    DarkCard {
        Text("SESSION ENDED · RECORD THE RESULT", color = Blood, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("${session.actualSeconds / 60} MINUTES TRACKED · ${session.interruptionCount} INTERRUPTIONS", color = Ash, fontSize = 10.sp)
        Text("Did this session produce meaningful progress?", color = Bone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onOutcome(true) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28543A), contentColor = Bone)) {
                Text("YES, PROGRESS")
            }
            Button(onClick = { onOutcome(false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34313A), contentColor = Bone)) {
                Text("NO, NOT REALLY")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun DarkCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Panel, RoundedCornerShape(12.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Ash, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Bone, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Blood else Color(0xFF29262E),
            contentColor = Bone
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blood,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF3B343A),
            disabledContentColor = Ash
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Blood,
    unfocusedBorderColor = Color(0xFF48404A),
    focusedLabelColor = Blood,
    cursorColor = Blood,
    focusedTextColor = Bone,
    unfocusedTextColor = Bone
)

private suspend fun migrateLegacyMission(
    context: android.content.Context,
    dao: com.apexajay.memento.data.MementoDao
) {
    val prefs = context.getSharedPreferences("memento", android.content.Context.MODE_PRIVATE)
    if (prefs.getBoolean("room_migration_complete", false)) return

    val oldMission = prefs.getString("mission", null)?.trim().orEmpty()
    if (oldMission.isNotBlank() && dao.getMissionForDay(LocalDate.now().toString()) == null) {
        val wasCompleted = prefs.getBoolean("completed", false)
        val now = System.currentTimeMillis()
        dao.upsertMission(
            DailyMission(
                dayKey = LocalDate.now().toString(),
                title = oldMission,
                nextAction = "Define the next physical action.",
                plannedFocusMinutes = 120,
                status = if (wasCompleted) MissionStatus.COMPLETED else MissionStatus.OPEN,
                createdAt = now,
                completedAt = if (wasCompleted) now else null
            )
        )
        MementoWidget().updateAll(context)
    }
    prefs.edit().putBoolean("room_migration_complete", true).apply()
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
