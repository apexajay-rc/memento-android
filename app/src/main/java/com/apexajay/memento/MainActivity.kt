package com.apexajay.memento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

private val Ink = Color(0xFF08090C)
private val Panel = Color(0xFF17171D)
private val Bone = Color(0xFFF0EEEE)
private val Ash = Color(0xFFA5A0A8)
private val Blood = Color(0xFFD64A4A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Blood,
                onBackground = Bone, onSurface = Bone
            )) {
                MementoScreen()
            }
        }
    }
}

@Composable
private fun MementoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("memento", android.content.Context.MODE_PRIVATE) }
    var mission by remember { mutableStateOf(prefs.getString("mission", "Build something that works.") ?: "") }
    var completed by remember { mutableStateOf(prefs.getBoolean("completed", false)) }
    val scope = rememberCoroutineScope()
    val daysAlive = remember { java.time.temporal.ChronoUnit.DAYS.between(
        java.time.LocalDate.of(2006, 3, 20), java.time.LocalDate.now()
    ) }
    val yearStart = java.time.LocalDate.now().withDayOfYear(1)
    val yearDays = if (java.time.Year.isLeap(java.time.LocalDate.now().year.toLong())) 366 else 365
    val yearProgress = (java.time.temporal.ChronoUnit.DAYS.between(yearStart, java.time.LocalDate.now()).toDouble() / yearDays * 100).toInt()

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("☠  MEMENTO", color = Bone, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("MORTALITY TERMINAL", color = Blood, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Color(0xFF393039))
            Text("YOU HAVE BEEN ALIVE FOR", color = Ash, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("%,d DAYS".format(daysAlive), color = Bone, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Text("${daysAlive / 365} YEARS  ·  ${daysAlive % 365} DAYS", color = Blood, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("WEEKS LIVED", "%,d".format(daysAlive / 7), Modifier.weight(1f))
                StatCard("YEAR ELAPSED", "$yearProgress%", Modifier.weight(1f))
            }
            Column(modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE CENTRAL REMINDER", color = Blood, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("YOU ARE NOT GETTING THIS DAY BACK.", color = Bone, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
                Text("The clock does not negotiate. Potential does not automatically become reality.", color = Ash, fontSize = 13.sp)
            }
            Text("TODAY'S EXECUTION TARGET", color = Ash, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            OutlinedTextField(
                value = mission, onValueChange = { mission = it; completed = false },
                modifier = Modifier.fillMaxWidth(), label = { Text("One concrete task") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blood, unfocusedBorderColor = Color(0xFF48404A),
                    focusedLabelColor = Blood, cursorColor = Blood
                )
            )
            Button(
                onClick = {
                    prefs.edit().putString("mission", mission).putBoolean("completed", false).apply()
                    completed = false
                    scope.launch { MementoWidget().updateAll(context) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = Color.White)
            ) { Text("COMMIT TO THE MISSION", fontWeight = FontWeight.Bold) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (completed) "MISSION COMPLETED" else "NOT YET EXECUTED", color = if (completed) Color(0xFF8BC59A) else Blood, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Checkbox(checked = completed, onCheckedChange = {
                    completed = it
                    prefs.edit().putBoolean("completed", it).apply()
                    scope.launch { MementoWidget().updateAll(context) }
                }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4E9A68)))
            }
            HorizontalDivider(color = Color(0xFF393039))
            Text("MEMENTO MORI.", color = Bone, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("MEMENTO VIVERE.", color = Blood, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("REMEMBER YOU MUST DIE. REMEMBER TO LIVE.", color = Ash, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("This app does not predict your death. The counter records elapsed time only.", color = Ash, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text(label, color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Bone, fontSize = 27.sp, fontWeight = FontWeight.Black)
    }
}
