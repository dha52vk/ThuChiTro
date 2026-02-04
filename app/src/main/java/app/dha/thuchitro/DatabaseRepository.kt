package app.dha.thuchitro

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class DatabaseRepository(private val db: FirebaseFirestore) {

    private val recordsCollection = db.collection("records")
    private val usersCollection = db.collection("users")

    // --- RECORDS (THU CHI) ---

    fun getQuery(): Query {
        return recordsCollection
    }

    fun addRecord(record: Map<String, Any?>): Task<DocumentReference> {
        return recordsCollection.add(record)
    }

    fun updateRecord(recordId: String, updates: Map<String, Any>): Task<Void> {
        return recordsCollection.document(recordId).update(updates)
    }

    fun deleteRecord(recordId: String): Task<Void> {
        return recordsCollection.document(recordId).delete()
    }

    // --- USERS (THÀNH VIÊN) ---

    // Chỉ lấy danh sách để hiển thị, không tự động lưu/ghi đè
    fun getUsers(): Task<QuerySnapshot> {
        return usersCollection.get()
    }
}