package com.beranfamily.hub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beranfamily.hub.data.CalEvent
import com.beranfamily.hub.data.KidActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.compositeOver
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface DialogState {
    data object None : DialogState
    data class AddEvent(val date: LocalDate) : DialogState
    data class EventDetails(val event: CalEvent) : DialogState
    data class DayEvents(val date: LocalDate) : DialogState
    data class EditActivity(val activity: KidActivity?, val date: LocalDate, val kidId: String? = null) : DialogState
    data object Settings : DialogState
}

private val headerDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayName = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val fullDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** Width of the label column on the left of the planner (row names). */
private val GUTTER = 96.dp
private val GAP = 6.dp
/** How many calendar events a day cell shows before "+N more". */
private const val MAX_EVENTS_IN_CELL = 3

@Composable
fun Dashboard(vm: FamilyViewModel, onRequestCalendarPermission: () -> Unit) {
    var dialog by remember { mutableStateOf<DialogState>(DialogState.None) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(
                vm = vm,
                onAddEvent = { dialog = DialogState.AddEvent(vm.today) },
                onAddActivity = { dialog = DialogState.EditActivity(null, vm.today) },
                onSettings = { dialog = DialogState.Settings }
            )
            if (!vm.hasCalendarPermission) {
                Spacer(Modifier.height(8.dp))
                PermissionBanner(onRequestCalendarPermission)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Planner(
                    vm = vm,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onEventClick = { dialog = DialogState.EventDetails(it) },
                    onMoreClick = { day -> dialog = DialogState.DayEvents(day) },
                    onCalendarDayClick = { day -> if (vm.hasCalendarPermission) dialog = DialogState.AddEvent(day) },
                    onKidItemClick = { a, day -> dialog = DialogState.EditActivity(a, day) },
                    onKidCellClick = { kidId, day -> dialog = DialogState.EditActivity(null, day, kidId) }
                )
                Column(
                    Modifier.width(330.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ListPanel(vm, ListKind.SHOPPING, "Shopping", Icons.Filled.ShoppingCart, "Add an item…", Modifier.weight(1f))
                    ListPanel(vm, ListKind.TODO, "To do", Icons.Filled.CheckCircle, "Add a to-do…", Modifier.weight(1f))
                }
            }
        }
    }

    when (val d = dialog) {
        DialogState.None -> Unit
        is DialogState.AddEvent -> AddEventDialog(vm, d.date) { dialog = DialogState.None }
        is DialogState.EventDetails -> EventDetailsDialog(vm, d.event) { dialog = DialogState.None }
        is DialogState.DayEvents -> DayEventsDialog(
            vm = vm,
            day = d.date,
            onEventClick = { dialog = DialogState.EventDetails(it) },
            onAdd = { dialog = DialogState.AddEvent(d.date) },
            onClose = { dialog = DialogState.None }
        )
        is DialogState.EditActivity -> ActivityDialog(vm, d.activity, d.date, d.kidId) { dialog = DialogState.None }
        DialogState.Settings -> SettingsDialog(vm, onRequestCalendarPermission) { dialog = DialogState.None }
    }
}

// ---------------- Header ----------------

