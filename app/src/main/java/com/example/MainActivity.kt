package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.inspector.ChartOfAccountsScreen
import com.example.ui.inspector.HealthCheckScreen
import com.example.ui.inspector.InspectorViewModel
import com.example.ui.inspector.JournalBrowserScreen
import com.example.ui.inspector.TrialBalanceScreen
import com.example.ui.theme.SamMikrotikTheme

object AppBrand {
    const val NAME = "SamMikrotik"
    const val TAGLINE = "النظام المالي المحاسبي المتكامل لشبكات الإنترنت"
}

sealed class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    object Health : ScreenTab("السلامة والاتزان", Icons.Default.Security, "tab_health")
    object Accounts : ScreenTab("دليل الحسابات", Icons.Default.AccountTree, "tab_accounts")
    object Journal : ScreenTab("دفتر اليومية", Icons.Default.Book, "tab_journal")
    object TrialBalance : ScreenTab("ميزان المراجعة", Icons.Default.AccountBalance, "tab_trial_balance")

    companion object {
        val all = listOf(Health, Accounts, Journal, TrialBalance)
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SamMikrotikTheme {
                val inspectorViewModel: InspectorViewModel = viewModel()
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "${AppBrand.NAME} | الرقابة المحاسبية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            ScreenTab.all.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (selectedTabIndex) {
                        0 -> HealthCheckScreen(inspectorViewModel, modifier)
                        1 -> ChartOfAccountsScreen(inspectorViewModel, modifier)
                        2 -> JournalBrowserScreen(inspectorViewModel, modifier)
                        3 -> TrialBalanceScreen(inspectorViewModel, modifier)
                    }
                }
            }
        }
    }
}
