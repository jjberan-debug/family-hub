package com.beranfamily.hub.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Colours offered for kids (ARGB). */
val KID_COLOURS: List<Long> = listOf(
    0xFFE07A5F, // terracotta
    0xFF3D9970, // green
    0xFF5B7DD8, // blue
    0xFF9B5DE5, // purple
    0xFFF2A541, // amber
    0xFFD6457A, // pink
    0xFF2A9DB5, // teal
    0xFF6D6875  // slate
)

data class Kid(
    val id: String,
    val name: String,
    val colour: Long
)

/**
 * A kid's activity. If [date] is null it repeats every week on [dayOfWeek];
 * otherwise it is a one-off on [date].
 */
data class KidActivity(
    val id: String,
    val kidId: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val date: LocalDate?,
    val start: LocalTime,
    val end: LocalTime,
    val location: String = ""
) {
    val isRecurring: Boolean get() = date == null

    fun occursOn(day: LocalDate): Boolean =
        if (date != null) date == day else dayOfWeek == day.dayOfWeek
}

data class ListItem(
    val id: String,
    val text: String,
    val done: Boolean = false
)

data class AppData(
    val kids: List<Kid> = defaultKids(),
    val activities: List<KidActivity> = emptyList(),
    val shopping: List<ListItem> = emptyList(),
    val todos: List<ListItem> = emptyList(),
    /** Calendar ids hidden from the week view. */
    val hiddenCalendarIds: Set<Long> = emptySet(),
    /** Calendar that new events go into (null = first writable one). */
    val defaultCalendarId: Long? = null,
    val keepScreenOn: Boolean = true,
    val familyName: String = "Beran Family"
)

fun defaultKids(): List<Kid> = listOf(
    Kid(newId(), "Kid 1", KID_COLOURS[0]),
    Kid(newId(), "Kid 2", KID_COLOURS[1]),
    Kid(newId(), "Kid 3", KID_COLOURS[2])
)

/** A calendar available on the tablet (from the Android calendar provider). */
data class CalendarInfo(
    val id: Long,
    val name: String,
    val account: String,
    val colour: Int,
    val writable: Boolean
)

/** One occurrence of a Google Calendar event. */
data class CalEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val location: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val colour: Int,
    val calendarName: String,
    val recurring: Boolean
)
