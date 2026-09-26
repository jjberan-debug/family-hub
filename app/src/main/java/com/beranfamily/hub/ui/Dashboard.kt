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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface DialogState {
    data object None : DialogState
    data class AddEvent(val date: LocalDate) : DialogState
    data class EventDetails(val event: CalEvent) : DialogState
    data class EditActivity(val activity: KidActivity?, val date: LocalDate) : DialogState
    data object Settings : DialogState
}

private val headerDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayName = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

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
            Spacer(Modifier.height(10.dp))
            FilterRow(vm)
            if (!vm.hasCalendarPermission) {
                Spacer(Modifier.height(8.dp))
                PermissionBanner(onRequestCalendarPermission)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WeekGrid(
                    vm = vm,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onItemClick = { item ->
                        item.activity?.let { dialog = DialogState.EditActivity(it, vm.today) }
                        item.event?.let { dialog = DialogState.EventDetails(it) }
                    },
                    onDayClick = { day -> dialog = DialogState.EditActivity(null, day) }
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
        is DialogState.EditActivity -> ActivityDialog(vm, d.activity, d.date) { dialog = DialogState.None }
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
            Text("Kids' activity")
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

// ---------------- Filters ----------------

@Composable
private fun FilterRow(vm: FamilyViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.data.kids.forEach { kid ->
            val on = kid.id !in vm.hiddenKidIds
            LegendChip(kid.name, Color(kid.colour), on) { vm.toggleKidFilter(kid.id) }
        }
        LegendChip("Family calendar", MaterialTheme.colorScheme.primary, vm.showCalendarEvents) { vm.toggleCalendarFilter() }
    }
}

@Composable
private fun LegendChip(label: String, colour: Color, on: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (on) colour.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(1.dp, if (on) colour.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background(if (on) colour else MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (on) null else TextDecoration.LineThrough
            )
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

// ---------------- Week grid ----------------

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
                dayItems = vm.itemsFor(day),
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
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                    fontSize = 22.sp,
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
