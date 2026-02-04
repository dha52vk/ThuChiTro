package app.dha.thuchitro.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dha.thuchitro.R
import app.dha.thuchitro.RecordInfo
import app.dha.thuchitro.RecordsViewModel
import app.dha.thuchitro.RentSplitItem
import app.dha.thuchitro.utils.format
import app.dha.thuchitro.utils.formatCurrencyInput
import app.dha.thuchitro.utils.toVndCurrency
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

enum class RecordType { EXPENSE, INCOME, RENT }

class SubRecordState(
    userId: String? = null,
    initialAmount: String = "-"
) {
    var userId by mutableStateOf(userId)
    var amount by mutableStateOf(
        TextFieldValue(text = initialAmount, selection = TextRange(initialAmount.length))
    )
}

@Composable
fun RecordDialog(
    viewModel: RecordsViewModel,
    existingRecord: RecordInfo? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val usersMap by viewModel.users.observeAsState(emptyMap())

    val isRentRecord = existingRecord?.isRent == true
    val initialType = remember {
        when {
            existingRecord == null -> RecordType.EXPENSE
            isRentRecord -> RecordType.RENT
            existingRecord.amount > 0 -> RecordType.INCOME
            else -> RecordType.EXPENSE
        }
    }
    var selectedType by remember { mutableStateOf(initialType) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f).padding(vertical = 24.dp).heightIn(max = 800.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (existingRecord == null) "Thêm mới" else (if (isRentRecord) "Chi tiết tiền trọ" else "Chỉnh sửa"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (existingRecord != null && isRentRecord) {
                    RentDetailView(
                        record = existingRecord,
                        onDelete = { showDeleteConfirm = true },
                        onClose = onDismiss
                    )
                }
                else if (existingRecord == null) {
                    Row(modifier = Modifier.fillMaxWidth().height(45.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(4.dp)) {
                        TypeButton("Chi tiêu", selectedType == RecordType.EXPENSE, Color(0xFFE53935), { selectedType = RecordType.EXPENSE }, Modifier.weight(1f))
                        TypeButton("Thu nhập", selectedType == RecordType.INCOME, Color(0xFF43A047), { selectedType = RecordType.INCOME }, Modifier.weight(1f))
                        TypeButton("Tiền trọ", selectedType == RecordType.RENT, MaterialTheme.colorScheme.primary, { selectedType = RecordType.RENT }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedType == RecordType.RENT) {
                        RentForm(
                            viewModel = viewModel,
                            existingRecord = null,
                            usersMap = usersMap,
                            isLoading = isLoading,
                            onConfirmBatch = { details, eNew, wNew, room, service, ePrice, wPrice, customDate, items ->
                                isLoading = true
                                viewModel.addRentBatch(details, customDate, eNew, wNew, room, service, ePrice, wPrice, items, { isLoading = false; onDismiss() }, { isLoading = false; Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() })
                            },
                            onDelete = { },
                            onCancel = onDismiss
                        )
                    } else {
                        NormalForm(
                            type = selectedType,
                            existingRecord = null,
                            isLoading = isLoading,
                            onConfirm = { content, amount ->
                                isLoading = true
                                val finalAmount = if (selectedType == RecordType.EXPENSE) -abs(amount) else abs(amount)
                                viewModel.addRecord(content, finalAmount, onAdded = { isLoading = false; onDismiss() }, onFailed = { isLoading = false; Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() })
                            },
                            onDelete = {},
                            onCancel = onDismiss
                        )
                    }
                }
                else {
                    NormalForm(
                        type = initialType,
                        existingRecord = existingRecord,
                        isLoading = isLoading,
                        onConfirm = { content, amount ->
                            isLoading = true
                            val finalAmount = if (initialType == RecordType.EXPENSE) -abs(amount) else abs(amount)
                            viewModel.updateRecord(existingRecord.id, content, finalAmount, onSuccess = { isLoading = false; onDismiss(); Toast.makeText(context, "Đã cập nhật", Toast.LENGTH_SHORT).show() }, onFailed = { isLoading = false; Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() })
                        },
                        onDelete = { showDeleteConfirm = true },
                        onCancel = onDismiss
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && existingRecord != null) {
        val deleteTitle = if (existingRecord.isRent) "Xóa toàn bộ tháng?" else "Xóa mục này?"
        val deleteMessage = if (existingRecord.isRent) "Hành động này sẽ xóa TẤT CẢ các mục Tiền Trọ trong tháng này." else "Bạn có chắc chắn muốn xóa không?"

        AlertDialog(
            onDismissRequest = { if (!isLoading) showDeleteConfirm = false },
            title = { Text(deleteTitle, fontWeight = FontWeight.Bold) },
            text = { Text(deleteMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        viewModel.deleteRecord(
                            targetRecord = existingRecord,
                            onSuccess = { isLoading = false; showDeleteConfirm = false; onDismiss(); Toast.makeText(context, "Đã xóa thành công", Toast.LENGTH_SHORT).show() },
                            onFailed = { isLoading = false; Toast.makeText(context, "Lỗi xóa: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Xóa vĩnh viễn")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }, enabled = !isLoading) { Text("Hủy") } }
        )
    }
}

@Composable
fun RentDetailView(record: RecordInfo, onDelete: () -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = if(record.amount > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if(record.amount > 0) "THU NHẬP" else "CHI TIÊU",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = record.amount.toVndCurrency(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if(record.amount > 0) Color(0xFF43A047) else Color(0xFFE53935),
                    textAlign = TextAlign.Center
                )
            }
        }
        val userLabel = if (record.waterIndex != null) "Người tạo:" else "Người đóng:"
        InfoRow(userLabel, record.userName)
        InfoRow("Nội dung:", record.details)
        InfoRow("Ngày tạo:", record.dateCreated.format("dd/MM/yyyy HH:mm"))

        if (record.elecIndex != null) {
            HorizontalDivider()
            Text("Thông tin chỉ số:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            InfoRow("Điện mới:", "${record.elecIndex}")
            InfoRow("Nước mới:", "${record.waterIndex}")

            // --- SỬA LỖI SAFE CALL: Bỏ dấu ? thừa vì đã check != null ---
            if (record.roomPrice != null) InfoRow("Tiền phòng:", record.roomPrice.toVndCurrency())
            if (record.serviceFee != null) InfoRow("Tiền dịch vụ:", record.serviceFee.toVndCurrency())
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Xóa bản ghi này") }
            Button(onClick = onClose) { Text("Đóng") }
        }
    }
}

@Composable fun InfoRow(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium); Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium) } }

@Composable
fun RentForm(
    viewModel: RecordsViewModel,
    existingRecord: RecordInfo?,
    usersMap: Map<String, String>,
    isLoading: Boolean,
    onConfirmBatch: (String, Long, Long, Long, Long, Long, Long, Date?, List<RentSplitItem>) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var roomPrice by remember { mutableStateOf((existingRecord?.roomPrice ?: 3000000).toString()) }
    var serviceFee by remember { mutableStateOf((existingRecord?.serviceFee ?: 100000).toString()) }
    var elecOld by remember { mutableStateOf("") }
    var elecNew by remember { mutableStateOf((existingRecord?.elecIndex ?: "").toString()) }
    var elecPrice by remember { mutableStateOf((existingRecord?.elecPrice ?: 3500).toString()) }
    var waterOld by remember { mutableStateOf("") }
    var waterNew by remember { mutableStateOf((existingRecord?.waterIndex ?: "").toString()) }
    var waterPrice by remember { mutableStateOf((existingRecord?.waterPrice ?: 25000).toString()) }

    val subRecordStates = remember { mutableStateListOf(SubRecordState(initialAmount = "-")) }

    LaunchedEffect(Unit) {
        val lastRecord = viewModel.getLastRentInfo()
        if (lastRecord != null && lastRecord.id != existingRecord?.id) {
            if (lastRecord.elecIndex != null) elecOld = lastRecord.elecIndex.toString()
            if (lastRecord.waterIndex != null) waterOld = lastRecord.waterIndex.toString()
        }
    }

    val totalAmount by remember(roomPrice, serviceFee, elecOld, elecNew, elecPrice, waterOld, waterNew, waterPrice) {
        derivedStateOf {
            val room = roomPrice.toLongOrNull() ?: 0L
            val service = serviceFee.toLongOrNull() ?: 0L
            val eVal = ((elecNew.toLongOrNull() ?: 0) - (elecOld.toLongOrNull() ?: 0)).coerceAtLeast(0) * (elecPrice.toLongOrNull() ?: 0)
            val wVal = ((waterNew.toLongOrNull() ?: 0) - (waterOld.toLongOrNull() ?: 0)).coerceAtLeast(0) * (waterPrice.toLongOrNull() ?: 0)
            room + service + eVal + wVal
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallInput(value = roomPrice, onValueChange = { roomPrice = it }, label = "Tiền phòng", modifier = Modifier.weight(1.5f))
            SmallInput(value = serviceFee, onValueChange = { serviceFee = it }, label = "Dịch vụ", modifier = Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Text("Điện (Cũ - Mới - Giá)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallInput(value = elecOld, onValueChange = { elecOld = it }, label = "Số cũ", modifier = Modifier.weight(1f)); SmallInput(value = elecNew, onValueChange = { elecNew = it }, label = "Số mới", modifier = Modifier.weight(1f)); SmallInput(value = elecPrice, onValueChange = { elecPrice = it }, label = "Giá/số", modifier = Modifier.weight(1f)) }
        Text("Nước (Cũ - Mới - Giá)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallInput(value = waterOld, onValueChange = { waterOld = it }, label = "Số cũ", modifier = Modifier.weight(1f)); SmallInput(value = waterNew, onValueChange = { waterNew = it }, label = "Số mới", modifier = Modifier.weight(1f)); SmallInput(value = waterPrice, onValueChange = { waterPrice = it }, label = "Giá/số", modifier = Modifier.weight(1f)) }

        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Tổng bill:", style = MaterialTheme.typography.titleMedium); Text(text = totalAmount.toVndCurrency(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Danh sách chi/thu:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Mẹo: Mặc định (-): Chi. Xóa dấu trừ: Thu.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
            }
        }

        subRecordStates.forEachIndexed { index, state ->
            SubRecordRow(state = state, usersMap = usersMap, onRemove = { subRecordStates.removeAt(index) })
        }
        OutlinedButton(
            onClick = { subRecordStates.add(SubRecordState(initialAmount = "-")) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thêm dòng")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Lưu vào tháng trước", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        ActionButtons(
            isLoading = isLoading, isEditing = existingRecord != null, onCancel = onCancel, onDelete = onDelete,
            onConfirm = {
                val eNewVal = elecNew.toLongOrNull() ?: 0L; val wNewVal = waterNew.toLongOrNull() ?: 0L
                val ePriceVal = elecPrice.toLongOrNull() ?: 0L; val wPriceVal = waterPrice.toLongOrNull() ?: 0L
                val roomVal = roomPrice.toLongOrNull() ?: 0L; val serviceVal = serviceFee.toLongOrNull() ?: 0L

                // Tính toán ngay trong confirm để tránh warning "Assigned value never read"
                val eOldVal = elecOld.toLongOrNull() ?: 0L
                val wOldVal = waterOld.toLongOrNull() ?: 0L
                val eUsed = (eNewVal - eOldVal).coerceAtLeast(0)
                val wUsed = (wNewVal - wOldVal).coerceAtLeast(0)

                val details = "Tiền trọ (Điện: ${eUsed}s, Nước: ${wUsed}s)"
                val recordDate = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time
                val items = subRecordStates.map { RentSplitItem(it.userId ?: "", it.amount.text) }

                onConfirmBatch(details, eNewVal, wNewVal, roomVal, serviceVal, ePriceVal, wPriceVal, recordDate, items)
            },
            confirmColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SubRecordRow(state: SubRecordState, usersMap: Map<String, String>, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val textVal = state.amount.text
    val isExpense = textVal.startsWith("-")
    val textColor = if (textVal.isNotEmpty() && textVal != "-") { if (isExpense) Color(0xFFE53935) else Color(0xFF43A047) } else Color.Unspecified

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (state.userId != null) (usersMap[state.userId] ?: "Không xác định") else "Chọn người",
                onValueChange = {}, readOnly = true, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true
            )
            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                usersMap.forEach { (uid, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { state.userId = uid; expanded = false }) }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = state.amount,
            onValueChange = { newValue ->
                val newText = newValue.text
                val isNegative = newText.startsWith("-")
                // --- SỬA REGEX: [^\d] -> \D ---
                val cleanText = newText.replace(Regex("\\D"), "")

                if (cleanText.isEmpty()) { state.amount = if (isNegative) TextFieldValue("-", TextRange(1)) else TextFieldValue("") }
                else { val formatted = formatCurrencyInput(TextFieldValue(cleanText, TextRange(cleanText.length))); val finalText = if (isNegative) "-${formatted.text}" else formatted.text; state.amount = TextFieldValue(finalText, TextRange(finalText.length)) }
            },
            placeholder = { Text("Chi(-)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = TextStyle(color = textColor, fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
    }
}

@Composable fun NormalForm(type: RecordType, existingRecord: RecordInfo?, isLoading: Boolean, onConfirm: (String, Long) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) { var content by remember { mutableStateOf(existingRecord?.details ?: "") }; var amountInput by remember { val initVal = if (existingRecord != null) abs(existingRecord.amount).toVndCurrency(showSymbol = false) else ""; mutableStateOf(TextFieldValue(initVal)) }; val color = if (type == RecordType.EXPENSE) Color(0xFFE53935) else Color(0xFF43A047); Column { OutlinedTextField(value = amountInput, onValueChange = { amountInput = formatCurrencyInput(it) }, label = { Text(stringResource(R.string.amount_title)) }, textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("đ") }); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(stringResource(R.string.details_title)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)); Spacer(modifier = Modifier.height(32.dp)); ActionButtons(isLoading = isLoading, isEditing = existingRecord != null, onCancel = onCancel, onDelete = onDelete, onConfirm = { val rawAmount = amountInput.text.replace(",", "").toLongOrNull() ?: 0L; if (!content.isBlank() && rawAmount > 0) { onConfirm(content, rawAmount) } }, confirmColor = color) } }
@Composable fun ActionButtons(isLoading: Boolean, isEditing: Boolean, onCancel: () -> Unit, onDelete: () -> Unit, onConfirm: () -> Unit, confirmColor: Color) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { if (isEditing) { TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Xóa") }; Spacer(modifier = Modifier.weight(1f)) }; TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) { Text(stringResource(R.string.cancel)) }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = onConfirm, enabled = !isLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.White)) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text(if (isEditing) "Cập nhật" else stringResource(R.string.confirm)) } } }
@Composable fun SmallInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) { OutlinedTextField(value = value, onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) }, label = { Text(label, fontSize = 11.sp) }, modifier = modifier, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyMedium) }
@Composable fun TypeButton(text: String, isSelected: Boolean, activeColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) { Button(onClick = onClick, modifier = modifier.padding(2.dp).fillMaxHeight(), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color.White else Color.Transparent, contentColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant), elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null, shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(0.dp)) { Text(text = text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) } }