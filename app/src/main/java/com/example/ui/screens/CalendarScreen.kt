package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CalendarEventEntity
import com.example.ui.CalendarViewMode
import com.example.ui.SecretaryViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DangerRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessEmerald
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: SecretaryViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.allCalendarEvents.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val viewMode by viewModel.calendarViewMode.collectAsState()

    // Calculate dates around current selected date
    val calendarDays = getDaysAround(selectedDateMillis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with Month Title and Add Event Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                val monthYearStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                Text(
                    text = monthYearStr,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Deadlines, Work Sessions & Meetings",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }

            IconButton(
                onClick = { viewModel.openAddEvent() },
                modifier = Modifier.testTag("add_calendar_event_btn")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Event", tint = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Mode Switcher
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                CalendarViewMode.AGENDA to "Agenda",
                CalendarViewMode.DAY to "Day",
                CalendarViewMode.WEEK to "Week",
                CalendarViewMode.MONTH to "Month"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { viewModel.setCalendarViewMode(mode) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Interactive Date Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(calendarDays) { dayCal ->
                val isSelected = isSameDay(dayCal.timeInMillis, selectedDateMillis)
                val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(dayCal.time)
                val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(dayCal.time)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(72.dp)
                        .clickable { viewModel.setSelectedDate(dayCal.timeInMillis) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayOfWeek,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Slate600
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayOfMonth,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter events for the day or all agenda
        val displayedEvents = if (viewMode == CalendarViewMode.DAY) {
            events.filter { isSameDay(it.startTimeMillis, selectedDateMillis) }
        } else {
            events.sortedBy { it.startTimeMillis }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (displayedEvents.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No events scheduled",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Deadlines extracted from your documents appear automatically here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedEvents, key = { it.id }) { event ->
                    CalendarEventCard(
                        event = event,
                        onDelete = { viewModel.deleteCalendarEvent(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEventEntity,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(event.startTimeMillis))
    val (eventTypeLabel, eventColor) = when (event.eventType) {
        "SUBMISSION_DEADLINE" -> "Submission Deadline" to DangerRose
        "MEETING_DATE" -> "Meeting" to Color(0xFF8B5CF6)
        "WORK_SESSION" -> "Work Session" to PrimaryBlue
        "MILESTONE" -> "Milestone" to SuccessEmerald
        else -> "Deadline Reminder" to AccentAmber
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = eventColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = eventTypeLabel,
                            color = eventColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Schedule, contentDescription = null, tint = Slate600, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = dateStr, fontSize = 11.sp, color = Slate600)
                }
            }
        }
    }
}

private fun getDaysAround(currentMillis: Long): List<Calendar> {
    val list = mutableListOf<Calendar>()
    for (i in -3..10) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentMillis
            add(Calendar.DAY_OF_YEAR, i)
        }
        list.add(cal)
    }
    return list
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
