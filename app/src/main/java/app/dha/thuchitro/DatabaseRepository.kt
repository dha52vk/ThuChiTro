package app.dha.thuchitro

import android.util.Log
import app.dha.thuchitro.utils.format
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class DatabaseRepository(private val db: FirebaseFirestore) {
    val TAG = "Firestore"
    fun getUser(userId: String): Query {
        return db.collection("users").whereEqualTo(FieldPath.documentId(), userId)
    }

    fun getAllUsers(): Query{
        return db.collection("users")
    }

    fun addRecord(month: Int, year: Int, content: String, amount: Long, onSuccess: () -> Unit = {}){
        val current = Timestamp.now().toDate()
        val recordId = current.format("yyyyMMdd_HHmmss")
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .set(hashMapOf(
                "NgayTao" to Timestamp.now(),
                "NoiDung" to content,
                "SoTien" to amount,
                "UserId" to AppCache.userId
            )).addOnSuccessListener {
                onSuccess()
                Log.d(TAG, "addRecord: ${content}")
            }.addOnFailureListener {
                Log.e(TAG, "addRecord: Failed to add ${content}", )
            }
    }

    fun editRecord(recordId: String, record: RecordInfo, onSuccess: () -> Unit = {}){
        val cal = Calendar.getInstance().apply { time = record.dateCreated }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .set(hashMapOf(
                "NoiDung" to record.details,
                "SoTien" to record.amount,
            ), SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
                Log.d(TAG, "editRecord: ${record.details}")
            }.addOnFailureListener {
                Log.e(TAG, "editRecord: Failed to edit ${record.details}", )
            }
    }

    fun removeRecord(record: RecordInfo, onSuccess: () -> Unit = {}){
        val cal = Calendar.getInstance().apply { time = record.dateCreated }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        removeRecord(month, year, record.id, onSuccess)
    }

    fun removeRecord(month: Int, year: Int, recordId: String, onSuccess: () -> Unit = {}){
        db.collection("records")
            .document("Y$year")
            .collection("M$month")
            .document(recordId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
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
        return query.orderBy("NgayTao", Query.Direction.DESCENDING)
    }
}