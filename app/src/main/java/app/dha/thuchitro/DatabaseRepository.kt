package app.dha.thuchitro

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.type.DateTime
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.collections.map

class DatabaseRepository(private val db: FirebaseFirestore) {
    val TAG = "Firestore"
    fun getUser(userId: String): Query {
        return db.collection("users").whereEqualTo(FieldPath.documentId(), userId)
    }

    fun getAllUsers(): Query{
        return db.collection("users")
    }

    fun addRecord(month: Int, year: Int, record: RecordInfo){
        val current = Timestamp.now().toDate()
        val formatter = SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault())
        val recordId = formatter.format(current)
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .set(hashMapOf(
                "NgayTao" to Timestamp.now(),
                "NoiDung" to record.content,
                "SoTien" to record.amount,
                "UserId" to AppCache.userId
            )).addOnSuccessListener {
                Log.d(TAG, "addRecord: ${record.content}")
            }.addOnFailureListener {
                Log.e(TAG, "addRecord: Failed to add ${record.content}", )
            }
    }

    fun editRecord(month: Int, year:Int, recordId: String, record: RecordInfo){
        if (AppCache.userId != record.userId)
            throw IllegalAccessError("It is not your record!")
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .set(hashMapOf(
                "NoiDung" to record.content,
                "SoTien" to record.amount,
            ), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "editRecord: ${record.content}")
            }.addOnFailureListener {
                Log.e(TAG, "editRecord: Failed to edit ${record.content}", )
            }
    }

    fun removeRecord(month: Int, year: Int, recordId: String){
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Document successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
            }
    }

    fun getAllRecords(userId: String? = null, month: Int, year: Int): Query{
        var query: Query = db.collection("records")
            .document("Y$year")
            .collection("M$month")

        if (userId != null) query = query.whereEqualTo("UserId", userId)
        query.orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
        return query
    }
}