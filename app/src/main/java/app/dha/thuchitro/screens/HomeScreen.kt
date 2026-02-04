package app.dha.thuchitro.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.dha.thuchitro.RecordInfo
import app.dha.thuchitro.RecordsViewModel
import app.dha.thuchitro.utils.format
import app.dha.thuchitro.utils.toVndCurrency
import java.util.Calendar
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: RecordsViewModel,
    onAddClick: () -> Unit
) {
    val monthList = remember { listOf("6 tháng") + (1..12).map { "Tháng $it" } }

    val realTimeCal = Calendar.getInstance()
    val realCurrentMonth = realTimeCal.get(Calendar.MONTH) + 1
    val realCurrentYear = realTimeCal.get(Calendar.YEAR)

    var selectedIndex by remember { mutableIntStateOf(realCurrentMonth) }
    var selectedYear by remember { mutableIntStateOf(realCurrentYear) }

    var editingRecord by remember { mutableStateOf<RecordInfo?>(null) }
    var showMemberDetail by remember { mutableStateOf(false) }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    val records by viewModel.records.observeAsState(emptyList())
    val signedIn by viewModel.signedIn.observeAsState(false)
    val userName by viewModel.userName.observeAsState("")
    val usersMap by viewModel.users.observeAsState(emptyMap())

    val summaryData = remember(records, usersMap) {
        val income = records.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = records.filter { it.amount < 0 }.sumOf { it.amount }
        val netBalance = income + expense
        val distinctUserIds = (usersMap.keys + records.map { it.userId }).distinct()
        val memberCount = distinctUserIds.size.coerceAtLeast(1)
        val perPerson = netBalance / memberCount
        Triple(expense, memberCount, perPerson)
    }
    val (totalExpense, memberCount, perPerson) = summaryData

    LaunchedEffect(selectedIndex, selectedYear, signedIn) {
        if (signedIn) {
            val monthParam = if (selectedIndex == 0) 0 else selectedIndex
            viewModel.loadRecords(month = monthParam, year = selectedYear)
        }
    }

    fun changeMonth(amount: Int) {
        if (selectedIndex == 0) {
            selectedIndex = realCurrentMonth
            selectedYear = realCurrentYear
            return
        }
        var targetMonth = selectedIndex + amount
        var targetYear = selectedYear

        if (targetMonth > 12) { targetMonth = 1; targetYear++ }
        else if (targetMonth < 1) { targetMonth = 12; targetYear-- }

        val targetTotal = targetYear * 12 + targetMonth
        val realTotal = realCurrentYear * 12 + realCurrentMonth

        if (targetTotal > realTotal) return

        selectedIndex = targetMonth
        selectedYear = targetYear
    }

    fun changeYear(amount: Int) {
        val newYear = selectedYear + amount
        if (newYear > realCurrentYear) return
        selectedYear = newYear
        if (newYear == realCurrentYear && selectedIndex > realCurrentMonth) { selectedIndex = realCurrentMonth }
        if (selectedIndex == 0) { selectedIndex = 1 }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Thêm")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 300) changeMonth(-1)
                            else if (dragOffset < -300) changeMonth(1)
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount -> change.consume(); dragOffset += dragAmount }
                    )
                }
        ) {
            if (signedIn) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Xin chào,", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(36.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { changeYear(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                            Text(text = "$selectedYear", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            val canGoNextYear = selectedYear < realCurrentYear
                            IconButton(onClick = { changeYear(1) }, enabled = canGoNextYear) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if(canGoNextYear) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.3f)) }
                        }
                    }
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                items(monthList.size) { index ->
                    val isSelected = index == selectedIndex
                    val isFutureMonth = (selectedYear == realCurrentYear && index > realCurrentMonth) || (selectedYear > realCurrentYear)
                    FilterChip(
                        selected = isSelected, onClick = { if (!isFutureMonth) selectedIndex = index },
                        label = { Text(text = monthList[index], fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.primary, disabledContainerColor = Color.Transparent, disabledLabelColor = Color.LightGray.copy(alpha = 0.5f)),
                        border = FilterChipDefaults.filterChipBorder(enabled = !isFutureMonth, selected = isSelected, borderColor = Color.LightGray), enabled = !isFutureMonth
                    )
                    if (index == 0) { Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray)) }
                }
            }

            val displayTimeTitle = if (selectedIndex == 0) "Dữ liệu 6 tháng gần nhất" else "Tháng $selectedIndex năm $selectedYear"
            Text(text = displayTimeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            if (selectedIndex != 0 && records.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(16.dp)) {
                    Box(modifier = Modifier.background(brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFFE3F2FD), Color(0xFFEDE7F6))))) {
                        Row(modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { SummaryItem(title = "Tổng chi", amount = totalExpense, color = Color(0xFFE53935)) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { showMemberDetail = true }.padding(vertical = 4.dp)) {
                                Text("Thành viên ⓘ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp, modifier = Modifier.padding(top = 4.dp)) { Text(text = "$memberCount người", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { SummaryItem(title = if (perPerson < 0) "TB Đóng" else "TB Nhận", amount = abs(perPerson), color = if (perPerson < 0) Color(0xFFE53935) else Color(0xFF43A047)) }
                        }
                    }
                }
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Không có dữ liệu", color = Color.Gray); Text(displayTimeTitle, fontSize = 12.sp, color = Color.LightGray) } }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records) { record -> RecordItem(record = record, onClick = { editingRecord = record }) }
                }
            }
        }
    }

    if (editingRecord != null) { RecordDialog(viewModel = viewModel, existingRecord = editingRecord, onDismiss = { editingRecord = null }) }
    if (showMemberDetail) { MemberDetailDialog(usersMap = usersMap, records = records, perPerson = perPerson, onDismiss = { showMemberDetail = false }) }
}

