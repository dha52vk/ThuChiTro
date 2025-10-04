package app.dha.thuchitro

import java.util.Date

data class RecordInfo(
    val id: String,
    val dateCreated: Date,
    var details: String,
    var amount: Long,
    val userName: String,
    val userId: String,
) {
    override fun toString(): String {
        return "RecordInfo(id='$id', dateCreated=$dateCreated, content='$details', amount=$amount, userName='$userName', userId='$userId')"
    }
}