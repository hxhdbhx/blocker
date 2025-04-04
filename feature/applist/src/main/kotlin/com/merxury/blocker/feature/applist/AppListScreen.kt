/*
 * Copyright 2025 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.feature.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.merxury.blocker.core.designsystem.component.BlockerErrorAlertDialog
import com.merxury.blocker.core.designsystem.component.BlockerTopAppBar
import com.merxury.blocker.core.designsystem.component.BlockerWarningAlertDialog
import com.merxury.blocker.core.designsystem.component.PreviewThemes
import com.merxury.blocker.core.designsystem.icon.BlockerIcons
import com.merxury.blocker.core.designsystem.theme.BlockerTheme
import com.merxury.blocker.core.model.data.AppItem
import com.merxury.blocker.core.ui.TrackScreenViewEvent
import com.merxury.blocker.core.ui.applist.AppList
import com.merxury.blocker.core.ui.data.UiMessage
import com.merxury.blocker.core.ui.previewparameter.AppListPreviewParameterProvider
import com.merxury.blocker.core.ui.screen.EmptyScreen
import com.merxury.blocker.core.ui.screen.ErrorScreen
import com.merxury.blocker.core.ui.screen.InitializingScreen
import com.merxury.blocker.feature.applist.AppListUiState.Error
import com.merxury.blocker.feature.applist.AppListUiState.Initializing
import com.merxury.blocker.feature.applist.AppListUiState.Success
import com.merxury.blocker.feature.applist.R.string
import com.merxury.blocker.feature.applist.component.AppSortBottomSheetRoute
import com.merxury.blocker.feature.applist.component.TopAppBarMoreMenu

@Composable
fun AppListScreen(
    navigateToAppDetail: (String) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToSupportAndFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    highlightSelectedApp: Boolean = false,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val warningState by viewModel.warningState.collectAsStateWithLifecycle()
    val appList = viewModel.appListFlow.collectAsState()
    AppListScreen(
        uiState = uiState,
        appList = appList.value,
        onAppItemClick = {
            viewModel.onAppClick(it)
            navigateToAppDetail(it)
        },
        highlightSelectedApp = highlightSelectedApp,
        onClearCacheClick = viewModel::clearCache,
        onClearDataClick = viewModel::clearData,
        onForceStopClick = viewModel::forceStop,
        onUninstallClick = viewModel::uninstall,
        onEnableClick = viewModel::enable,
        onDisableClick = viewModel::disable,
        navigateToSettings = navigateToSettings,
        navigateToSupportAndFeedback = navigateToSupportAndFeedback,
        showAppSortBottomSheet = viewModel::showAppSortBottomSheet,
        modifier = modifier,
        onRefresh = {
            viewModel.loadData()
            viewModel.updateInstalledAppList()
        },
    )
    if (errorState != null) {
        BlockerErrorAlertDialog(
            title = errorState?.title.orEmpty(),
            text = errorState?.content.orEmpty(),
            onDismissRequest = viewModel::dismissErrorDialog,
        )
    }
    warningState?.let {
        BlockerWarningAlertDialog(
            title = it.title,
            text = stringResource(id = it.message),
            onDismissRequest = viewModel::dismissWarningDialog,
            onConfirmRequest = it.onPositiveButtonClicked,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppListScreen(
    uiState: AppListUiState,
    appList: List<AppItem>,
    modifier: Modifier = Modifier,
    highlightSelectedApp: Boolean = false,
    onAppItemClick: (String) -> Unit = {},
    onClearCacheClick: (String) -> Unit = {},
    onClearDataClick: (String) -> Unit = {},
    onForceStopClick: (String) -> Unit = {},
    onUninstallClick: (String) -> Unit = {},
    onEnableClick: (String) -> Unit = {},
    onDisableClick: (String) -> Unit = {},
    showAppSortBottomSheet: (Boolean) -> Unit = {},
    navigateToSettings: () -> Unit = {},
    navigateToSupportAndFeedback: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BlockerTopAppBar(
            title = stringResource(id = string.feature_applist_app_name),
            actions = {
                if (uiState is Success) {
                    IconButton(onClick = { showAppSortBottomSheet(true) }) {
                        Icon(
                            imageVector = BlockerIcons.Sort,
                            contentDescription = stringResource(id = string.feature_applist_sort_menu),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                TopAppBarMoreMenu(
                    navigateToSettings = navigateToSettings,
                    navigateToFeedback = navigateToSupportAndFeedback,
                )
            },
        )
        val appListTestTag = "appList:applicationList"
        when (uiState) {
            is Initializing -> InitializingScreen(processingName = uiState.processingName)

            is Success -> {
                val refreshingState = rememberPullRefreshState(
                    refreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                )
                Box(modifier = Modifier.pullRefresh(refreshingState)) {
                    if (appList.isEmpty()) {
                        EmptyScreen(textRes = string.feature_applist_no_applications_to_display)
                    } else {
                        AppList(
                            appList = appList,
                            highlightSelectedApp = highlightSelectedApp,
                            selectedPackageName = uiState.selectedPackageName,
                            onAppItemClick = onAppItemClick,
                            onClearCacheClick = onClearCacheClick,
                            onClearDataClick = onClearDataClick,
                            onForceStopClick = onForceStopClick,
                            onUninstallClick = onUninstallClick,
                            onEnableClick = onEnableClick,
                            onDisableClick = onDisableClick,
                            modifier = Modifier.testTag(appListTestTag),
                        )
                        if (uiState.showAppSortBottomSheet) {
                            AppSortBottomSheetRoute(
                                dismissHandler = { showAppSortBottomSheet(false) },
                            )
                        }
                    }
                    PullRefreshIndicator(
                        refreshing = uiState.isRefreshing,
                        state = refreshingState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        scale = true,
                    )
                }
            }

            is Error -> ErrorScreen(uiState.error)
        }
    }
    TrackScreenViewEvent(screenName = "AppListScreen")
}

@PreviewThemes
@Composable
private fun AppListScreenPreview(
    @PreviewParameter(AppListPreviewParameterProvider::class) appList: List<AppItem>,
) {
    BlockerTheme {
        Surface {
            AppListScreen(uiState = Success(), appList = appList)
        }
    }
}

@PreviewThemes
@Composable
private fun AppListScreenInitialPreview() {
    BlockerTheme {
        Surface {
            AppListScreen(uiState = Initializing("Blocker"), appList = listOf())
        }
    }
}

@PreviewThemes
@Composable
private fun AppListScreenErrorPreview() {
    BlockerTheme {
        Surface {
            AppListScreen(uiState = Error(UiMessage("Error")), appList = listOf())
        }
    }
}

@PreviewThemes
@Composable
private fun AppListScreenEmptyPreview() {
    BlockerTheme {
        Surface {
            AppListScreen(uiState = Success(), appList = listOf())
        }
    }
}
