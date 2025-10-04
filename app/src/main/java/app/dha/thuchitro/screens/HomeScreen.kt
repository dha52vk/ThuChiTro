package app.dha.thuchitro.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.dha.thuchitro.R
import app.dha.thuchitro.RecordInfo
import app.dha.thuchitro.RecordsViewModel
import app.dha.thuchitro.utils.format
import app.dha.thuchitro.utils.toVndCurrency
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(viewModel: RecordsViewModel) {
    val context = LocalContext.current

    val records by viewModel.records.observeAsState(emptyList())
    val users by viewModel.users.observeAsState(emptyMap())
    val signedIn by viewModel.signedIn.observeAsState(false)
    val userIds by remember(users) { mutableStateOf(users.keys.toList()) }
    var searchText by remember { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<RecordInfo?>(null) }
    var userIdFilter by remember { mutableStateOf<String?>(null) }
    var dateFilter by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val scrollRecordListState = rememberScrollState()
    val scrollFilterRowState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        if (signedIn) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(text = stringResource(R.string.search_placeholder)) })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .horizontalScroll(scrollFilterRowState)
                    ) {
                        SelectDateButton(
                            onDateSelected = { pair ->
                                dateFilter = pair
                                viewModel.loadRecords(
                                    userId = userIdFilter,
                                    month = dateFilter!!.first,
                                    year = dateFilter!!.second
                                )
                                Toast.makeText(
                                    context,
                                    "Selected ${dateFilter!!.first}/${dateFilter!!.second}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDateReset = {
                                viewModel.loadRecords(userIdFilter)
                            }
                        )
                        FilterDropdownMenu(
                            label = stringResource(R.string.all_user),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User"
                                )
                            },
                            items = userIds,
                            displayItems = users,
                            onItemChanged = { index, id ->
                                userIdFilter = if (index == 0) null else id
                                viewModel.loadRecords(
                                    userId = userIdFilter,
                                    month = dateFilter?.first,
                                    year = dateFilter?.second
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                MonthTotalButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.calc_total_transactions),
                    records = records,
                    users = users,
                    userIdFilter = userIdFilter,
                    dateFilter = dateFilter ?: Pair(
                        LocalDateTime.now().monthValue,
                        LocalDateTime.now().year
                    )
                )
            }

            if (records.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(10.dp)
                        .scrollable(scrollRecordListState, Orientation.Vertical)
                ) {
                    for (record in records) {
                        if (record.toString().contains(searchText, ignoreCase = true)) {
                            RecordEntry(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRecord = record }
                                    .padding(8.dp),
                                record
                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
            else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.no_record_title))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.not_signed_in))
            }
        }
    }
    selectedRecord?.let { record ->
        var editDialog by remember { mutableStateOf(false) }
        var removeDialog by remember { mutableStateOf(false) }
        EntryDialog(
            onDismissRequest = { selectedRecord = null },
            onEditClick = {
                editDialog = true
//                    selectedRecord = null
            },
            onRemoveClick = {
                removeDialog = true
//                    selectedRecord = null
            }
        )
        if (editDialog) {
            EditRecordDialog(
                record,
                onDismissRequest = {
                    editDialog = false
                },
                onEditClick = { content, amount ->
                    editDialog = false
                    record.details = content
                    record.amount = amount
                    viewModel.editRecord(
                        record.id, record,
                        onEditted = {
                            viewModel.loadRecords(
                                userIdFilter,
                                dateFilter?.first,
                                dateFilter?.second
                            )
                            Toast.makeText(context,
                                context.getString(R.string.edited), Toast.LENGTH_SHORT).show()
                        })
                    selectedRecord = null
                }
            )
        }
        if (removeDialog) {
            RemoveRecordDialog(
                onDismissRequest = {
                    removeDialog = false
                },
                onRemoveClick = {
                    removeDialog = false
                    viewModel.removeRecord(record, onRemove = {
                        viewModel.loadRecords(
                            userIdFilter,
                            dateFilter?.first,
                            dateFilter?.second
                        )
                        Toast.makeText(context,
                            context.getString(R.string.removed), Toast.LENGTH_SHORT).show()
                    })
                    selectedRecord = null
                }
            )
        }
    }

}

