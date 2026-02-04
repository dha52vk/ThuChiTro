package app.dha.thuchitro.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import app.dha.thuchitro.R
import app.dha.thuchitro.RecordsViewModel
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: RecordsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lấy state từ ViewModel
    val signedIn by viewModel.signedIn.observeAsState(false)
    val userName by viewModel.userName.observeAsState("Khách")
    val userEmail by viewModel.userEmail.observeAsState("")
    val userAvatar = viewModel.auth.currentUser?.photoUrl // URL ảnh đại diện từ Firebase User

    // 1. Khởi tạo Credential Manager
    val credentialManager = remember { CredentialManager.create(context) }

    // Hàm xử lý Đăng nhập với Credential Manager
    fun performGoogleSignIn() {
        // Lấy Web Client ID tự động từ file google-services.json
        // Giá trị này được sinh ra tự động, không cần copy paste thủ công
        val webClientId = context.getString(R.string.default_web_client_id)

        // Cấu hình tùy chọn lấy Google ID
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // false: Cho phép chọn tài khoản chưa từng đăng nhập vào app
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true) // Tự động đăng nhập nếu chỉ có 1 tài khoản hợp lệ
            .build()

        // Tạo request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                // Gọi hộp thoại đăng nhập của hệ thống
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential

                // Kiểm tra nếu credential trả về là Google ID Token
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Gửi ID Token sang ViewModel để Firebase xác thực
                    viewModel.signInWithGoogle(
                        idToken = idToken,
                        onSuccess = {
                            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        },
                        onFailed = { e ->
                            Toast.makeText(context, "Lỗi Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Log.e("Auth", "Loại credential không hỗ trợ: ${credential.type}")
                }
            } catch (e: GetCredentialException) {
                // Xử lý lỗi (người dùng hủy, mất mạng, sai cấu hình...)
                Log.e("Auth", "Lỗi Credential Manager: ${e.message}")

                // Chỉ hiện Toast nếu lỗi không phải do người dùng tự hủy bỏ
                if (!e.message.toString().contains("UserCanceled")) {
                    Toast.makeText(context, "Đăng nhập thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (signedIn) {
            // --- GIAO DIỆN KHI ĐÃ ĐĂNG NHẬP ---
            if (userAvatar != null) {
                AsyncImage(
                    model = userAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.signOut() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất")
            }
        } else {
            // --- GIAO DIỆN KHI CHƯA ĐĂNG NHẬP ---
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bạn chưa đăng nhập",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Đăng nhập để đồng bộ dữ liệu chi tiêu",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { performGoogleSignIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Đăng nhập bằng Google")
            }
        }
    }
}