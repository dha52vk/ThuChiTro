package app.dha.thuchitro

import java.util.Date

data class RecordInfo(
    val id: String,
    val dateCreated: Date,
    val content: String,
    val amount: Long,
    val userName: String,
    val userId: String,
)