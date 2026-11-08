package com.apexajay.memento

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.action.clickable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apexajay.memento.data.MementoDatabase
import com.apexajay.memento.data.MissionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MementoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = MementoDatabase.getInstance(context).mementoDao()
        val mission = dao.getMissionForDay(LocalDate.now().toString())
        val days = ChronoUnit.DAYS.between(LocalDate.of(2006, 3, 20), LocalDate.now())

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF08090C)))
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text("☠ MEMENTO  /  EXECUTION TERMINAL", style = TextStyle(color = ColorProvider(Color(0xFFD64A4A)), fontSize = 10.sp))
                Spacer(GlanceModifier.height(7.dp))
                Text("%,d DAYS LIVED".format(days), style = TextStyle(color = ColorProvider(Color(0xFFF0EEEE)), fontSize = 27.sp))
                Text("YOU ARE NOT GETTING THIS DAY BACK.", style = TextStyle(color = ColorProvider(Color(0xFFA5A0A8)), fontSize = 9.sp))
                Spacer(GlanceModifier.height(10.dp))
                Text("TODAY'S DIRECTIVE", style = TextStyle(color = ColorProvider(Color(0xFFA5A0A8)), fontSize = 9.sp))
                Text(
                    mission?.title ?: "NO DIRECTIVE COMMITTED",
                    style = TextStyle(color = ColorProvider(Color(0xFFF0EEEE)), fontSize = 14.sp)
                )
                Text(
                    when (mission?.status) {
                        MissionStatus.COMPLETED -> "STATUS: COMPLETED"
                        null -> "STATUS: AWAITING COMMITMENT"
                        else -> "STATUS: OPEN"
                    },
                    style = TextStyle(
                        color = ColorProvider(Color(if (mission?.status == MissionStatus.COMPLETED) 0xFF8BC59A else 0xFFD64A4A)),
                        fontSize = 10.sp
                    )
                )
                Text("OPEN MEMENTO TO RECORD WHAT YOU DO.", style = TextStyle(color = ColorProvider(Color(0xFFA5A0A8)), fontSize = 9.sp))
            }
        }
    }
}

class MementoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MementoWidget()
}
