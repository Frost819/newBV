package dev.frost819.newbv.app.ui.screen.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.rememberDrawerState
import dev.frost819.newbv.app.ui.component.user.UserPanel
import dev.frost819.newbv.app.ui.screen.home.HomeContent
import dev.frost819.newbv.app.viewmodel.user.UserViewModel
import dev.frost819.newbv.data.datastore.LeftNaviItem
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 主页面框架。
 *
 * 左侧 [NavigationDrawer]（永久展开）+ 右侧内容区（[AnimatedContent] 切换）。
 *
 * 左侧栏：用户头像（点击显示 [UserPanel]）+ 6 导航项 + 设置。
 * 内容区：Home / Search / Personal / UGC / PGC / Live（后 5 项为占位）。
 *
 * 双击返回退出：首次返回显示 Toast，3 秒内再次返回退出 App。
 *
 * @param navController 导航控制器（跳转设置/登录等页面）。
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
) {
    val userUiState by userViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showUserPanel by remember { mutableStateOf(false) }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDrawerItem by rememberSaveable { mutableStateOf(Prefs.homeLeftNavItem) }
    var focusInitialized by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val homeFocusRequester = remember { FocusRequester() }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1000 * 3) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastPressBack = currentTime
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    val onFocusToContent: () -> Unit = {
        runCatching { homeFocusRequester.requestFocus() }
    }

    LaunchedEffect(Unit) {
        if (!focusInitialized) {
            focusInitialized = true
            runCatching { onFocusToContent() }
        }
    }

    BackHandler {
        handleBack()
    }

    NavigationDrawer(
        modifier = modifier,
        drawerContent = {
            LeftNaviContent(
                isLogin = userUiState.isLogin,
                avatar = userUiState.avatar,
                selectedItem = selectedDrawerItem,
                onLeftNaviItemChanged = { selectedDrawerItem = it },
                onOpenSettings = {
                    navController.navigate(dev.frost819.newbv.app.ui.navigation.SettingsRoute)
                },
                onFocusToContent = onFocusToContent,
                onShowUserPanel = {
                    showUserPanel = true
                },
                onLogin = {
                    navController.navigate(dev.frost819.newbv.app.ui.navigation.LoginRoute)
                },
            )
        },
        drawerState = drawerState,
    ) {
        Box(modifier = Modifier) {
            AnimatedContent(
                targetState = selectedDrawerItem,
                label = "main-animated-content",
                transitionSpec = {
                    val coefficient = 20
                    if (targetState.ordinal < initialState.ordinal) {
                        fadeIn() + slideInVertically { -it / coefficient } togetherWith
                            fadeOut() + slideOutVertically { it / coefficient }
                    } else {
                        fadeIn() + slideInVertically { it / coefficient } togetherWith
                            fadeOut() + slideOutVertically { -it / coefficient }
                    }
                },
            ) { screen ->
                when (screen) {
                    LeftNaviItem.Home -> HomeContent(
                        navFocusRequester = homeFocusRequester,
                        navController = navController,
                    )
                    LeftNaviItem.Search -> {
                        val searchInputViewModel: dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel =
                            androidx.hilt.navigation.compose.hiltViewModel()
                        dev.frost819.newbv.app.ui.screen.search.SearchInputContent(
                            viewModel = searchInputViewModel,
                            focusRequester = homeFocusRequester,
                            onSearch = { keyword ->
                                searchInputViewModel.commitSearch(keyword)
                                navController.navigate(
                                    dev.frost819.newbv.app.ui.navigation.SearchResultRoute(keyword = keyword),
                                )
                            },
                        )
                    }
                    LeftNaviItem.Personal -> dev.frost819.newbv.app.ui.screen.personal.PersonalContent(
                        navFocusRequester = homeFocusRequester,
                        navController = navController,
                    )
                    LeftNaviItem.UGC -> dev.frost819.newbv.app.ui.screen.ugc.UgcContent(
                        navFocusRequester = homeFocusRequester,
                        navController = navController,
                    )
                    LeftNaviItem.PGC -> dev.frost819.newbv.app.ui.screen.pgc.PgcContent(
                        navFocusRequester = homeFocusRequester,
                        navController = navController,
                    )
                    LeftNaviItem.Live -> dev.frost819.newbv.app.ui.screen.live.LiveContent(
                        navFocusRequester = homeFocusRequester,
                        navController = navController,
                    )
                }
            }

            if (showUserPanel) {
                val userPanelFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    runCatching { userPanelFocusRequester.requestFocus() }
                }
                BackHandler { showUserPanel = false }
                Dialog(
                    onDismissRequest = { showUserPanel = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                    ) {
                        UserPanel(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(400.dp)
                                .padding(12.dp),
                            focusRequester = userPanelFocusRequester,
                            onHide = { showUserPanel = false },
                            onGoUserSwitch = {
                                showUserPanel = false
                                navController.navigate(dev.frost819.newbv.app.ui.navigation.UserSwitchRoute)
                            },
                            onGoFollowList = {
                                showUserPanel = false
                                val uid = userUiState.uid
                                if (uid != 0L) {
                                    navController.navigate(
                                        dev.frost819.newbv.app.ui.navigation.FollowRoute(mid = uid)
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 占位内容（UGC/PGC/Live 等未实现的页面）。
 */
@Composable
private fun PlaceholderContent(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.tv.material3.Text(
            text = "$title (待实现)",
            style = androidx.tv.material3.MaterialTheme.typography.displaySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderContentPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        PlaceholderContent(title = "分区")
    }
}
