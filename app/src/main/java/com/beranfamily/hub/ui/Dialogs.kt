package com.beranfamily.hub.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.beranfamily.hub.data.CalEvent
import com.beranfamily.hub.data.KidActivity
import com.beranfamily.hub.data.newId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val longDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

private fun pickDate(context: Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth
    ).show()
}

private fun pickTime(context: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, h, m -> onPicked(LocalTime.of(h, m)) },
        initial.hour, initial.minute, false
    ).show()
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun TimeRow(start: LocalTime, end: LocalTime, onStart: (LocalTime) -> Unit, onEnd: (LocalTime) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { pickTime(context, start, onStart) }) { Text(FamilyViewModel.formatTime(start)) }
        Text("  to  ")
        OutlinedButton(onClick = { pickTime(context, end, onEnd) }) { Text(FamilyViewModel.formatTime(end)) }
    }
}

// ---------------- Kids' activity ----------------

@Composable
fun ActivityDialog(vm: FamilyViewModel, existing: KidActivity?, defaultDate: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val kids = vm.data.kids
    var kidId by remember { mutableStateOf(existing?.kidId ?: kids.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var recurring by remember { mutableStateOf(existing?.isRecurring ?: true) }
    var day by remember { mutableStateOf(existing?.dayOfWeek ?: defaultDate.dayOfWeek) }
    var date by remember { mutableStateOf(existing?.date ?: defaultDate) }
    var start by remember { mutableStateOf(existing?.start ?: LocalTime.of(16, 0)) }
    var end by remember { mutableStateOf(existing?.end ?: LocalTime.of(17, 0)) }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && kids.any { it.id == kidId }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) "Kids' activity" else "Edit activity") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Label("Who")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kids.forEach { kid ->
                        FilterChip(
                            selected = kid.id == kidId,
                            onClick = { kidId = kid.id },
                            label = { Text(kid.name) },
                            leadingIcon = { ColourDot(Color(kid.colour)) }
                        )
                    }
                }
                Label("What")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Swimming") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Label("When")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = recurring, onClick = { recurring = true }, label = { Text("Every week") })
                    FilterChip(selected = !recurring, onClick = { recurring = false }, label = { Text("One-off") })
                }
                Spacer(Modifier.height(6.dp))
                if (recurring) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.values().forEach { d ->
                            FilterChip(
                                selected = d == day,
                                onClick = { day = d },
                                label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) }
                            )
                        }
                    }
                } else {
                    OutlinedButton(onClick = { pickDate(context, date) { date = it } }) {
                        Text(date.format(longDate))
                    }
                }
                Spacer(Modifier.height(6.dp))
                TimeRow(start, end, { start = it; if (!end.isAfter(it)) end = it.plusHours(1) }, { end = it })
                Label("Where (optional)")
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                if (confirmDelete) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (existing?.isRecurring == true) "Delete this activity from every week?" else "Delete this activity?",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (confirmDelete && existing != null) {
                Button(onClick = { vm.deleteActivity(existing.id); onClose() }) { Text("Delete") }
            } else {
                Button(
                    enabled = canSave,
                    onClick = {
                        vm.saveActivity(
                            KidActivity(
                                id = existing?.id ?: newId(),
                                kidId = kidId,
                                title = title.trim(),
                                dayOfWeek = if (recurring) day else date.dayOfWeek,
                                date = if (recurring) null else date,
                                start = start,
                                end = end,
                                location = location.trim()
                            )
                        )
                        onClose()
                    }
                ) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                if (existing != null && !confirmDelete) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { if (confirmDelete) confirmDelete = false else onClose() }) { Text("Cancel") }
            }
        }
    )
}

// ---------------- Google Calendar event ----------------

@Composable
fun AddEventDialog(vm: FamilyViewModel, defaultDate: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val calendars = vm.writableCalendars
    var calendarId by remember { mutableStateOf(vm.defaultCalendar?.id) }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var allDay by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var end by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var location by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("New calendar event") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (calendars.isEmpty()) {
                    Text(
                        "No calendars on this tablet can be written to. Check the tablet is signed in to your Google account " +
                            "and that calendar sync is on."
                    )
                } else {
                Label("What")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Dinner with the Cohens") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Label("When")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickDate(context, date) { date = it } }) { Text(date.format(longDate)) }
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                    Spacer(Modifier.width(8.dp))
                    Text("All day")
                }
                if (!allDay) {
                    Spacer(Modifier.height(6.dp))
                    TimeRow(start, end, { start = it; if (!end.isAfter(it)) end = it.plusHours(1) }, { end = it })
                }
                Label("Calendar")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    calendars.forEach { cal ->
                        FilterChip(
                            selected = cal.id == calendarId,
                            onClick = { calendarId = cal.id },
                            label = { Text(cal.name) },
                            leadingIcon = { ColourDot(Color(cal.colour)) }
                        )
                    }
                }
                Label("Where (optional)")
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                }
            }
        },
        confirmButton = {
            val calId = calendarId
            Button(
                enabled = !saving && title.isNotBlank() && calId != null,
                onClick = {
                    if (calId != null) {
                        saving = true
                        vm.addEvent(calId, title.trim(), location.trim(), date, start, end, allDay) { ok ->
                            saving = false
                            if (ok) onClose() else error = "Couldn't add the event. Please try again."
                        }
                    }
                }
            ) { Text(if (saving) "Saving…" else "Add to calendar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

@Composable
fun EventDetailsDialog(vm: FamilyViewModel, event: CalEvent, onClose: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val canDelete = !event.recurring && vm.calendars.any { it.id == event.calendarId && it.writable }

    val whenText = if (event.allDay) {
        val s = Instant.ofEpochMilli(event.beginMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val e = Instant.ofEpochMilli(event.endMillis).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
        if (!e.isAfter(s)) "${s.format(longDate)} · all day" else "${s.format(longDate)} – ${e.format(longDate)}"
    } else {
        val s = Instant.ofEpochMilli(event.beginMillis).atZone(zone)
        val e = Instant.ofEpochMilli(event.endMillis).atZone(zone)
        val sameDay = s.toLocalDate() == e.toLocalDate()
        "${s.toLocalDate().format(longDate)} · ${FamilyViewModel.formatTime(s.toLocalTime())} – " +
            (if (sameDay) "" else e.toLocalDate().format(longDate) + " ") + FamilyViewModel.formatTime(e.toLocalTime())
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(event.title) },
        text = {
            Column {
                Text(whenText, style = MaterialTheme.typography.bodyLarge)
                if (event.location.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(event.location)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColourDot(Color(event.colour))
                    Spacer(Modifier.width(8.dp))
                    Text(event.calendarName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (event.recurring) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This is a repeating event. Use “Open in Calendar” to change or delete it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (confirmDelete) {
                    Spacer(Modifier.height(12.dp))
                    Text("Delete this event from Google Calendar?", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (confirmDelete) {
                Button(onClick = { vm.deleteEvent(event); onClose() }) { Text("Delete") }
            } else {
                Button(onClick = { openInCalendar(context, event); onClose() }) { Text("Open in Calendar") }
            }
        },
        dismissButton = {
            Row {
                if (canDelete && !confirmDelete) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { if (confirmDelete) confirmDelete = false else onClose() }) {
                    Text(if (confirmDelete) "Cancel" else "Close")
                }
            }
        }
    )
}

private fun openInCalendar(context: Context, event: CalEvent) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No calendar app installed; nothing else to do.
    }
}
