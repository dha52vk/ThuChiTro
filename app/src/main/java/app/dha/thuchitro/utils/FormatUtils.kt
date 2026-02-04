package app.dha.thuchitro.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- 1. EXTENSION FORMAT NGÀY GIỜ ---
fun LocalDateTime.format(pattern: String): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return this.format(formatter)
}

// --- 2. EXTENSION FORMAT TIỀN TỆ (HIỂN THỊ) ---
// Dùng để hiển thị lên Text (Ví dụ: 100,000đ)
fun Long.toVndCurrency(showSymbol: Boolean = true): String {
    val formatted = DecimalFormat("#,###").format(this).replace(".", ",")
    return if (showSymbol) "${formatted}đ" else formatted
}

// --- 3. HELPER FORMAT INPUT (NHẬP LIỆU) ---
// Dùng cho TextField: Tự động thêm dấu phẩy và giữ con trỏ ở cuối
fun formatCurrencyInput(input: TextFieldValue): TextFieldValue {
    // 1. Xóa dấu phẩy cũ để lấy số thô
    val rawText = input.text.replace(",", "")

    // 2. Kiểm tra tính hợp lệ (chỉ chấp nhận số)
    if (!rawText.all { it.isDigit() }) return input
    if (rawText.isEmpty()) return input

    // 3. Format lại
    val parsed = rawText.toLongOrNull() ?: 0L
    val formatted = DecimalFormat("#,###").format(parsed).replace(".", ",")

    // 4. Trả về giá trị mới với con trỏ nằm ở cuối cùng
    return TextFieldValue(
        text = formatted,
        selection = TextRange(formatted.length)
    )
}