@Composable
fun MonthTotalButton(
    modifier: Modifier = Modifier,
    label: String,
    records: List<RecordInfo>,
    users: Map<String, String>,
    userIdFilter: String? = null,
    dateFilter: Pair<Int, Int>
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Button(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        onClick = { showDialog = true },
    ) {
        Text(text = label)
    }

    if (showDialog) {
        val userTotals = remember(records) {
            records.groupBy { it.userId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        }
        val average = remember(userTotals) {
            if (userTotals.isNotEmpty()) userTotals.values.average().toLong() else 0L
        }

        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.total_transactions_month_title) + "${dateFilter.first}/${dateFilter.second}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (userTotals.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.name),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = stringResource(R.string.total),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                modifier = Modifier.padding(start = 20.dp),
                                text = stringResource(R.string.receive),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        if (userIdFilter == null) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentHeight()
                            ) {
                                userTotals.keys.toList().forEach { userId ->
                                    val total = userTotals[userId] ?: 0
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = users[userId] ?: stringResource(R.string.unknown),
                                        )
                                        Text(
                                            text = total.toVndCurrency(),
                                            color = if (total <= 0) Color.Red else Color.Green,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        val receiveMoney = average.minus(total)
                                        Text(
                                            modifier = Modifier.padding(start = 10.dp),
                                            text = (receiveMoney).toVndCurrency(true),
                                            color = if (receiveMoney <= 0) Color.Red else Color.Green,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else {
                            UserTotalRow(
                                users[userIdFilter],
                                userTotals[userIdFilter] ?: 0,
                                average
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        UserTotalRow(
                            stringResource(R.string.average),
                            average
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    } else {
                        Text(stringResource(R.string.no_transactions_title))
                    }

                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@Composable
fun UserTotalRow(userName: String?, total: Long, average: Long? = null) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = userName ?: context.getString(R.string.unknown),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = total.toVndCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (total <= 0) Color.Red else Color.Green
        )
        if (average != null) {
            val receiveMoney = average.minus(total)
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = (receiveMoney).toVndCurrency(true),
                style = MaterialTheme.typography.bodyLarge,
                color = if (receiveMoney <= 0) Color.Red else Color.Green
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDateButton(
    modifier: Modifier = Modifier,
    onDateSelected: (Pair<Int, Int>) -> Unit = {},
    onDateReset: () -> Unit = {},
    label: String = "",
) {
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
    var selectedDate by remember {
        mutableStateOf(formatter.format(current))
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
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
                        Text(text = stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        onDateReset()
                    }) {
                        Text(text = stringResource(R.string.reset))
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
    onItemChanged: (Int, String) -> Unit = { i, s -> },
    items: List<String>? = null,
    displayItems: Map<String, String>? = null
) {

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(label) }

    val items = (items ?: emptyList()).toMutableList()
        .apply { add(0, label) }

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
            Box(
                modifier
                    .wrapContentSize()
                    .padding(end = 5.dp)
            ) {
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
                        selectedText =
                            if (index == 0) label else if (displayItems != null) displayItems[item]
                                ?: "" else item
                        onItemChanged(index, if (index == 0) "" else item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RecordEntry(modifier: Modifier = Modifier, record: RecordInfo) {
    Box(
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier
                    .width(40.dp)
                    .padding(end = 10.dp),
                painter = painterResource(if (record.amount > 0) R.drawable.money_pay else R.drawable.shopping_cart),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "EntryIcon"
            )
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(
                    text = record.userName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = record.details,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = record.dateCreated.format("hh:mm - dd/MM/yyyy"),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            Text(
                text = record.amount.toVndCurrency(true),
                color = if (record.amount > 0) Color(
                    66,
                    179,
                    121
                ) else MaterialTheme.typography.bodyLarge.color,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EditRecordDialog(
    record: RecordInfo? = null,
    onDismissRequest: () -> Unit = {},
    onEditClick: (String, Long) -> Unit = { c, a -> }
) {
    val context = LocalContext.current
    var noiDung by remember { mutableStateOf(record?.details ?: "") }
    var soTien by remember { mutableStateOf(record?.amount?.toString() ?: "") }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = noiDung,
                    onValueChange = { noiDung = it },
                    label = { Text(stringResource(R.string.details_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                TextField(
                    value = soTien,
                    onValueChange = { soTien = it },
                    label = { Text(stringResource(R.string.amount_title)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                val options = listOf(stringResource(R.string.short_expense_title),
                    stringResource(R.string.short_income_title)
                )
                var selectedOption by remember {
                    mutableStateOf(
                        if ((record?.amount ?: 0) > 0) context.getString(R.string.short_income_title)
                        else context.getString(R.string.short_expense_title)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    options.forEach { text ->
                        TextButton(
                            onClick = { selectedOption = text },
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedOption == text) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = text,
                                color =
                                    if (selectedOption == text) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TextButton(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = {
                            try {
                                onEditClick(
                                    noiDung,
                                    if (selectedOption == context.getString(R.string.short_income_title)) abs(soTien.toLong()) else -abs(
                                        soTien.toLong()
                                    )
                                )
                            } catch (_: Exception) {
                                Toast.makeText(context,
                                    context.getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                        Text(
                            stringResource(R.string.confirm),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveRecordDialog(
    onDismissRequest: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.confirm_remove_record_title),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TextButton(onClick = onRemoveClick) {
                        Text(
                            stringResource(R.string.confirm),
                            color = Color.Red,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            stringResource(R.string.cancel),
                            color = Color.Green,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntryDialog(
    onDismissRequest: () -> Unit,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DialogButton(
                    text = stringResource(R.string.edit),
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                    onClick = onEditClick
                )
                DialogButton(
                    text = stringResource(R.string.remove),
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Remove") },
                    onClick = onRemoveClick
                )
            }
        }
    }
}

@Composable
fun RowScope.DialogButton(text: String, icon: @Composable () -> Unit = {}, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.padding(10.dp)) { icon() }
            Text(text)
        }
    }
}