package app.dha.thuchitro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
// SỬA LỖI IMPORT: Dùng RecordDialog thay vì AddRecordDialog
import app.dha.thuchitro.screens.RecordDialog
import app.dha.thuchitro.screens.HomeScreen
import app.dha.thuchitro.screens.ProfileScreen
// IMPORT THEME CỦA BẠN
import app.dha.thuchitro.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FirebaseFirestore.getInstance()
        val repo = DatabaseRepository(db)
        val factory = RecordsViewModelFactory(repo)
        val viewModel = ViewModelProvider(this, factory)[RecordsViewModel::class.java]

        setContent {
            // SỬA LỖI THEME: Dùng MyApplicationTheme
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: RecordsViewModel) {
    val navController = rememberNavController()
    // State quản lý việc hiển thị Dialog thêm mới
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Trang chủ") },
                    selected = currentRoute == "home", // Đã fix lỗi selected
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Cá nhân") },
                    selected = currentRoute == "profile", // Đã fix lỗi selected
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo("home")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                // Khi bấm nút Add ở HomeScreen -> set showAddDialog = true
                HomeScreen(
                    viewModel = viewModel,
                    onAddClick = { showAddDialog = true }
                )
            }
            composable("profile") {
                ProfileScreen(viewModel = viewModel)
            }
        }

        // --- DIALOG THÊM MỚI ---
        if (showAddDialog) {
            // SỬA TẠI ĐÂY: Đổi AddRecordDialog -> RecordDialog
            RecordDialog(
                viewModel = viewModel,
                existingRecord = null, // Truyền null nghĩa là đang THÊM MỚI
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

class RecordsViewModelFactory(private val repository: DatabaseRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}