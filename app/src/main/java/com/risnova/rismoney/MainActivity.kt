
package com.risnova.rismoney

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MoneyEntry(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val date: Long,
    val category: String = "ทั่วไป"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RisMoneyApp() }
    }
}

private fun money(value: Double): String =
    NumberFormat.getNumberInstance(Locale("th", "TH"))
        .format(value) + " ฿"

private val purple = Color(0xFF9B72FF)
private val green = Color(0xFF35C98A)
private val red = Color(0xFFFF7185)

@Composable
fun RisMoneyApp() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadEntries(context)) }
    var dark by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyEntry?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ทั้งหมด") }
    var page by remember { mutableStateOf("หน้าหลัก") }

    val income = entries.filter { it.type == "รายรับ" }.sumOf { it.amount }
    val expense = entries.filter { it.type == "รายจ่าย" }.sumOf { it.amount }
    val balance = income - expense

    val bg = if (dark) Color(0xFF090B13) else Color(0xFFF4F5FA)
    val panel = if (dark) Color(0xFF171A28) else Color.White
    val fg = if (dark) Color(0xFFF4F5FF) else Color(0xFF171827)
    val muted = if (dark) Color(0xFF9DA3B8) else Color(0xFF6C7182)

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = purple,
            background = bg,
            surface = panel,
            onSurface = fg
        ) else lightColorScheme(
            primary = purple,
            background = bg,
            surface = panel,
            onSurface = fg
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bg
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "RIS MONEY",
                            color = purple,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "จัดการเงินของคุณ",
                            color = muted,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = { dark = !dark }) {
                        Text(
                            if (dark) "☀ สว่าง" else "☾ มืด",
                            color = fg
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = panel
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ยอดคงเหลือสุทธิ", color = muted)
                        Text(
                            money(balance),
                            color = fg,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("รายรับทั้งหมด", color = muted)
                                Text(
                                    "+ ${money(income)}",
                                    color = green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("รายจ่ายทั้งหมด", color = muted)
                                Text(
                                    "- ${money(expense)}",
                                    color = red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("หน้าหลัก", "ประวัติ", "กราฟ").forEach { item ->
                        FilterChip(
                            selected = page == item,
                            onClick = { page = item },
                            label = { Text(item) }
                        )
                    }
                }

                if (page == "กราฟ") {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "สรุปรายรับและรายจ่าย",
                        color = fg,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("รายรับ", color = green)
                            Text(money(income), color = fg)
                            val maxValue = maxOf(income, expense, 1.0)
                            LinearProgressIndicator(
                                progress = (income / maxValue).toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                color = green
                            )
                            Text("รายจ่าย", color = red)
                            Text(money(expense), color = fg)
                            LinearProgressIndicator(
                                progress = (expense / maxValue).toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                color = red
                            )
                            Divider()
                            Text(
                                "จำนวนรายการทั้งหมด: ${entries.size}",
                                color = muted
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("ค้นหารายการ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ทั้งหมด", "รายรับ", "รายจ่าย").forEach { item ->
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            editing = null
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = purple
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("+ เพิ่มรายการ", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        if (page == "ประวัติ") "ประวัติทั้งหมด"
                        else "รายการล่าสุด",
                        color = fg,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    val shown = entries
                        .filter {
                            (filter == "ทั้งหมด" || it.type == filter) &&
                            (it.title.contains(search, ignoreCase = true) ||
                             it.category.contains(search, ignoreCase = true))
                        }
                        .sortedByDescending { it.date }

                    if (shown.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ยังไม่มีรายการ", color = muted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(shown, key = { it.id }) { entry ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = panel
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    entry.title,
                                                    color = fg,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "${entry.type} • ${entry.category}",
                                                    color = muted,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    SimpleDateFormat(
                                                        "dd/MM/yyyy HH:mm",
                                                        Locale.getDefault()
                                                    ).format(Date(entry.date)),
                                                    color = muted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                (if (entry.type == "รายรับ") "+" else "-") +
                                                    money(entry.amount),
                                                color = if (entry.type == "รายรับ") green else red,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = {
                                                editing = entry
                                                showDialog = true
                                            }) {
                                                Text("แก้ไข", color = purple)
                                            }
                                            TextButton(onClick = {
                                                entries = entries.filterNot {
                                                    it.id == entry.id
                                                }
                                                saveEntries(context, entries)
                                            }) {
                                                Text("ลบ", color = red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember(editing) {
            mutableStateOf(editing?.title ?: "")
        }
        var amountText by remember(editing) {
            mutableStateOf(editing?.amount?.toString() ?: "")
        }
        var type by remember(editing) {
            mutableStateOf(editing?.type ?: "รายจ่าย")
        }
        var category by remember(editing) {
            mutableStateOf(editing?.category ?: "ทั่วไป")
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(if (editing == null) "เพิ่มรายการ" else "แก้ไขรายการ")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("ชื่อรายการ") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { c ->
                                c.isDigit() || c == '.'
                            }
                        },
                        label = { Text("จำนวนเงิน (บาท)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("หมวดหมู่ เช่น อาหาร เดินทาง") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "รายรับ",
                            onClick = { type = "รายรับ" },
                            label = { Text("รายรับ") }
                        )
                        FilterChip(
                            selected = type == "รายจ่าย",
                            onClick = { type = "รายจ่าย" },
                            label = { Text("รายจ่าย") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isNotBlank() && amount != null && amount > 0) {
                        val updated = MoneyEntry(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            amount = amount,
                            type = type,
                            date = editing?.date ?: System.currentTimeMillis(),
                            category = category.trim().ifBlank { "ทั่วไป" }
                        )

                        entries = if (editing == null) {
                            entries + updated
                        } else {
                            entries.map {
                                if (it.id == updated.id) updated else it
                            }
                        }

                        saveEntries(context, entries)
                        showDialog = false
                    }
                }) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

private fun loadEntries(context: Context): List<MoneyEntry> {
    return try {
        val raw = context.getSharedPreferences(
            "ris_money",
            Context.MODE_PRIVATE
        ).getString("entries", "[]") ?: "[]"

        val array = JSONArray(raw)

        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            MoneyEntry(
                id = o.getString("id"),
                title = o.getString("title"),
                amount = o.getDouble("amount"),
                type = o.getString("type"),
                date = o.getLong("date"),
                category = if (o.has("category")) {
                    o.getString("category")
                } else {
                    "ทั่วไป"
                }
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveEntries(
    context: Context,
    entries: List<MoneyEntry>
) {
    val array = JSONArray()

    entries.forEach { e ->
        array.put(JSONObject().apply {
            put("id", e.id)
            put("title", e.title)
            put("amount", e.amount)
            put("type", e.type)
            put("date", e.date)
            put("category", e.category)
        })
    }

    context.getSharedPreferences(
        "ris_money",
        Context.MODE_PRIVATE
    ).edit().putString("entries", array.toString()).apply()
}