@Composable
private fun Header(vm: FamilyViewModel, onAddEvent: () -> Unit, onAddActivity: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(vm.data.familyName, style = MaterialTheme.typography.headlineSmall)
            Text(
                vm.today.format(headerDate),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }

        // Week navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.changeWeek(-1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            val start = vm.weekStart
            val label = when (vm.weekOffset) {
                0 -> "This week"
                1 -> "Next week"
                -1 -> "Last week"
                else -> "Week of"
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(170.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${start.format(shortDate)} – ${start.plusDays(6).format(shortDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.changeWeek(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
            if (vm.weekOffset != 0) {
                TextButton(onClick = { vm.changeWeek(0) }) { Text("Today") }
            }
        }

        Spacer(Modifier.width(20.dp))
        Text(
            FamilyViewModel.formatTime(vm.now.toLocalTime()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(20.dp))

        FilledTonalButton(onClick = onAddActivity) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Kids' item")
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onAddEvent, enabled = vm.hasCalendarPermission) {
            Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Event")
        }
        IconButton(onClick = { vm.syncNow() }, enabled = vm.hasCalendarPermission && !vm.syncing) {
            Icon(Icons.Filled.Refresh, contentDescription = "Sync calendar now")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun PermissionBanner(onAllow: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Allow calendar access to show and add Google Calendar events.",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(onClick = onAllow) { Text("Allow") }
        }
    }
}

// ---------------- Planner: calendar row + one row per kid, lined up by day ----------------

@Composable
private fun Planner(
    vm: FamilyViewModel,
    modifier: Modifier,
    onEventClick: (CalEvent) -> Unit,
    onMoreClick: (LocalDate) -> Unit,
    onCalendarDayClick: (LocalDate) -> Unit,
    onKidItemClick: (KidActivity, LocalDate) -> Unit,
    onKidCellClick: (String, LocalDate) -> Unit
) {
    val days = vm.weekDays
    val today = vm.today
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GAP)) {
        // Day headers
        Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Spacer(Modifier.width(GUTTER))
            days.forEach { day -> DayHeader(day, day == today, Modifier.weight(1f).fillMaxHeight()) }
        }

        // Family calendar
        Row(Modifier.fillMaxWidth().weight(1.3f), horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Box(Modifier.width(GUTTER).fillMaxHeight().padding(start = 4.dp, top = 8.dp)) {
                Text(
                    "FAMILY\nCALENDAR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            days.forEach { day ->
                CalendarCell(
                    dayItems = vm.calendarItemsFor(day),
                    isToday = day == today,
                    isPast = day.isBefore(today),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onEventClick = onEventClick,
                    onMoreClick = { onMoreClick(day) },
                    onEmptyClick = { onCalendarDayClick(day) }
                )
            }
        }

        // Kids
        if (vm.data.kids.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Add your kids in Settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        vm.data.kids.forEach { kid ->
            val colour = Color(kid.colour)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(GAP)) {
                Row(
                    Modifier.width(GUTTER).fillMaxHeight().padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colour))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        kid.name,
                        fontSize = 19.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                days.forEach { day ->
                    KidCell(
                        items = vm.kidItemsFor(kid.id, day),
                        colour = colour,
                        isToday = day == today,
                        isPast = day.isBefore(today),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onItemClick = { onKidItemClick(it, day) },
                        onEmptyClick = { onKidCellClick(kid.id, day) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isToday) accent else Color.Transparent)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            day.format(dayName).uppercase(Locale.ENGLISH),
            style = MaterialTheme.typography.labelLarge,
            color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            day.dayOfMonth.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CalendarCell(
    dayItems: List<WeekItem>,
    isToday: Boolean,
    isPast: Boolean,
    modifier: Modifier,
    onEventClick: (CalEvent) -> Unit,
    onMoreClick: () -> Unit,
    onEmptyClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isToday) 1f else if (isPast) 0.55f else 0.85f),
        border = BorderStroke(if (isToday) 2.dp else 1.dp, if (isToday) accent else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onEmptyClick)
                .verticalScroll(rememberScrollState())
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dayItems.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(4.dp))
            }
            val visible = if (dayItems.size > MAX_EVENTS_IN_CELL) dayItems.take(MAX_EVENTS_IN_CELL) else dayItems
            visible.forEach { item ->
                EventChip(item, faded = isPast) { item.event?.let(onEventClick) }
            }
            val hidden = dayItems.size - visible.size
            if (hidden > 0) {
                Text(
                    "+$hidden more",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onMoreClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EventChip(item: WeekItem, faded: Boolean, onClick: () -> Unit) {
    val colour = Color(item.colour)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(colour.copy(alpha = if (faded) 0.08f else 0.15f))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(colour))
        Column(Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(item.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                item.title,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KidCell(
    items: List<KidActivity>,
    colour: Color,
    isToday: Boolean,
    isPast: Boolean,
    modifier: Modifier,
    onItemClick: (KidActivity) -> Unit,
    onEmptyClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colour.copy(alpha = if (isPast) 0.04f else 0.08f).compositeOver(MaterialTheme.colorScheme.background),
        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onEmptyClick)
                .verticalScroll(rememberScrollState())
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (items.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(4.dp))
            }
            items.forEach { a ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colour.copy(alpha = if (isPast) 0.12f else 0.22f))
                        .clickable { onItemClick(a) }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        a.title,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val t = kidTimeLabel(a)
                    if (t.isNotBlank()) {
                        Text(t, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Start time only, to keep the planner cells compact. */
fun kidTimeLabel(a: KidActivity): String = a.start?.let { FamilyViewModel.formatTime(it) } ?: ""

@Composable
private fun DayEventsDialog(
    vm: FamilyViewModel,
    day: LocalDate,
    onEventClick: (CalEvent) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit
) {
    val dayItems = vm.calendarItemsFor(day)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(day.format(fullDate)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (dayItems.isEmpty()) Text("Nothing in the calendar.")
                dayItems.forEach { item ->
                    EventChip(item, faded = false) { item.event?.let(onEventClick) }
                }
            }
        },
        confirmButton = { Button(onClick = onAdd, enabled = vm.hasCalendarPermission) { Text("Add event") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

// ---------------- Lists ----------------

@Composable
private fun ListPanel(
    vm: FamilyViewModel,
    kind: ListKind,
    title: String,
    icon: ImageVector,
    placeholder: String,
    modifier: Modifier
) {
    var text by remember(kind) { mutableStateOf("") }
    val all = vm.items(kind)
    val open = all.filter { !it.done }
    val done = all.filter { it.done }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (open.isNotEmpty()) {
                    Text("${open.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        vm.addItem(kind, text)
                        text = ""
                    })
                )
                IconButton(onClick = { vm.addItem(kind, text); text = "" }, enabled = text.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
            Spacer(Modifier.height(4.dp))
            if (all.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(open + done, key = { it.id }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.toggleItem(kind, item.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.done,
                                onCheckedChange = { vm.toggleItem(kind, item.id) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                item.text,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null
                            )
                            IconButton(onClick = { vm.deleteItem(kind, item.id) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (done.isNotEmpty()) {
                OutlinedButton(onClick = { vm.clearDone(kind) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear ${done.size} ticked")
                }
            }
        }
    }
}
