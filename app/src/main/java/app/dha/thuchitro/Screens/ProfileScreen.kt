package app.dha.thuchitro.Screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.dha.thuchitro.AppCache
import app.dha.thuchitro.RecordsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun ProfileScreen(viewModel: RecordsViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val uid = viewModel.uid.observeAsState()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("834655526443-43271ni4ikluek3fcmfft24uoaktdvua.apps.googleusercontent.com") // Lấy từ google-services.json
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context as Activity, gso)
    viewModel.loadUserId()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(scrollState, Orientation.Vertical)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    true,
                    onClick = {},
                    onLongClick = {
                        clipboardManager.setText(
                            AnnotatedString(
                                AppCache.userId
                            )
                        )
                    })
                .padding(10.dp),
            text = "Id của bạn: ${uid.value}"
        )

        GoogleSignInButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            viewModel.auth, googleSignInClient,
            { user ->
                viewModel.loadUserId()
                Toast.makeText(context, "Sign in success", Toast.LENGTH_SHORT).show()
            },
            { exception ->
                Log.e("TAG", "ProfileScreen: ${exception.message}", )
                Toast.makeText(context, "Lỗi đăng nhập google: ${exception.message}", Toast.LENGTH_LONG).show()
            })
    }
}

@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSignInSuccess: (FirebaseUser) -> Unit,
    onSignInFail: (Exception) -> Unit
) {
    val context = LocalContext.current as Activity

    // Launcher để startActivityForResult trong Compose
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(context) { taskAuth ->
                        if (taskAuth.isSuccessful) {
                            taskAuth.result?.user?.let { onSignInSuccess(it) }
                        } else {
                            onSignInFail(taskAuth.exception ?: Exception("Unknown error"))
                        }
                    }
            } catch (e: Exception) {
                onSignInFail(e)
            }
        }
    }

    // UI Nút login
    Button(modifier = modifier,
        onClick = {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }) {
        Text("Sign in with Google")
    }
}

fun showInputDialog(context: Context, hint: String = "", onResult: (String) -> Unit) {
    val editText = EditText(context)
    editText.hint = "$hint..."

    AlertDialog.Builder(context)
        .setTitle(hint)
        .setView(editText)
        .setPositiveButton("OK") { dialog, _ ->
            val input = editText.text.toString()
            onResult(input)
            dialog.dismiss()
        }
        .setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
