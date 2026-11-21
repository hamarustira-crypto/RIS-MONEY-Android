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
import java.util.Locale
import java.util.UUID

data class MoneyEntry(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val date: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RisMoneyApp() }
    }
}

private fun money(value: Double): String =
    NumberFormat.getNumberInstance(Locale("th", "TH")).format(value) + " ฿"

@Composable
fun RisMoneyApp() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadEntries(context)) }
    var dark by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("รายจ่าย") }

    val income = entries.filter { it.type == "รายรับ" }.sumOf { it.amount }
    val expense = entries.filter { it.type == "รายจ่าย" }.sumOf { it.amount }
    val balance = income - expense

    val bg = if (dark) Color(0xFF090B13) else Color(0xFFF4F5FA)
    val panel = if (dark) Color(0xFF171A28) else Color.White
    val fg = if (dark) Color(0xFFF4F5FF) else Color(0xFF171827)
    val muted = if (dark) Color(0xFF9DA3B8) else Color(0xFF6C7182)
    val accent = Color(0xFF9B72FF)

    MaterialTheme(colorScheme = if (dark) darkColorScheme(
        primary = accent, background = bg, surface = panel, onSurface = fg
    ) else lightColorScheme(primary = accent, background = bg, surface = panel, onSurface = fg)) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RIS MONEY", color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("จัดการเงินของคุณ", color = muted, fontSize = 13.sp)
                    }
                    TextButton(onClick = { dark = !dark }) {
                        Text(if (dark) "☀ โหมดสว่าง" else "☾ โหมดมืด", color = fg)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = panel),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("ยอดคงเหลือสุทธิ", color = muted, fontSize = 14.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(money(balance), color = fg, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("รายรับทั้งหมด", color = muted, fontSize = 12.sp)
                                Text("+ ${money(income)}", color = Color(0xFF35C98A), fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("รายจ่ายทั้งหมด", color = muted, fontSize = 12.sp)
                                Text("- ${money(expense)}", color = Color(0xFFFF7185), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("+ เพิ่มรายการ", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("รายการล่าสุด", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${entries.size} รายการ", color = muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ยังไม่มีรายการ\nกด “เพิ่มรายการ” เพื่อเริ่มบันทึก", color = muted)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entries.sortedByDescending { it.date }, key = { it.id }) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = panel),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.title, color = fg, fontWeight = FontWeight.SemiBold)
                                        Text(entry.type, color = muted, fontSize = 12.sp)
                                    }
                                    Text(
                                        (if (entry.type == "รายรับ") "+" else "-") + money(entry.amount),
                                        color = if (entry.type == "รายรับ") Color(0xFF35C98A) else Color(0xFFFF7185),
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = {
                                        entries = entries.filterNot { it.id == entry.id }
                                        saveEntries(context, entries)
                                    }) { Text("ลบ", color = muted) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(selectedType) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("เพิ่มรายการ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("ชื่อรายการ") }, singleLine = true)
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("จำนวนเงิน (บาท)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "รายรับ", onClick = { type = "รายรับ" }, label = { Text("รายรับ") })
                        FilterChip(selected = type == "รายจ่าย", onClick = { type = "รายจ่าย" }, label = { Text("รายจ่าย") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isNotBlank() && amount != null && amount > 0) {
                        entries = entries + MoneyEntry(UUID.randomUUID().toString(), title.trim(), amount, type, System.currentTimeMillis())
                        saveEntries(context, entries)
                        selectedType = type
                        showAdd = false
                    }
                }) { Text("บันทึก") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("ยกเลิก") } }
        )
    }
}

private fun loadEntries(context: Context): List<MoneyEntry> {
    return try {
        val raw = context.getSharedPreferences("ris_money", Context.MODE_PRIVATE).getString("entries", "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            MoneyEntry(o.getString("id"), o.getString("title"), o.getDouble("amount"), o.getString("type"), o.getLong("date"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveEntries(context: Context, entries: List<MoneyEntry>) {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(JSONObject().apply {
            put("id", e.id); put("title", e.title); put("amount", e.amount)
            put("type", e.type); put("date", e.date)
        })
    }
    context.getSharedPreferences("ris_money", Context.MODE_PRIVATE)
        .edit().putString("entries", array.toString()).apply()
}
