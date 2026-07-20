package com.example.reroplero.ui.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.ui.theme.ReroPleroTheme
import kotlinx.coroutines.launch

class MainPage : ComponentActivity() {
    private val viewModel by lazy { MainPageViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
          ReroPleroTheme {
            val scope = rememberCoroutineScope()
            var total by remember { mutableDoubleStateOf(0.0) }
            var username by remember { mutableStateOf("")}
            LaunchedEffect(Unit) {
                val user = viewModel.getCurrentUser()
                if (user == null) {
                    finish()
                    return@LaunchedEffect
                }
                username = user
                total = viewModel.getCurrentMoney()
            }

              val pagerState = rememberPagerState( pageCount = {Tab.entries.size} )
              var editing by remember { mutableStateOf<Payment?>(null)}
              val focusManager = LocalFocusManager.current
              LaunchedEffect(pagerState) {
                  snapshotFlow { pagerState.currentPage }.collect {
                      focusManager.clearFocus()
                  }
              }

            Scaffold(
                bottomBar = {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ){
                        NavigationBar(
                                modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Tab.entries.forEachIndexed { index, t ->
                                if (t == Tab.TRANSACTION){
                                    Spacer(Modifier.weight(1f))
                                }else {
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = { if (t == Tab.TRANSACTION) editing = null
                                                  scope.launch { pagerState.animateScrollToPage(index) }},
                                        icon = { Icon(t.icon, contentDescription = t.label) },
                                        label = { Text(t.label) },
                                    )
                                }
                            }
                        }

                        val onNewPage = pagerState.currentPage == Tab.TRANSACTION.ordinal
                        FloatingActionButton(
                            onClick = {
                                editing = null
                                scope.launch {
                                    pagerState.animateScrollToPage(Tab.TRANSACTION.ordinal)
                                }
                            },
                                shape = CircleShape,
                                containerColor = if (onNewPage) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (onNewPage) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.align(Alignment.TopCenter)
                                                .size(64.dp)
                                                .offset(y = (-24).dp)

                        ){
                            Icon(Tab.TRANSACTION.icon, contentDescription = Tab.TRANSACTION.label, modifier = Modifier.size(40.dp))
                        }
                    }
                }
            ) {
                innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(innerPadding)
                ) { page ->
                    when (Tab.entries[page]) {
                        Tab.HOME -> Homescreen(
                            username = username,
                            total = total,
                        )

                        Tab.ANALYTICS -> AnalyticsScreen( viewModel )
                        Tab.TRANSACTION -> NewtransScreen(
                            viewModel,
                            editing = editing,
                            onLeave = {editing = null},
                            onSaved = {
                                scope.launch { total = viewModel.getCurrentMoney() }
                                scope.launch { pagerState.animateScrollToPage(Tab.TRANSLIST.ordinal) }
                            },
                        )

                        Tab.TRANSLIST -> TransListScreen(viewModel, onEdit = { payment ->
                            editing = payment
                            scope.launch { pagerState.animateScrollToPage(Tab.TRANSACTION.ordinal) }
                           },
                            onChanged = {
                                scope.launch {
                                    total = viewModel.getCurrentMoney()
                                }
                            }
                        )

                        Tab.CRYPTO -> CryptoScreen()
                    }
                }
            }
          }

        }
    }
}


enum class Tab(val label: String, val icon: ImageVector){
    HOME("Home", Icons.Default.Home),
    ANALYTICS(label = "Analytics", Icons.Default.Check),
    TRANSACTION(label = "New", Icons.Default.AddCircle),
    TRANSLIST(label = "List", Icons.AutoMirrored.Filled.List),
    CRYPTO(label = "Crypto", Icons.Filled.Lock)

}