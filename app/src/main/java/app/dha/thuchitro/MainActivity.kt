package app.dha.thuchitro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.dha.thuchitro.screens.AddRecordScreen
import app.dha.thuchitro.screens.HomeScreen
import app.dha.thuchitro.screens.ProfileScreen
import app.dha.thuchitro.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    val repo = DatabaseRepository(FirebaseFirestore.getInstance())
    val viewModel = RecordsViewModel(repo)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MyApp(viewModel)
            }
        }
    }
}

@Composable
fun MyApp(viewModel: RecordsViewModel) {
    viewModel.loadUserId()
    val context = LocalContext.current

    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.AddRecord,
        Screen.Profile
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = navController
                    .currentBackStackEntryAsState().value?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Giữ state khi chuyển tab
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (screen.isButton) {
                                IconButton(
                                    onClick = { navController.navigate(screen.route) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = context.getString(screen.titleRes),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Icon(screen.icon, contentDescription = context.getString(screen.titleRes))
                            }
                        },
                        label = { Text(context.getString(screen.titleRes)) },
                        alwaysShowLabel = !screen.isButton
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
            composable(Screen.AddRecord.route) { AddRecordScreen(viewModel, onDismiss = { navController.popBackStack() }) }
        }
    }
}

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    val isButton: Boolean = false
) {
    object Home : Screen("home", R.string.home, Icons.Default.Home)
    object AddRecord : Screen("add_record", R.string.add, Icons.Default.Add, true)
    object Profile : Screen("profile", R.string.profile, Icons.Default.Person)
}
