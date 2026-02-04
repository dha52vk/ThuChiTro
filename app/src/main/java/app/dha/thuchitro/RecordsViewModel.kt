package app.dha.thuchitro

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

// Lưu ý: RecordInfo đã được định nghĩa ở file riêng (RecordInfo.kt), không khai báo lại ở đây.

// Data class này chỉ dùng nội bộ để truyền list từ UI vào ViewModel
data class RentSplitItem(
    val userId: String,
    val rawAmountString: String
)

class RecordsViewModel(private val repo: DatabaseRepository) : ViewModel() {

    val auth = FirebaseAuth.getInstance()

    // --- LIVE DATA ---
    private val _records = MutableLiveData<List<RecordInfo>>()
    val records: LiveData<List<RecordInfo>> = _records

    private val _signedIn = MutableLiveData<Boolean>()
    val signedIn: LiveData<Boolean> = _signedIn

    private val _uid = MutableLiveData<String>()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _users = MutableLiveData<Map<String, String>>()
    val users: LiveData<Map<String, String>> = _users

    // --- VARIABLES ---
    private var recordListener: ListenerRegistration? = null
    private var currentFilterMonth: Int? = null
    private var currentFilterYear: Int? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            updateUserInfo(user)
        }
        loadUserId()
        loadUsers()
        loadRecords(month = null, year = null)
    }

    // --- AUTH & USER ---
    fun loadUserId() {
        val user = auth.currentUser
        updateUserInfo(user)
    }

    private fun updateUserInfo(user: com.google.firebase.auth.FirebaseUser?) {
        _signedIn.value = user != null
        if (user != null) {
            _uid.value = user.uid
            _userName.value = user.displayName ?: "Người dùng"
            _userEmail.value = user.email ?: ""
            loadUsers()
            reloadRecords()
        } else {
            _uid.value = ""
            _userName.value = ""
            _userEmail.value = ""
            _records.value = emptyList()
            _users.value = emptyMap()
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onFailed: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailed(it) }
    }

    fun signOut() { auth.signOut() }

    private fun loadUsers() {
        repo.getUsers()
            .addOnSuccessListener { result ->
                val map = _users.value?.toMutableMap() ?: mutableMapOf()
                for (doc in result) {
                    val name = doc.getString("userName") ?: doc.getString("displayName") ?: "Thành viên"
                    map[doc.id] = name
                }
                val currentUser = auth.currentUser
                if (currentUser != null && !map.containsKey(currentUser.uid)) {
                    map[currentUser.uid] = currentUser.displayName ?: "Tôi"
                }
                _users.value = map
            }
            .addOnFailureListener { Log.e("ViewModel", "Lỗi load users", it) }
    }

    // --- LOAD RECORDS ---
    fun loadRecords(userId: String? = null, month: Int? = null, year: Int? = null) {
        this.currentFilterMonth = month
        this.currentFilterYear = year

        recordListener?.remove()
        var query: Query = repo.getQuery()

        if (userId != null) query = query.whereEqualTo("userId", userId)

        if (month == 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -6)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            query = query.whereGreaterThanOrEqualTo("dateCreated", Timestamp(cal.time)).orderBy("dateCreated", Query.Direction.DESCENDING)
        } else {
            val targetMonth = month ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            val targetYear = year ?: Calendar.getInstance().get(Calendar.YEAR)
            val startCal = Calendar.getInstance().apply { set(targetYear, targetMonth - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            val endCal = Calendar.getInstance().apply { time = startCal.time; add(Calendar.MONTH, 1) }
            query = query.whereGreaterThanOrEqualTo("dateCreated", Timestamp(startCal.time)).whereLessThan("dateCreated", Timestamp(endCal.time)).orderBy("dateCreated", Query.Direction.DESCENDING)
        }

        recordListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timestamp = doc.getTimestamp("dateCreated")
                        val date = timestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                        // Fallback logic cho isRent
                        val isRentVal = doc.getBoolean("isRent") ?: (doc.getLong("elecIndex") != null)

                        RecordInfo(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            amount = doc.getLong("amount") ?: 0,
                            details = doc.getString("details") ?: "",
                            dateCreated = date ?: LocalDateTime.now(),
                            isRent = isRentVal,
                            elecIndex = doc.getLong("elecIndex"),
                            waterIndex = doc.getLong("waterIndex"),
                            roomPrice = doc.getLong("roomPrice"),
                            serviceFee = doc.getLong("serviceFee"),
                            elecPrice = doc.getLong("elecPrice"),
                            waterPrice = doc.getLong("waterPrice")
                        )
                    } catch (_: Exception) { null }
                }
                _records.postValue(list)

                val currentUsersMap = _users.value?.toMutableMap() ?: mutableMapOf()
                var hasNewUser = false
                list.forEach { record ->
                    if (!currentUsersMap.containsKey(record.userId) && record.userName.isNotBlank()) {
                        currentUsersMap[record.userId] = record.userName
                        hasNewUser = true
                    }
                }
                if (hasNewUser) _users.postValue(currentUsersMap)
            }
        }
    }

    fun reloadRecords() { loadRecords(month = currentFilterMonth, year = currentFilterYear) }

    // --- ADD / UPDATE / DELETE ---

    fun addRecord(content: String, amount: Long, userId: String? = null, userName: String? = null, onAdded: () -> Unit, onFailed: (Exception) -> Unit) {
        val currentUser = auth.currentUser ?: return onFailed(Exception("Chưa đăng nhập"))
        val uid = userId ?: currentUser.uid
        val uname = userName ?: (currentUser.displayName ?: "Ẩn danh")

        val record = hashMapOf(
            "userId" to uid,
            "userName" to uname,
            "amount" to amount,
            "details" to content,
            "dateCreated" to Timestamp.now(),
            "isRent" to false
        )
        repo.addRecord(record).addOnSuccessListener { reloadRecords(); onAdded() }.addOnFailureListener { onFailed(it) }
    }

    fun addRentBatch(
        content: String,
        customDate: Date? = null,
        elecIndex: Long? = null, waterIndex: Long? = null,
        roomPrice: Long? = null, serviceFee: Long? = null,
        elecPrice: Long? = null, waterPrice: Long? = null,
        splitList: List<RentSplitItem>,
        onFinished: () -> Unit,
        onFailed: (Exception) -> Unit
    ) {
        val timestamp = if (customDate != null) Timestamp(customDate) else Timestamp.now()
        val userMap = _users.value ?: emptyMap()

        val validItems = splitList.filter { it.userId.isNotBlank() }
        val totalCount = validItems.size
        var successCount = 0

        if (totalCount == 0) {
            onFinished()
            return
        }

        validItems.forEachIndexed { index, item ->
            val rawText = item.rawAmountString.replace(",", "").trim()
            val finalAmount = rawText.toLongOrNull() ?: 0L

            if (finalAmount != 0L) {
                val uName = userMap[item.userId] ?: "Thành viên"
                val isMain = (index == 0)

                val recordMap = hashMapOf(
                    "userId" to item.userId,
                    "userName" to uName,
                    "amount" to finalAmount,
                    "details" to content,
                    "dateCreated" to timestamp,
                    "isRent" to true, // Đánh dấu là Tiền Trọ cho tất cả record sinh ra
                    "elecIndex" to if (isMain) elecIndex else null,
                    "waterIndex" to if (isMain) waterIndex else null,
                    "roomPrice" to if (isMain) roomPrice else null,
                    "serviceFee" to if (isMain) serviceFee else null,
                    "elecPrice" to if (isMain) elecPrice else null,
                    "waterPrice" to if (isMain) waterPrice else null
                )

                repo.addRecord(recordMap)
                    .addOnSuccessListener {
                        successCount++
                        if (successCount == totalCount) {
                            reloadRecords()
                            onFinished()
                            loadUsers()
                        }
                    }
                    .addOnFailureListener { onFailed(it) }
            } else {
                successCount++
                if (successCount == totalCount) onFinished()
            }
        }
    }

    fun updateRecord(recordId: String, content: String, amount: Long, elecIndex: Long? = null, waterIndex: Long? = null, onSuccess: () -> Unit, onFailed: (Exception) -> Unit) {
        val updates = hashMapOf<String, Any?>("details" to content, "amount" to amount, "elecIndex" to elecIndex, "waterIndex" to waterIndex)
        @Suppress("UNCHECKED_CAST")
        val cleanUpdates = updates.filterValues { it != null } as Map<String, Any>
        repo.updateRecord(recordId, cleanUpdates).addOnSuccessListener { reloadRecords(); onSuccess() }.addOnFailureListener { onFailed(it) }
    }

    // Xóa thông minh: Nếu là rent -> Xóa cả tháng
    fun deleteRecord(targetRecord: RecordInfo, onSuccess: () -> Unit, onFailed: (Exception) -> Unit) {
        if (targetRecord.isRent) {
            val targetMonth = targetRecord.dateCreated.monthValue
            val targetYear = targetRecord.dateCreated.year

            // Tìm các record cùng đợt (cùng tháng, cùng năm, là tiền trọ)
            val recordsToDelete = _records.value?.filter {
                it.isRent &&
                        it.dateCreated.monthValue == targetMonth &&
                        it.dateCreated.year == targetYear
            } ?: listOf(targetRecord)

            val total = recordsToDelete.size
            var deletedCount = 0

            if (total == 0) { onSuccess(); return }

            recordsToDelete.forEach { record ->
                repo.deleteRecord(record.id)
                    .addOnSuccessListener {
                        deletedCount++
                        if (deletedCount == total) {
                            reloadRecords()
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { onFailed(it) }
            }
        } else {
            repo.deleteRecord(targetRecord.id)
                .addOnSuccessListener { reloadRecords(); onSuccess() }
                .addOnFailureListener { onFailed(it) }
        }
    }

    fun getLastRentInfo(): RecordInfo? { return _records.value?.firstOrNull { it.elecIndex != null } }
}