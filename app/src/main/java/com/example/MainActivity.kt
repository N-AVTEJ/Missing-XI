package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MissingXiTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.DeepBg
import com.example.ui.screens.*
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MissingXiTheme {
        MainNavigationContainer()
      }
    }
  }
}

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector, val activeColor: Color) {
  object Home : NavigationItem("home", "Home", Icons.Default.Home, NeonGreen)
  object Build : NavigationItem("build", "Build", Icons.Default.AddCircle, NeonBlue)
  object Teams : NavigationItem("teams", "Teams", Icons.Default.Groups, NeonGreen)
  object Setup : NavigationItem("setup", "Setup", Icons.Default.Group, IndigoAccent)
  object Toss : NavigationItem("toss", "Toss", Icons.Default.Casino, GoldStar)
  object History : NavigationItem("history", "History", Icons.Default.History, NeonGreen)
  object Settings : NavigationItem("settings", "Settings", Icons.Default.Settings, NeonBlue)
}

@Composable
fun MainNavigationContainer() {
  val navController = rememberNavController()
  val viewModel: AppViewModel = viewModel()
  
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: NavigationItem.Home.route

  Scaffold(
    modifier = Modifier
        .fillMaxSize()
        .background(DeepBg),
    bottomBar = {
      CustomBottomNavigationBar(
        navController = navController,
        currentRoute = currentRoute
      )
    },
    contentWindowInsets = WindowInsets.safeDrawing
  ) { paddingValues ->
    Box(
      modifier = Modifier
          .fillMaxSize()
          .background(DeepBg)
          .padding(bottom = 70.dp) // Leave exact space for custom bottom-nav float card
          .padding(paddingValues)
    ) {
      NavHost(
        navController = navController,
        startDestination = NavigationItem.Home.route,
        modifier = Modifier.fillMaxSize()
      ) {
        composable("home") {
          HomeScreen(
            viewModel = viewModel,
            onNavigateToBuild = { navController.navigate("build") },
            onNavigateToToss = { navController.navigate("toss") }
          )
        }
        composable("build") {
          BuildScreen(viewModel = viewModel)
        }
        composable("teams") {
          TeamConfigScreen(viewModel = viewModel)
        }
        composable("setup") {
          MatchSetupScreen(viewModel = viewModel)
        }
        composable("toss") {
          TossScreen(viewModel = viewModel)
        }
        composable("history") {
          HistoryScreen(viewModel = viewModel)
        }
        composable("settings") {
          SettingsScreen(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun CustomBottomNavigationBar(
  navController: NavController,
  currentRoute: String
) {
  val items = listOf(
    NavigationItem.Home,
    NavigationItem.Build,
    NavigationItem.Teams,
    NavigationItem.Setup,
    NavigationItem.Toss,
    NavigationItem.History,
    NavigationItem.Settings
  )

  Box(
    modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Elegant Premium Translucent Floating Card
    Card(
      modifier = Modifier
          .fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0x99121824))
    ) {
      Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        items.forEach { item ->
          val isSelected = currentRoute == item.route
          
          Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                  if (currentRoute != item.route) {
                    navController.navigate(item.route) {
                      popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                }
                .testTag("nav_btn_${item.route}")
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) item.activeColor else Color.Gray,
                modifier = Modifier.size(24.dp)
              )
              
              Spacer(modifier = Modifier.height(2.dp))
              
              Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else Color.Gray
                )
              )
            }
          }
        }
      }
    }
  }
}
