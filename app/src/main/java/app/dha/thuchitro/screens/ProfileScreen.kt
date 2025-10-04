package app.dha.thuchitro.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.dha.thuchitro.AppCache
import app.dha.thuchitro.R
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
    val uid by viewModel.uid.observeAsState(context.getString(R.string.unknown))
    val signedIn by viewModel.signedIn.observeAsState(false)

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            clipboardManager.setText(
                                AnnotatedString(
                                    AppCache.userId
                                )
                            )
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.copied_id), Toast.LENGTH_SHORT
                                )
                                .show()
                        },
                    )
                }
                .padding(10.dp),
            text = stringResource(R.string.your_id_label, uid)
        )

        if (signedIn) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp),
                onClick = {
                    viewModel.signOut()
                    googleSignInClient.signOut().addOnCompleteListener {
                        Toast.makeText(
                            context,
                            context.getString(R.string.not_signed_in),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                Text(stringResource(R.string.sign_out))
            }
        } else {
            GoogleSignInButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                viewModel.auth, googleSignInClient,
                { user ->
                    viewModel.loadUserId()
                    Toast.makeText(context,
                        context.getString(R.string.sign_in_success), Toast.LENGTH_SHORT).show()
                },
                { exception ->
//                    Log.e("TAG", "ProfileScreen: ${exception.message}")
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_signin_google) + "${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
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
                            onSignInFail(taskAuth.exception ?: Exception(context.getString(R.string.unknown)))
                        }
                    }
            } catch (e: Exception) {
                onSignInFail(e)
            }
        }
    }

    // UI Nút login
    Button(
        modifier = modifier,
        onClick = {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }) {
        Text(stringResource(R.string.sign_in_with_google))
    }
}
