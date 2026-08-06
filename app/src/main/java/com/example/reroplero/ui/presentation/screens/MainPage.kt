package com.example.reroplero.ui.presentation.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reroplero.ui.presentation.login.LoginPage
import com.example.reroplero.ui.theme.ReroPleroTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainPage : ComponentActivity() {
    private val viewModel: MainPageViewModel by viewModels()
    private val cryptoViewModel: CryptoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReroPleroTheme {
                val scope = rememberCoroutineScope()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val cryptoState by cryptoViewModel.state.collectAsStateWithLifecycle()

                val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
                val focusManager = LocalFocusManager.current
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect {
                        focusManager.clearFocus()
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            MainEffect.GoToLogin -> {
                                startActivity(Intent(this@MainPage, LoginPage::class.java))
                                finish()
                            }

                            MainEffect.GoToList -> pagerState.animateScrollToPage(Tab.TRANSLIST.ordinal)
                            is MainEffect.ShowError ->
                                Toast.makeText(this@MainPage, effect.message, Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
                }
                var lastBackPress by remember { mutableLongStateOf(0L) }
                var showSettings by remember { mutableStateOf(false) }
                BackHandler {
                    if (showSettings) {
                        showSettings = false
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPress < 2000) {
                            finish()
                        } else {
                            lastBackPress = now
                            Toast.makeText(
                                this@MainPage,
                                "Press back again to exit",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                if (showSettings){
                    Settings(onBack = {showSettings = false})
                }else{
                    Scaffold(
                        bottomBar = {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                NavigationBar(
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    Tab.entries.forEachIndexed { index, t ->
                                        if (t == Tab.TRANSACTION) {
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            NavigationBarItem(
                                                selected = pagerState.currentPage == index,
                                                onClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            index
                                                        )
                                                    }
                                                },
                                                icon = { Icon(t.icon, contentDescription = t.label) },
                                                label = { Text(t.label) },
                                            )
                                        }
                                    }
                                }

                                val onNewPage = pagerState.currentPage == Tab.TRANSACTION.ordinal
                                FloatingActionButton(
                                    onClick = {
                                        state.editing = null
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

                                ) {
                                    Icon(
                                        Tab.TRANSACTION.icon,
                                        contentDescription = Tab.TRANSACTION.label,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.padding(innerPadding)
                        ) { page ->
                            when (Tab.entries[page]) {
                                Tab.HOME -> Homescreen(
                                    state = state,
                                    username = state.username,
                                    total = state.total,
                                    onLogout = {
                                        viewModel.onIntent(MainIntent.Logout)
                                    },
                                    onSetLogoutDialog = { visible ->
                                        viewModel.onIntent(MainIntent.SetLogoutDialog(visible))
                                    },
                                    onOpenSettings = {
                                        showSettings = true
                                    }
                                )

                                Tab.ANALYTICS -> AnalyticsScreen(viewModel, state)
                                Tab.TRANSACTION -> NewtransScreen(
                                    viewModel,
                                    editing = state.editing,
                                    onLeave = { state.editing = null },
                                    state = state
                                )

                                Tab.TRANSLIST -> TransListScreen(
                                    viewModel,
                                    onEdit = { payment ->
                                        state.editing = payment
                                        scope.launch { pagerState.animateScrollToPage(Tab.TRANSACTION.ordinal) }
                                    },
                                    onFlingNext = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(Tab.CRYPTO.ordinal)
                                        }
                                    },
                                    onFlingPrev = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(Tab.TRANSACTION.ordinal)
                                        }
                                    }
                                )

                                Tab.CRYPTO -> CryptoScreen(
                                    state = cryptoState,
                                    onRefresh = { cryptoViewModel.refresh() }
                                )
                            }
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