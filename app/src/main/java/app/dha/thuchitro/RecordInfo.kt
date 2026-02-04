// Trong file RecordInfo.kt hoặc cùng file ViewModel
package app.dha.thuchitro

import java.time.LocalDateTime

data class RecordInfo(
    val id: String,
    val userId: String,
    val userName: String,
    val amount: Long,
    val details: String,
    val dateCreated: LocalDateTime,
    val isRent: Boolean = false, // <--- TRƯỜNG MỚI ĐỂ ĐỊNH DANH

    val elecIndex: Long? = null,
    val waterIndex: Long? = null,
    val roomPrice: Long? = null,
    val serviceFee: Long? = null,
    val elecPrice: Long? = null,
    val waterPrice: Long? = null
)