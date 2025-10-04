package app.dha.thuchitro.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.dha.thuchitro.R
import app.dha.thuchitro.RecordsViewModel

@Composable
fun AddRecordScreen(viewModel: RecordsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val signedIn by viewModel.signedIn.observeAsState(false)
    if (signedIn) {
        EditRecordDialog(
            onDismissRequest = onDismiss,
            onEditClick = { content, amount ->
                viewModel.addRecord(
                    content,
                    amount,
                    onAdded = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.added),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailed = { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    })
                onDismiss()
            }
        )
    } else {
        onDismiss()
    }
}