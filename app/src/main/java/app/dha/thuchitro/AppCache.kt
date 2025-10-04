package app.dha.thuchitro

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth

object AppCache {
    private var _userId: String? = null
    val userId: String
        get() = if (_userId.isNullOrEmpty()) "UserId chưa được set" else _userId!!
    val signedIn: Boolean
        get() = !_userId.isNullOrEmpty()

    fun setUserId(id: String) {
        if (_userId == null) {
            _userId = id
        }
    }

    fun refresh(context: Context){
        val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    }

    fun save(context: Context, id: String? = null){
        val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    }
}