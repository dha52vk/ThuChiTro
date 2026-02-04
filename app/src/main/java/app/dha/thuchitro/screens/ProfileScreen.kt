package app.dha.thuchitro.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.dha.thuchitro.R
import app.dha.thuchitro.RecordsViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: RecordsViewModel) {
    val context = LocalContext.current

    // State từ ViewModel
    val signedIn by viewModel.signedIn.observeAsState(false)
    val userName by viewModel.userName.observeAsState("Khách")
    val userEmail by viewModel.userEmail.observeAsState("")
    val userAvatar = viewModel.auth.currentUser?.photoUrl

    // 1. Cấu hình Google Sign-In (Legacy)
    val webClientId = stringResource(R.string.default_web_client_id)

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // 2. Launcher hứng kết quả đăng nhập
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                if (idToken != null) {
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
                    Toast.makeText(context, "Không lấy được ID Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // Mã lỗi phổ biến: 12500 (Sai SHA1), 10 (Sai ClientID)
                Toast.makeText(context, "Đăng nhập thất bại (Mã lỗi: ${e.statusCode})", Toast.LENGTH_LONG).show()
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
            // --- GIAO DIỆN ĐÃ ĐĂNG NHẬP ---
            if (userAvatar != null) {
                AsyncImage(
                    model = userAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(100.dp).clip(CircleShape),
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
                onClick = {
                    // Đăng xuất khỏi Google Client trước (quan trọng để lần sau hiện bảng chọn tài khoản)
                    googleSignInClient.signOut().addOnCompleteListener {
                        // Sau đó đăng xuất khỏi Firebase
                        viewModel.signOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                // Nếu báo lỗi icon này, hãy đổi thành Icons.Default.ExitToApp
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất")
            }
        } else {
            // --- GIAO DIỆN CHƯA ĐĂNG NHẬP ---
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
                text = "Đăng nhập để đồng bộ dữ liệu",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Mở màn hình đăng nhập của Google
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Đăng nhập bằng Google")
            }
        }
    }
}