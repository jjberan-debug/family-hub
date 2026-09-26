package com.beranfamily.hub.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Saves everything the app owns to a single JSON file on the tablet. */
class Store(context: Context) {

    private val file = File(context.filesDir, "family.json")

    fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            Log.e("FamilyHub", "Could not read saved data", e)
            // Keep the unreadable file so nothing is lost for good.
            file.copyTo(File(file.parentFile, "family.broken.json"), overwrite = true)
            AppData()
        }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, "family.json.tmp")
        tmp.writeText(toJson(data).toString(2))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    // ---- JSON mapping ----

    private fun toJson(d: AppData): JSONObject = JSONObject().apply {
        put("version", 1)
        put("familyName", d.familyName)
        put("keepScreenOn", d.keepScreenOn)
        put("defaultCalendarId", d.defaultCalendarId ?: JSONObject.NULL)
        put("hiddenCalendarIds", JSONArray().apply { d.hiddenCalendarIds.forEach { put(it) } })
        put("kids", JSONArray().apply {
            d.kids.forEach { k ->
                put(JSONObject().put("id", k.id).put("name", k.name).put("colour", k.colour))
            }
        })
        put("activities", JSONArray().apply {
            d.activities.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("kidId", a.kidId)
                    put("title", a.title)
                    put("dayOfWeek", a.dayOfWeek.value)
                    put("date", a.date?.toString() ?: JSONObject.NULL)
                    put("start", a.start.toString())
                    put("end", a.end.toString())
                    put("location", a.location)
                })
            }
        })
        put("shopping", listToJson(d.shopping))
        put("todos", listToJson(d.todos))
    }

    private fun listToJson(items: List<ListItem>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)) }
    }

    private fun fromJson(o: JSONObject): AppData {
        val kids = o.optJSONArray("kids")?.let { arr ->
            (0 until arr.length()).map { i ->
                val k = arr.getJSONObject(i)
                Kid(k.getString("id"), k.getString("name"), k.getLong("colour"))
            }
        } ?: defaultKids()

        val activities = o.optJSONArray("activities")?.let { arr ->
            (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                KidActivity(
                    id = a.getString("id"),
                    kidId = a.getString("kidId"),
                    title = a.getString("title"),
                    dayOfWeek = DayOfWeek.of(a.getInt("dayOfWeek")),
                    date = if (a.isNull("date")) null else LocalDate.parse(a.getString("date")),
                    start = LocalTime.parse(a.getString("start")),
                    end = LocalTime.parse(a.getString("end")),
                    location = a.optString("location", "")
                )
            }
        } ?: emptyList()

        val hidden = o.optJSONArray("hiddenCalendarIds")?.let { arr ->
            (0 until arr.length()).map { arr.getLong(it) }.toSet()
        } ?: emptySet()

        return AppData(
            kids = kids,
            activities = activities,
            shopping = listFromJson(o.optJSONArray("shopping")),
            todos = listFromJson(o.optJSONArray("todos")),
            hiddenCalendarIds = hidden,
            defaultCalendarId = if (o.isNull("defaultCalendarId")) null else o.optLong("defaultCalendarId"),
            keepScreenOn = o.optBoolean("keepScreenOn", true),
            familyName = o.optString("familyName", "Beran Family")
        )
    }

    private fun listFromJson(arr: JSONArray?): List<ListItem> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            ListItem(it.getString("id"), it.getString("text"), it.optBoolean("done", false))
        }
    }
}
