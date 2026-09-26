package com.beranfamily.hub.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reads and writes the tablet's calendars through Android's calendar provider.
 * The Google account on the tablet syncs these with Google Calendar in the background.
 */
class CalendarRepo(private val context: Context) {

    private val resolver get() = context.contentResolver

    fun calendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )
        val result = mutableListOf<CalendarInfo>()
        try {
            resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val visible = c.getInt(5) == 1
                    if (!visible) continue
                    result += CalendarInfo(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "Calendar",
                        account = c.getString(2) ?: "",
                        colour = c.getInt(3),
                        writable = c.getInt(4) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w("FamilyHub", "No calendar permission", e)
        }
        return result.sortedWith(compareBy({ it.account }, { it.name }))
    }

    /** All event occurrences (recurring ones expanded) between the two dates, end exclusive. */
    fun events(from: LocalDate, toExclusive: LocalDate, zone: ZoneId): List<CalEvent> {
        // Pad by a day either side so all-day events (stored in UTC) are not missed.
        val begin = from.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = toExclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
            it.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.RRULE
        )
        val result = mutableListOf<CalEvent>()
        try {
            resolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { c ->
                while (c.moveToNext()) {
                    val status = if (c.isNull(9)) -1 else c.getInt(9)
                    if (status == CalendarContract.Events.STATUS_CANCELED) continue
                    result += CalEvent(
                        eventId = c.getLong(0),
                        calendarId = c.getLong(1),
                        title = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(No title)",
                        location = c.getString(3) ?: "",
                        beginMillis = c.getLong(4),
                        endMillis = c.getLong(5),
                        allDay = c.getInt(6) == 1,
                        colour = c.getInt(7),
                        calendarName = c.getString(8) ?: "",
                        recurring = !c.isNull(10) && !c.getString(10).isNullOrBlank()
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w("FamilyHub", "No calendar permission", e)
        }
        return result
    }

    /** Creates an event. For all-day events [start]/[end] times are ignored. Returns the new event id. */
    fun addEvent(
        calendarId: Long,
        title: String,
        location: String,
        startDate: LocalDate,
        start: LocalDateTime,
        end: LocalDateTime,
        allDay: Boolean,
        zone: ZoneId
    ): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            if (location.isNotBlank()) put(CalendarContract.Events.EVENT_LOCATION, location)
            if (allDay) {
                val s = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val e = startDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                put(CalendarContract.Events.DTSTART, s)
                put(CalendarContract.Events.DTEND, e)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                put(CalendarContract.Events.DTSTART, start.atZone(zone).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, end.atZone(zone).toInstant().toEpochMilli())
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }
        }
        return try {
            resolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e("FamilyHub", "Could not add event", e)
            null
        }
    }

    fun deleteEvent(eventId: Long): Boolean = try {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        resolver.delete(uri, null, null) > 0
    } catch (e: Exception) {
        Log.e("FamilyHub", "Could not delete event", e)
        false
    }
}
