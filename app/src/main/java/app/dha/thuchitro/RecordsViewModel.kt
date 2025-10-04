package app.dha.thuchitro

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDateTime

class RecordsViewModel(private val repo: DatabaseRepository) : ViewModel() {
    val TAG = "ViewModel"
    val auth = FirebaseAuth.getInstance()

    private val _records = MutableLiveData<List<RecordInfo>>()
    val records: LiveData<List<RecordInfo>> get() = _records

    private val _uid = MutableLiveData<String?>()
    val uid: LiveData<String> get() = _uid.map { if (it.isNullOrEmpty()) "Chưa đăng nhập" else it}
    val signedIn: LiveData<Boolean> get() = _uid.map { !it.isNullOrEmpty() }
    private val _users = MutableLiveData<Map<String, String>>()
    val users: LiveData<Map<String, String>> get() = _users

    fun loadUserId() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            _uid.postValue(uid)
            AppCache.setUserId(uid)
            loadRecords()
        }else{
            _uid.postValue("")
        }
    }

    fun signOut(){
        auth.signOut()
        _uid.postValue("Chưa đăng nhập")
        AppCache.setUserId("")
    }

    fun addRecord(content: String, amount: Long, onAdded: () -> Unit = {}, onFailed: (Exception) -> Unit = {}){
        val current = LocalDateTime.now()
        repo.addRecord(current.month.value, current.year, content, amount, onAdded, onFailed)
    }

    fun editRecord(recordId: String, record: RecordInfo, onEdited: () -> Unit = {}, onFailed: (Exception) -> Unit = {}){
        repo.editRecord(recordId, record, onEdited,onFailed)
    }

    fun removeRecord(record: RecordInfo, onRemove: () -> Unit = {}, onFailed: (Exception) -> Unit = {}){
        repo.removeRecord(record, onRemove, onFailed)
    }

    fun removeRecord(month: Int, year: Int, recordId: String, onRemove: () -> Unit = {}, onFailed: (Exception) -> Unit = {}){
        repo.removeRecord(month, year, recordId, onRemove, onFailed)
    }

    private var registration: ListenerRegistration? = null

    fun loadRecords(userId: String? = null, month: Int?=null, year: Int?=null, onFailed: (Exception) -> Unit = {}) {
        registration?.remove()

        val current = LocalDateTime.now()
        val query = repo.getAllRecords(userId, month ?: current.month.value, year ?: current.year)
        repo.getAllUsers().addSnapshotListener { snapshot, e ->
            if (e!= null){
                onFailed(e)
                Log.e(TAG, "loadRecords: ${e.message}", )
                return@addSnapshotListener
            }
            val uids = emptyMap<String, String>().toMutableMap()
            for (doc in snapshot!!.documents){
                val name = doc.getString("Ten")
                if (name != null)
                    uids[doc.id] = name
            }
            _users.postValue(uids)

            _records.postValue(emptyList())
            registration = query
                .addSnapshotListener { snapshot, e ->
                    if (e != null){
                        Log.e(TAG, "getAllRecords: " + e.message)
                        return@addSnapshotListener
                    }
                    _records.postValue(snapshot!!.documents.mapNotNull { doc ->
                        var record: RecordInfo? = null
                        try {
                            val dateCreated = doc.getTimestamp("NgayTao")!!.toDate()
                            val content = doc.getString("NoiDung")!!
                            val amount = doc.getLong("SoTien")!!
                            val userId = doc.getString("UserId")!!
                            record = RecordInfo(doc.id, dateCreated, content, amount, _users.value!![userId]!!, userId)
                        }catch(e: Exception){
                            record = null
                        }
                        record
                    })
                    Log.i(TAG, "loadRecords: Loaded ${_records.value?.size ?: 0} records", Exception("Call stack"))
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        registration?.remove()
    }
}