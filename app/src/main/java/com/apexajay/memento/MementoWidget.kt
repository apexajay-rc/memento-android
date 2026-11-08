package com.apexajay.memento

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MementoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val days = ChronoUnit.DAYS.between(LocalDate.of(2006, 3, 20), LocalDate.now())
        val prefs = context.getSharedPreferences("memento", Context.MODE_PRIVATE)
        val mission = prefs.getString("mission", "Build something that works.") ?: "Build something that works."
        val done = prefs.getBoolean("completed", false)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(0xFF08090C)).padding(14.dp),
                verticalAlignment = Alignment.Vertical.Top
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.SpaceBetween) {
                    Text("☠ MEMENTO", style = TextStyle(color = ColorProvider(0xFFF0EEEE), fontSize = 16.sp))
                    Text("MORTALITY TERMINAL", style = TextStyle(color = ColorProvider(0xFFD64A4A), fontSize = 9.sp))
                }
                Spacer(GlanceModifier.height(8.dp))
                Text("YOU HAVE BEEN ALIVE FOR", style = TextStyle(color = ColorProvider(0xFFA5A0A8), fontSize = 9.sp))
                Text("%,d DAYS".format(days), style = TextStyle(color = ColorProvider(0xFFF0EEEE), fontSize = 30.sp))
                Text("YOU ARE NOT GETTING THIS DAY BACK.", style = TextStyle(color = ColorProvider(0xFFD64A4A), fontSize = 11.sp))
                Spacer(GlanceModifier.height(8.dp))
                Text("TODAY'S MISSION", style = TextStyle(color = ColorProvider(0xFFA5A0A8), fontSize = 9.sp))
                Text(mission, style = TextStyle(color = ColorProvider(0xFFF0EEEE), fontSize = 13.sp))
                Text(if (done) "✓ COMPLETED" else "○ NOT YET EXECUTED", style = TextStyle(color = ColorProvider(if (done) 0xFF8BC59A else 0xFFD64A4A), fontSize = 10.sp))
            }
        }
    }
}

class MementoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MementoWidget()
}
