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
    data class EditActivity(val activity: KidActivity?, val date: LocalDate, val kidId: String? = null) : DialogState
    data class KidWeek(val kidId: String) : DialogState
    data object Settings : DialogState
}

private val headerDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayName = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val fullDayName = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

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
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    SectionLabel("Family calendar")
                    WeekGrid(
                        vm = vm,
                        modifier = Modifier.weight(0.45f).fillMaxWidth(),
                        onItemClick = { item -> item.event?.let { dialog = DialogState.EventDetails(it) } },
                        onDayClick = { day -> if (vm.hasCalendarPermission) dialog = DialogState.AddEvent(day) }
                    )
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Kids · today and tomorrow")
                    KidsRow(
                        vm = vm,
                        modifier = Modifier.weight(0.55f).fillMaxWidth(),
                        onItemClick = { a -> dialog = DialogState.EditActivity(a, vm.today) },
                        onWeek = { kidId -> dialog = DialogState.KidWeek(kidId) },
                        onAdd = { kidId -> dialog = DialogState.EditActivity(null, vm.today, kidId) }
                    )
                }
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
        is DialogState.EditActivity -> ActivityDialog(vm, d.activity, d.date, d.kidId) { dialog = DialogState.None }
        is DialogState.KidWeek -> KidWeekDialog(
            vm = vm,
            kidId = d.kidId,
            onEdit = { a -> dialog = DialogState.EditActivity(a, vm.today) },
            onAdd = { day -> dialog = DialogState.EditActivity(null, day, d.kidId) },
            onClose = { dialog = DialogState.None }
        )
        DialogState.Settings -> SettingsDialog(vm, onRequestCalendarPermission) { dialog = DialogState.None }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.ENGLISH),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
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

        // Week navigation (family calendar)
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

// ---------------- Family calendar week grid ----------------

@Composable
private fun WeekGrid(
    vm: FamilyViewModel,
    modifier: Modifier,
    onItemClick: (WeekItem) -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.weekDays.forEach { day ->
            DayColumn(
                day = day,
                isToday = day == vm.today,
                isPast = day.isBefore(vm.today),
                dayItems = vm.calendarItemsFor(day),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onItemClick = onItemClick,
                onHeaderClick = { onDayClick(day) }
            )
        }
    }
}

@Composable
private fun DayColumn(
    day: LocalDate,
    isToday: Boolean,
    isPast: Boolean,
    dayItems: List<WeekItem>,
    modifier: Modifier,
    onItemClick: (WeekItem) -> Unit,
    onHeaderClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = if (isPast) 0.55f else 0.85f),
        border = BorderStroke(if (isToday) 2.dp else 1.dp, if (isToday) accent else MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick)
                    .background(if (isToday) accent else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom
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
            if (dayItems.isEmpty()) {
                Box(Modifier.fillMaxSize().clickable(onClick = onHeaderClick), contentAlignment = Alignment.Center) {
                    Text("—", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dayItems, key = { it.key }) { item ->
                        WeekItemCard(item, faded = isPast) { onItemClick(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekItemCard(item: WeekItem, faded: Boolean, onClick: () -> Unit) {
    val colour = Color(item.colour)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(colour.copy(alpha = if (faded) 0.08f else 0.15f))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(colour))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                item.timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------- Kids' cards ----------------

@Composable
private fun KidsRow(
    vm: FamilyViewModel,
    modifier: Modifier,
    onItemClick: (KidActivity) -> Unit,
    onWeek: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    if (vm.data.kids.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Add your kids in Settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        vm.data.kids.forEach { kid ->
            KidCard(
                name = kid.name,
                colour = Color(kid.colour),
                today = vm.today,
                todayItems = vm.kidItemsFor(kid.id, vm.today),
                tomorrowItems = vm.kidItemsFor(kid.id, vm.today.plusDays(1)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onItemClick = onItemClick,
                onWeek = { onWeek(kid.id) },
                onAdd = { onAdd(kid.id) }
            )
        }
    }
}

@Composable
private fun KidCard(
    name: String,
    colour: Color,
    today: LocalDate,
    todayItems: List<KidActivity>,
    tomorrowItems: List<KidActivity>,
    modifier: Modifier,
    onItemClick: (KidActivity) -> Unit,
    onWeek: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colour.copy(alpha = 0.08f).compositeOver(Color.White),
        border = BorderStroke(2.dp, colour.copy(alpha = 0.55f))
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colour)
                    .padding(start = 18.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onWeek, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Week")
                }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add for $name", tint = Color.White)
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                DayHeading("Today · ${today.format(fullDayName)}", colour)
                if (todayItems.isEmpty()) {
                    Text(
                        "Nothing special today",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    todayItems.forEach { KidItemRow(it, big = true) { onItemClick(it) } }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colour.copy(alpha = 0.3f))
                Spacer(Modifier.height(10.dp))
                DayHeading("Tomorrow · ${today.plusDays(1).format(fullDayName)}", MaterialTheme.colorScheme.onSurfaceVariant)
                if (tomorrowItems.isEmpty()) {
                    Text("Nothing special", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    tomorrowItems.forEach { KidItemRow(it, big = false) { onItemClick(it) } }
                }
            }
        }
    }
}

@Composable
private fun DayHeading(text: String, colour: Color) {
    Text(
        text.uppercase(Locale.ENGLISH),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = colour,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

fun kidTimeLabel(a: KidActivity): String {
    val start = a.start ?: return ""
    val end = a.end
    return if (end != null && end.isAfter(start)) {
        "${FamilyViewModel.formatTime(start)} – ${FamilyViewModel.formatTime(end)}"
    } else {
        FamilyViewModel.formatTime(start)
    }
}

@Composable
private fun KidItemRow(item: KidActivity, big: Boolean, onClick: () -> Unit) {
    val detail = listOf(kidTimeLabel(item), item.location).filter { it.isNotBlank() }.joinToString(" · ")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (big) 6.dp else 3.dp)
    ) {
        Text(
            item.title,
            fontSize = if (big) 26.sp else 18.sp,
            lineHeight = if (big) 30.sp else 22.sp,
            fontWeight = if (big) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (detail.isNotBlank()) {
            Text(
                detail,
                fontSize = if (big) 18.sp else 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KidWeekDialog(
    vm: FamilyViewModel,
    kidId: String,
    onEdit: (KidActivity) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onClose: () -> Unit
) {
    val kid = vm.kid(kidId)
    if (kid == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val colour = Color(kid.colour)
    val monday = vm.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("${kid.name}'s week") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (0L until 7L).map { monday.plusDays(it) }.forEach { day ->
                    val isToday = day == vm.today
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            day.format(fullDayName),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) colour else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(110.dp).clickable { onAdd(day) }
                        )
                        Column(Modifier.weight(1f)) {
                            val dayItems = vm.kidItemsFor(kidId, day)
                            if (dayItems.isEmpty()) {
                                Text("—", color = MaterialTheme.colorScheme.outline)
                            }
                            dayItems.forEach { a ->
                                val t = kidTimeLabel(a)
                                Text(
                                    if (t.isBlank()) a.title else "${a.title}  ·  $t",
                                    modifier = Modifier.fillMaxWidth().clickable { onEdit(a) }.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap an item to change it, or a day to add something.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = { onAdd(vm.today) }) { Text("Add") } },
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