@Composable fun MemberDetailDialog(usersMap: Map<String, String>, records: List<RecordInfo>, perPerson: Long, onDismiss: () -> Unit) { val combinedUsers = remember(usersMap, records) { val allUserIds = usersMap.keys + records.map { it.userId }; allUserIds.distinct().map { uid -> val name = usersMap[uid] ?: records.find { it.userId == uid }?.userName ?: "Chưa đặt tên"; uid to name } }; Dialog(onDismissRequest = onDismiss) { Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Bảng kê chi tiết", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") } }; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Định mức:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer); Text(text = abs(perPerson).toVndCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) } }; LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) { items(combinedUsers) { (userId, userName) -> val userPaid = records.filter { it.userId == userId }.sumOf { it.amount }; val diff = userPaid - perPerson; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) { Column(modifier = Modifier.padding(12.dp)) { Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Đã đóng:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray); Text(text = abs(userPaid).toVndCurrency(showSymbol = false), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }; HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.5f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Kết quả:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); val resultText: String; val resultColor: Color; if (diff < 0) { resultText = "Nộp thêm: ${abs(diff).toVndCurrency(showSymbol = false)}"; resultColor = Color(0xFFE53935) } else if (diff > 0) { resultText = "Nhận về: ${abs(diff).toVndCurrency(showSymbol = false)}"; resultColor = Color(0xFF43A047) } else { resultText = "Đủ (0đ)"; resultColor = Color.Gray }; Text(text = resultText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = resultColor) } } } } } } } } }
@Composable fun SummaryItem(title: String, amount: Long, color: Color) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(text = abs(amount).toVndCurrency(showSymbol = false), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color) } }

// --- RECORD ITEM ĐÃ CẬP NHẬT: HIỆN "Người tạo" NẾU LÀ MAIN RECORD ---
@Composable
fun RecordItem(record: RecordInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(when { record.isRent -> Color(0xFFE3F2FD); record.amount > 0 -> Color(0xFFE8F5E9); else -> Color(0xFFFFEBEE) }), contentAlignment = Alignment.Center) {
                Text(text = when { record.isRent -> "🏠"; record.amount > 0 -> "💰"; else -> "💸" }, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.details, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = record.dateCreated.format("dd/MM HH:mm"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (record.userName.isNotEmpty()) {
                        Text(text = " • ", color = Color.Gray, fontSize = 10.sp)

                        // LOGIC: Nếu có chỉ số nước -> Đây là Main Record -> Thêm chữ "Người tạo"
                        val displayName = if (record.waterIndex != null) "Người tạo: ${record.userName}" else record.userName

                        Text(text = displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(text = record.amount.toVndCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (record.amount >= 0) Color(0xFF43A047) else Color(0xFFE53935))
        }
    }
}
