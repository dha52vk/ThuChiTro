package app.dha.thuchitro.Screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.dha.thuchitro.DatabaseRepository
import app.dha.thuchitro.RecordInfo
import app.dha.thuchitro.RecordsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(viewModel: RecordsViewModel) {
    val records by viewModel.records.observeAsState(emptyList<RecordInfo>())
    val users by viewModel.users.observeAsState(emptyMap())
    val userIds by remember(users) { mutableStateOf(users.keys.toList()) }
    var searchText by remember { mutableStateOf("") }

    Log.e("TAG", "HomeScreen: ${records.size}")
    //TODO remove condition
    if (records.isNotEmpty() || true) {
        val scrollRecordListState = rememberScrollState()
        val scrollFilterRowState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(text = "Tìm kiếm...") })
            Box(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))) {
                Row(
                    modifier = Modifier
                        .padding(5.dp)
                        .horizontalScroll(scrollFilterRowState)
                ) {
                    FilterDropdownMenu(
                        label = "User",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User"
                            )
                        },
                        items = userIds,
                        displayItems = users,
                        onItemChanged = {index, id ->
                            viewModel.loadRecords(userId = if (index == 0) null else id)
                        }
                    )
                    SelectDateButton(
                        label = "Select date",
                        onDateSelected = { pair ->
                            viewModel.loadRecords(month = pair.first, year = pair.second)
                        },
                        onDateReset = {
                            viewModel.loadRecords()
                        }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp)
                    .scrollable(scrollRecordListState, Orientation.Vertical)
            ) {
                for (record in records) {
                    RecordEntry(record)
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Không có bản ghi nào")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDateButton(modifier: Modifier = Modifier,
                     onDateSelected: (Pair<Int,Int>) -> Unit = {},
                     onDateReset: () -> Unit = {},
                     label: String = "",){
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
    var selectedDate by remember {
        mutableStateOf(formatter.format(current))
    }
    Box(modifier = modifier,
        contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clickable(true, onClick = { showDatePicker = true })
                .padding(5.dp)
                .border(
                    1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = label,
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(text = selectedDate)
        }


        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = {
                    showDatePicker = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            val date = Date(it)
                            val simpleDateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                            selectedDate = simpleDateFormat.format(date)
                            onDateSelected(Pair(date.month + 1, date.year + 1900))
                        }
                    }) {
                        Text(text = "Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        onDateReset()
                    }) {
                        Text(text = "Reset")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownMenu(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    label: String = "",
    onItemChanged: (Int,String) -> Unit = {i,s->},
    items: List<String>? = null,
    displayItems: Map<String, String>? = null
) {

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(label) }

    val items = (items ?: listOf("Đỗ Hồng Anh", "Item 2", "Item 3")).toMutableList().apply { add(0, "None") }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .clickable(true, onClick = { expanded = !expanded })
                .padding(5.dp)
                .border(
                    1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Box(modifier
                .wrapContentSize()
                .padding(end = 5.dp)) {
                icon()
            }
            Text(
                text = selectedText,
                maxLines = 1,
                modifier = Modifier.wrapContentWidth()
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(displayItems?.get(item) ?: item) },
                    onClick = {
                        selectedText = if (index == 0) label else item
                        onItemChanged(index,if (index == 0) "" else selectedText)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RecordEntry(record: RecordInfo) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    Text(text = "${formatter.format(record.dateCreated)}: ${record.content} - ${record.amount} (${record.userName})")
}

@Composable
fun EntryDialog() {

}