package app.dha.thuchitro

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth

object AppCache {
    private var _userId: String? = null
    val userId: String
        get() = _userId ?: "UserId chưa được set"

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