package com.beranfamily.hub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.beranfamily.hub.data.KID_COLOURS

@Composable
fun ColourDot(colour: Color, size: Int = 12) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(colour))
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun SettingsDialog(vm: FamilyViewModel, onRequestCalendarPermission: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    var removeKidId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.9f)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Button(onClick = onClose) { Text("Done") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                    SectionTitle("Family name")
                    OutlinedTextField(
                        value = vm.data.familyName,
                        onValueChange = { vm.setFamilyName(it) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    SectionTitle("Kids")
                    vm.data.kids.forEach { kid ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = kid.name,
                                onValueChange = { vm.updateKid(kid.copy(name = it)) },
                                singleLine = true,
                                modifier = Modifier.width(200.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                            )
                            Spacer(Modifier.width(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                KID_COLOURS.forEach { c ->
                                    val selected = c == kid.colour
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(
                                                BorderStroke(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface),
                                                CircleShape
                                            )
                                            .clickable { vm.updateKid(kid.copy(colour = c)) }
                                    )
                                }
                            }
                            IconButton(onClick = { removeKidId = kid.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove ${kid.name}", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        if (removeKidId == kid.id) {
                            val count = vm.data.activities.count { it.kidId == kid.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Remove ${kid.name}" + (if (count > 0) " and their $count activities?" else "?"),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { removeKidId = null }) { Text("Cancel") }
                                TextButton(onClick = { vm.removeKid(kid.id); removeKidId = null }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { vm.addKid() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add a kid")
                    }

                    SectionTitle("Calendars to show")
                    if (!vm.hasCalendarPermission) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Calendar access is off.", modifier = Modifier.weight(1f))
                            Button(onClick = onRequestCalendarPermission) { Text("Allow access") }
                        }
                    } else if (vm.calendars.isEmpty()) {
                        Text(
                            "No calendars found. Sign the tablet in to your Google account and turn on calendar sync, " +
                                "then come back here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        vm.calendars.forEach { cal ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.toggleCalendarHidden(cal.id) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = cal.id !in vm.data.hiddenCalendarIds, onCheckedChange = { vm.toggleCalendarHidden(cal.id) })
                                ColourDot(Color(cal.colour))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cal.name)
                                    Text(
                                        cal.account + if (cal.writable) "" else " · view only",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Missing a calendar (e.g. one Donna shared)? Open the Google Calendar app on this tablet, " +
                                "tick that calendar and turn on Sync for it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SectionTitle("Display")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Keep screen on")
                            Text(
                                "Best when the tablet is on the wall and plugged in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = vm.data.keepScreenOn, onCheckedChange = { vm.setKeepScreenOn(it) })
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Family Hub $version · Lists and kids' activities are stored only on this tablet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
