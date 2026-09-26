package com.beranfamily.hub.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beranfamily.hub.data.AppData
import com.beranfamily.hub.data.CalEvent
import com.beranfamily.hub.data.CalendarInfo
import com.beranfamily.hub.data.CalendarRepo
import com.beranfamily.hub.data.Kid
import com.beranfamily.hub.data.KidActivity
import com.beranfamily.hub.data.ListItem
import com.beranfamily.hub.data.Store
import com.beranfamily.hub.data.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class ListKind { SHOPPING, TODO }

/** Something shown in a day column: a kid's activity or a calendar event. */
data class WeekItem(
    val key: String,
    val sortMinutes: Int,          // -1 for all-day items so they sit at the top
    val timeLabel: String,
    val title: String,
    val subtitle: String,
    val colour: Long,
    val activity: KidActivity? = null,
    val event: CalEvent? = null
)

class FamilyViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val calendarRepo = CalendarRepo(app)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    var data by mutableStateOf(AppData())
        private set
    var now by mutableStateOf(LocalDateTime.now())
        private set
    var weekOffset by mutableStateOf(0)
        private set
    var calendars by mutableStateOf<List<CalendarInfo>>(emptyList())
        private set
    var events by mutableStateOf<List<CalEvent>>(emptyList())
        private set
    var hasCalendarPermission by mutableStateOf(false)
        private set

    val today: LocalDate get() = now.toLocalDate()
    val weekStart: LocalDate
        get() = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset.toLong())
    val weekDays: List<LocalDate> get() = (0L until 7L).map { weekStart.plusDays(it) }

    private val saveLock = Mutex()
    private var observerRegistered = false
    private var refreshJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleRefresh()
    }

    init {
        data = store.load()
        checkPermission()
        // Clock + periodic refresh.
        viewModelScope.launch {
            var minutesSinceRefresh = 0
            while (true) {
                val before = today
                now = LocalDateTime.now()
                if (today != before) {
                    weekOffset = 0
                    scheduleRefresh()
                }
                delay(30_000)
                minutesSinceRefresh++
                if (minutesSinceRefresh >= 10) { // every ~5 minutes
                    minutesSinceRefresh = 0
                    scheduleRefresh()
                }
            }
        }
    }

    override fun onCleared() {
        if (observerRegistered) {
            getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        }
    }

    /** Watch for calendar changes (e.g. Google sync) — only possible once we have permission. */
    private fun registerObserverIfNeeded() {
        if (observerRegistered || !hasCalendarPermission) return
        try {
            getApplication<Application>().contentResolver
                .registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            observerRegistered = true
        } catch (e: SecurityException) {
            // Fall back to the periodic refresh.
        }
    }

    // ---------- Calendar ----------

    fun checkPermission() {
        val ctx = getApplication<Application>()
        hasCalendarPermission =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        registerObserverIfNeeded()
        scheduleRefresh(0)
    }

    fun scheduleRefresh(delayMs: Long = 400) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(delayMs)
            refreshCalendar()
        }
    }

    private suspend fun refreshCalendar() {
        if (!hasCalendarPermission) {
            calendars = emptyList()
            events = emptyList()
            return
        }
        val start = weekStart
        val (cals, evs) = withContext(Dispatchers.IO) {
            calendarRepo.calendars() to calendarRepo.events(start, start.plusDays(7), zone)
        }
        calendars = cals
        events = evs
    }

    fun changeWeek(delta: Int) {
        weekOffset = if (delta == 0) 0 else weekOffset + delta
        scheduleRefresh(0)
    }

    val writableCalendars: List<CalendarInfo> get() = calendars.filter { it.writable }

    val defaultCalendar: CalendarInfo?
        get() = writableCalendars.firstOrNull { it.id == data.defaultCalendarId }
            ?: writableCalendars.firstOrNull { it.account.endsWith("@gmail.com") && it.name == it.account }
            ?: writableCalendars.firstOrNull()

    fun addEvent(
        calendarId: Long,
        title: String,
        location: String,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        allDay: Boolean,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val endDateTime = if (end > start) date.atTime(end) else date.atTime(start).plusHours(1)
            val id = withContext(Dispatchers.IO) {
                calendarRepo.addEvent(calendarId, title, location, date, date.atTime(start), endDateTime, allDay, zone)
            }
            if (id != null) updateData { it.copy(defaultCalendarId = calendarId) }
            scheduleRefresh(0)
            onDone(id != null)
        }
    }

    fun deleteEvent(event: CalEvent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { calendarRepo.deleteEvent(event.eventId) }
            scheduleRefresh(0)
        }
    }

    fun toggleCalendarHidden(id: Long) = updateData {
        it.copy(hiddenCalendarIds = if (id in it.hiddenCalendarIds) it.hiddenCalendarIds - id else it.hiddenCalendarIds + id)
    }

    // ---------- Family calendar (week grid) ----------

    fun kid(id: String): Kid? = data.kids.firstOrNull { it.id == id }

    /** Calendar events for one day of the week grid. Kids' items are shown separately. */
    fun calendarItemsFor(day: LocalDate): List<WeekItem> {
        val items = mutableListOf<WeekItem>()
        run {
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            events
                .filter { it.calendarId !in data.hiddenCalendarIds }
                .forEach { e ->
                    if (e.allDay) {
                        val s = Instant.ofEpochMilli(e.beginMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        var end = Instant.ofEpochMilli(e.endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (!end.isAfter(s)) end = s.plusDays(1)
                        if (!day.isBefore(s) && day.isBefore(end)) {
                            items += WeekItem(
                                key = "e-${e.eventId}-${e.beginMillis}-$day",
                                sortMinutes = -1,
                                timeLabel = "All day",
                                title = e.title,
                                subtitle = e.location,
                                colour = e.colour.toLong() and 0xFFFFFFFFL,
                                event = e
                            )
                        }
                    } else {
                        val overlaps = if (e.endMillis > e.beginMillis) {
                            e.beginMillis < dayEnd && e.endMillis > dayStart
                        } else {
                            e.beginMillis in dayStart until dayEnd
                        }
                        if (overlaps) {
                            val startsToday = e.beginMillis >= dayStart
                            val st = Instant.ofEpochMilli(e.beginMillis).atZone(zone).toLocalTime()
                            items += WeekItem(
                                key = "e-${e.eventId}-${e.beginMillis}-$day",
                                sortMinutes = if (startsToday) st.hour * 60 + st.minute else 0,
                                timeLabel = if (startsToday) formatTime(st) else "cont.",
                                title = e.title,
                                subtitle = e.location,
                                colour = e.colour.toLong() and 0xFFFFFFFFL,
                                event = e
                            )
                        }
                    }
                }
        }
        return items.sortedWith(compareBy({ it.sortMinutes }, { it.title }))
    }

    // ---------- Kids' regular items ----------

    /** A kid's items for one day: untimed things (uniform, library bag) first, then by time. */
    fun kidItemsFor(kidId: String, day: LocalDate): List<KidActivity> =
        data.activities
            .filter { it.kidId == kidId && it.occursOn(day) }
            .sortedWith(compareBy({ it.start != null }, { it.start }, { it.title }))


    fun updateKid(kid: Kid) = updateData { d ->
        d.copy(kids = d.kids.map { if (it.id == kid.id) kid else it })
    }

    fun addKid() = updateData { d ->
        val colour = com.beranfamily.hub.data.KID_COLOURS[d.kids.size % com.beranfamily.hub.data.KID_COLOURS.size]
        d.copy(kids = d.kids + Kid(newId(), "Kid ${d.kids.size + 1}", colour))
    }

    fun removeKid(kidId: String) = updateData { d ->
        d.copy(kids = d.kids.filter { it.id != kidId }, activities = d.activities.filter { it.kidId != kidId })
    }

    fun saveActivity(activity: KidActivity) = updateData { d ->
        val exists = d.activities.any { it.id == activity.id }
        d.copy(activities = if (exists) d.activities.map { if (it.id == activity.id) activity else it } else d.activities + activity)
    }

    fun deleteActivity(id: String) = updateData { d -> d.copy(activities = d.activities.filter { it.id != id }) }

    // ---------- Lists ----------

    fun items(kind: ListKind): List<ListItem> = when (kind) {
        ListKind.SHOPPING -> data.shopping
        ListKind.TODO -> data.todos
    }

    private fun updateList(kind: ListKind, change: (List<ListItem>) -> List<ListItem>) = updateData { d ->
        when (kind) {
            ListKind.SHOPPING -> d.copy(shopping = change(d.shopping))
            ListKind.TODO -> d.copy(todos = change(d.todos))
        }
    }

    fun addItem(kind: ListKind, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        updateList(kind) { list ->
            // Re-adding something already on the list just un-ticks it.
            val existing = list.firstOrNull { it.text.equals(clean, ignoreCase = true) }
            if (existing != null) list.map { if (it.id == existing.id) it.copy(done = false) else it }
            else list + ListItem(newId(), clean)
        }
    }

    fun toggleItem(kind: ListKind, id: String) = updateList(kind) { list ->
        list.map { if (it.id == id) it.copy(done = !it.done) else it }
    }

    fun editItem(kind: ListKind, id: String, text: String) = updateList(kind) { list ->
        if (text.isBlank()) list.filter { it.id != id } else list.map { if (it.id == id) it.copy(text = text.trim()) else it }
    }

    fun deleteItem(kind: ListKind, id: String) = updateList(kind) { list -> list.filter { it.id != id } }

    fun clearDone(kind: ListKind) = updateList(kind) { list -> list.filter { !it.done } }

    // ---------- Settings ----------

    fun setKeepScreenOn(on: Boolean) = updateData { it.copy(keepScreenOn = on) }

    fun setFamilyName(name: String) = updateData { it.copy(familyName = name) }

    // ---------- Persistence ----------

    private fun updateData(change: (AppData) -> AppData) {
        val updated = change(data)
        data = updated
        viewModelScope.launch(Dispatchers.IO) {
            // Always write the latest state, one write at a time.
            saveLock.withLock { store.save(data) }
        }
    }

    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        fun formatTime(t: LocalTime): String = t.format(timeFormat).lowercase(Locale.ENGLISH)
    }
}
