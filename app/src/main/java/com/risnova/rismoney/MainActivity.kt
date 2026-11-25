
package com.risnova.rismoney
import android.content.Intent
import android.provider.Settings
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val purple = Color(0xFF9B72FF)
private val green = Color(0xFF35C98A)
private val red = Color(0xFFFF7185)

private fun money(value: Double): String =
    NumberFormat.getNumberInstance(Locale("th", "TH"))
        .format(value) + " ฿"

private fun loadEntries(context: Context): List<MoneyEntry> {
    return try {
        val raw = context.getSharedPreferences(
            "ris_money", Context.MODE_PRIVATE
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
                category = o.optString("category", "ทั่วไป")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveEntries(context: Context, entries: List<MoneyEntry>) {
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
    context.getSharedPreferences("ris_money", Context.MODE_PRIVATE)
        .edit().putString("entries", array.toString()).apply()
}

@Composable
fun RisMoneyApp() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadEntries(context)) }
    var dark by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ทั้งหมด") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyEntry?>(null) }
    var message by remember { mutableStateOf("") }

    fun update(next: List<MoneyEntry>) {
        entries = next
        saveEntries(context, next)
    }

    val income = entries.filter { it.type == "รายรับ" }.sumOf { it.amount }
    val expense = entries.filter { it.type == "รายจ่าย" }.sumOf { it.amount }
    val balance = income - expense
    val ratio = if (income > 0) (balance / income).coerceIn(0.0, 1.0).toFloat() else 0f

    val bg = if (dark) Color(0xFF090B13) else Color(0xFFF3F4F9)
    val panel = if (dark) Color(0xFF171A28) else Color.White
    val fg = if (dark) Color(0xFFF4F5FF) else Color(0xFF171827)
    val muted = if (dark) Color(0xFF9DA3B8) else Color(0xFF6C7182)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val lines = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readLines() ?: emptyList()

                val imported = lines.drop(1).mapNotNull { line ->
                    val parts = line.split(",").map { it.trim().trim('"') }
                    if (parts.size < 3) return@mapNotNull null
                    val amount = parts[1].toDoubleOrNull()
                        ?: return@mapNotNull null
                    val type = parts[2]
                    if (type != "รายรับ" && type != "รายจ่าย") return@mapNotNull null

                    MoneyEntry(
                        UUID.randomUUID().toString(),
                        parts[0],
                        amount,
                        type,
                        System.currentTimeMillis(),
                        parts.getOrElse(3) { "นำเข้าจากไฟล์" }
                    )
                }
                update(entries + imported)
                message = "นำเข้าสำเร็จ ${imported.size} รายการ"
            } catch (_: Exception) {
                message = "อ่านไฟล์ไม่สำเร็จ กรุณาตรวจสอบรูปแบบ CSV"
            }
        }
    }

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = purple, background = bg, surface = panel,
            onSurface = fg
        ) else lightColorScheme(
            primary = purple, background = bg, surface = panel,
            onSurface = fg
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "RIS MONEY",
                                color = purple,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("จัดการเงินของคุณ", color = muted, fontSize = 13.sp)
                        }
                        TextButton(onClick = { dark = !dark }) {
                            Text(if (dark) "☀ สว่าง" else "☾ มืด", color = fg)
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("เชื่อมต่อธนาคาร", color = fg, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("เปิดสิทธิ์เพื่อให้ RIS MONEY ตรวจจับรายการแจ้งเตือนจากธนาคาร", color = muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ตั้งค่าการเข้าถึงการแจ้งเตือน")
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ยอดเงินคงเหลือทั้งหมด", color = muted)
                            Spacer(Modifier.height(14.dp))
                            Box(
                                Modifier.size(190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val stroke = 15.dp.toPx()
                                    drawArc(
                                        color = if (dark) Color(0xFF343044) else Color(0xFFE7E1F2),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = purple,
                                        startAngle = -90f,
                                        sweepAngle = 360f * ratio,
                                        useCenter = false,
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("คงเหลือ", color = muted, fontSize = 13.sp)
                                    Text(
                                        money(balance),
                                        color = fg,
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (income > 0) "${(ratio * 100).toInt()}% ของรายรับ" else "เริ่มบันทึกรายการ",
                                        color = purple,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("รายรับทั้งหมด", color = muted, fontSize = 12.sp)
                                    Text("+ ${money(income)}", color = green, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("รายจ่ายทั้งหมด", color = muted, fontSize = 12.sp)
                                    Text("- ${money(expense)}", color = red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                editing = null
                                showDialog = true
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("+ เพิ่มรายการ", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                picker.launch(arrayOf("text/*", "application/vnd.ms-excel"))
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("นำเข้า CSV", color = fg)
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("ภาพรวมรายรับ–รายจ่าย", color = fg, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                BarItem("รายรับ", income, maxOf(income, expense, 1.0), green)
                                BarItem("รายจ่าย", expense, maxOf(income, expense, 1.0), red)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("สัดส่วนรายรับ–รายจ่าย", color = fg, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Canvas(Modifier.size(125.dp)) {
                                    val total = income + expense
                                    val incomeSweep = if (total > 0) (income / total * 360).toFloat() else 0f
                                    drawArc(
                                        color = if (dark) Color(0xFF343044) else Color(0xFFE7E1F2),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(22.dp.toPx())
                                    )
                                    if (total > 0) {
                                        drawArc(
                                            color = green,
                                            startAngle = -90f,
                                            sweepAngle = incomeSweep,
                                            useCenter = false,
                                            style = Stroke(22.dp.toPx())
                                        )
                                        drawArc(
                                            color = red,
                                            startAngle = -90f + incomeSweep,
                                            sweepAngle = 360f - incomeSweep,
                                            useCenter = false,
                                            style = Stroke(22.dp.toPx())
                                        )
                                    }
                                }
                                Spacer(Modifier.width(20.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(green, CircleShape))
                                        Spacer(Modifier.width(7.dp))
                                        Text("รายรับ ${money(income)}", color = fg, fontSize = 12.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(red, CircleShape))
                                        Spacer(Modifier.width(7.dp))
                                        Text("รายจ่าย ${money(expense)}", color = fg, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("ประวัติรายการ", color = fg, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ค้นหารายการ") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("ทั้งหมด", "รายรับ", "รายจ่าย").forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                val visible = entries
                    .filter { filter == "ทั้งหมด" || it.type == filter }
                    .filter {
                        it.title.contains(search, true) ||
                        it.category.contains(search, true)
                    }
                    .sortedByDescending { it.date }

                if (visible.isEmpty()) {
                    item {
                        Text(
                            "ยังไม่มีรายการที่ตรงกับการค้นหา",
                            color = muted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(visible, key = { it.id }) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = panel),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, color = fg, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${entry.category} • " +
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .format(Date(entry.date)),
                                        color = muted,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    (if (entry.type == "รายรับ") "+" else "-") + money(entry.amount),
                                    color = if (entry.type == "รายรับ") green else red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                TextButton(onClick = {
                                    editing = entry
                                    showDialog = true
                                }) { Text("แก้ไข", color = purple) }
                                TextButton(onClick = {
                                    update(entries.filterNot { it.id == entry.id })
                                }) { Text("ลบ", color = red) }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "RIS NOVA • RIS MONEY V2.1",
                        color = muted,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        val old = editing
        var title by remember(showDialog, old?.id) { mutableStateOf(old?.title ?: "") }
        var amountText by remember(showDialog, old?.id) {
            mutableStateOf(old?.amount?.toString() ?: "")
        }
        var type by remember(showDialog, old?.id) {
            mutableStateOf(old?.type ?: "รายจ่าย")
        }
        var category by remember(showDialog, old?.id) {
            mutableStateOf(old?.category ?: "ทั่วไป")
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (old == null) "เพิ่มรายการ" else "แก้ไขรายการ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("ชื่อรายการ") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("จำนวนเงิน (บาท)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("หมวดหมู่") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isNotBlank() && amount != null && amount > 0) {
                        val entry = MoneyEntry(
                            id = old?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            amount = amount,
                            type = type,
                            date = old?.date ?: System.currentTimeMillis(),
                            category = category.ifBlank { "ทั่วไป" }
                        )
                        val next = if (old == null) entries + entry
                            else entries.map { if (it.id == old.id) entry else it }
                        update(next)
                        showDialog = false
                    }
                }) { Text("บันทึก") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("ยกเลิก") }
            }
        )
    }

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            confirmButton = {
                TextButton(onClick = { message = "" }) { Text("ตกลง") }
            },
            text = { Text(message) }
        )
    }
}

@Composable
private fun BarItem(
    label: String,
    value: Double,
    maxValue: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(110.dp)
    ) {
        Text(money(value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.width(62.dp).height(105.dp)) {
            val barHeight = (value / maxValue).toFloat() * size.height
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, size.height - barHeight),
                size = Size(size.width, barHeight.coerceAtLeast(3f)),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
        Text(label, color = color, fontWeight = FontWeight.SemiBold)
    }
}
