/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.penguinos.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import org.penguinos.updater.controller.UpdaterController
import org.penguinos.updater.data.Update
import org.penguinos.updater.data.UpdateStatus
import org.penguinos.updater.deviceinfo.DeviceInfoBanner
import org.penguinos.updater.deviceinfo.DeviceInfoUtils
import org.penguinos.updater.preferences.PreferencesActivity
import org.penguinos.updater.ui.InstallProgressScreen
import org.penguinos.updater.updates.UpdateList
import org.penguinos.updater.updates.action.AlertDialogState
import org.penguinos.updater.updates.action.UpdateActionDialog
import org.penguinos.updater.updates.action.UpdateActionHandler
import org.penguinos.updater.updates.state.UpdateItemStateMapper
import org.penguinos.updater.updatescheck.UpdatesCheck
import org.penguinos.updater.updatescheck.UpdatesCheckModel
import org.penguinos.updater.updatescheck.rememberUpdatesCheckUiState

private val AuroraAccent = Color(0xFFC8783E)

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                SettingsTheme {
                    val auroraScheme = MaterialTheme.colorScheme.copy(
                        primary = AuroraAccent,
                        onPrimary = Color.White,
                        secondary = AuroraAccent,
                        onSecondary = Color.White,
                        tertiary = AuroraAccent,
                        onTertiary = Color.White,
                    )
                    MaterialTheme(colorScheme = auroraScheme) {
                    val uiState by viewModel.uiState.collectAsState()
                    UpdatesScaffoldContent(
                        uiState = uiState,
                        updaterController = activeUpdaterController,
                        controllerStateVersion = controllerStateVersion,
                        onRefreshClick = { onRefreshClick() },
                        onLocalUpdateClick = { onLocalUpdateClick() },
                        onPreferencesClick = {
                            startActivity(
                                Intent(
                                    this@UpdatesScaffoldActivity,
                                    PreferencesActivity::class.java,
                                )
                            )
                        },
                        onControllerStateChanged = { notifyControllerStateChanged() },
                    )
                    }
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val title = getTitleForUpdateStatus(uiState.updates)

    val installingUpdate = remember(uiState.updates, updaterController, controllerStateVersion) {
        val controller = updaterController ?: return@remember null
        uiState.updates.firstNotNullOfOrNull { update ->
            controller.getUpdate(update.downloadId)?.takeIf {
                it.status == UpdateStatus.INSTALLING ||
                        it.status == UpdateStatus.INSTALLATION_SUSPENDED
            }
        }
    }
    if (installingUpdate != null) {
        InstallProgressScreen(
            version = installingUpdate.version,
            installProgress = installingUpdate.installProgress,
            isFinalizing = installingUpdate.isFinalizing,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    val context = LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(title) { activity.title = title }

    Box(modifier = Modifier.fillMaxSize().background(auroraBackgroundBrush())) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                LargeTopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { activity.finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_up),
                            )
                        }
                    },
                    actions = {
                        UpdaterOverflowMenu(
                            onLocalUpdateClick = onLocalUpdateClick,
                            onPreferencesClick = onPreferencesClick,
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (isWideScreen()) {
                WideUpdatesScaffold(
                    paddingValues = paddingValues,
                    updatesCheckModel = uiState.updatesCheckModel,
                    updates = uiState.updates,
                    updaterController = updaterController,
                    controllerStateVersion = controllerStateVersion,
                    showDialog = { actionDialogState.value = it },
                    onControllerStateChanged = onControllerStateChanged,
                    onRefreshClick = onRefreshClick,
                    onLocalUpdateClick = onLocalUpdateClick,
                    onPreferencesClick = onPreferencesClick,
                )
            } else {
                UpdatesScaffold(
                    paddingValues = paddingValues,
                    updatesCheckModel = uiState.updatesCheckModel,
                    updates = uiState.updates,
                    updaterController = updaterController,
                    controllerStateVersion = controllerStateVersion,
                    showDialog = { actionDialogState.value = it },
                    onControllerStateChanged = onControllerStateChanged,
                    onRefreshClick = onRefreshClick,
                    onLocalUpdateClick = onLocalUpdateClick,
                    onPreferencesClick = onPreferencesClick,
                )
            }
        }
    }
}

@Composable
private fun auroraBackgroundBrush(): Brush {
    val colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF3A2E57), Color(0xFF7A4F5C), Color(0xFFC98A4F), Color(0xFFE8B25A))
    } else {
        listOf(Color(0xFFFFF8EF), Color(0xFFFDE7CF), Color(0xFFF7CFA0), Color(0xFFF0B673))
    }
    return Brush.verticalGradient(colors)
}

@Composable
private fun UpdaterOverflowMenu(
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val context = LocalContext.current
    MoreOptionsAction {
        MenuItem(text = stringResource(R.string.show_changelog)) {
            val url = context.getString(R.string.menu_changelog_url, DeviceInfoUtils.device)
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
        MenuItem(text = stringResource(R.string.report_issues)) {
            val url = context.getString(R.string.report_issue_url)
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
        MenuItem(text = stringResource(R.string.local_update_import)) { onLocalUpdateClick() }
        MenuItem(text = stringResource(R.string.menu_preferences)) { onPreferencesClick() }
    }
}

@Composable
private fun WideUpdatesScaffold(
    paddingValues: PaddingValues,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val updatesCheckUiState = rememberUpdatesCheckUiState(updatesCheckModel.state)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding(),
            )
    ) {
        DeviceInfoBanner(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            UpdateListSection(
                updates = updates,
                updaterController = updaterController,
                controllerStateVersion = controllerStateVersion,
                isUpdatesCheckStatusVisible = updatesCheckUiState.isStatusVisible,
                showDialog = showDialog,
                onControllerStateChanged = onControllerStateChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            )
            UpdatesCheck(
                model = updatesCheckModel,
                uiState = updatesCheckUiState,
                onCheckClick = onRefreshClick,
            )
        }
    }
}

@Composable
private fun UpdatesScaffold(
    paddingValues: PaddingValues,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val updatesCheckUiState = rememberUpdatesCheckUiState(updatesCheckModel.state)

    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DeviceInfoBanner()
            UpdateListSection(
                updates = updates,
                updaterController = updaterController,
                controllerStateVersion = controllerStateVersion,
                isUpdatesCheckStatusVisible = updatesCheckUiState.isStatusVisible,
                showDialog = showDialog,
                onControllerStateChanged = onControllerStateChanged,
            )
        }
        UpdatesCheck(
            model = updatesCheckModel,
            uiState = updatesCheckUiState,
            onCheckClick = onRefreshClick,
        )
    }
}

@Composable
private fun UpdateListSection(
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    isUpdatesCheckStatusVisible: Boolean,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val networkMonitor =
        remember { (context.applicationContext as UpdaterApplication).networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository =
        remember { (context.applicationContext as UpdaterApplication).userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )

    val updateItems = remember(
        updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        updates.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = showDialog,
            )
        }
    }

    UpdateList(
        items = updateItems,
        isUpdatesCheckStatusVisible = isUpdatesCheckStatusVisible,
        onAction = { action, downloadId ->
            val controller = updaterController ?: return@UpdateList
            val update = controller.getUpdate(downloadId) ?: return@UpdateList
            actionHandler?.perform(action, update)
            onControllerStateChanged()
        },
        modifier = modifier,
    )
}

@Composable
private fun isWideScreen(): Boolean {
    val minWideScreenWidth = 600.dp
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize

    return with(density) { windowSize.width.toDp() >= minWideScreenWidth }
}

@Composable
private fun getTitleForUpdateStatus(updates: List<Update>): String = when {
    updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } ->
        stringResource(R.string.installing_update_finished)

    updates.any { it.status == UpdateStatus.INSTALLATION_FAILED } ->
        stringResource(R.string.installing_update_error)

    updates.any {
        it.status == UpdateStatus.INSTALLING ||
                it.status == UpdateStatus.INSTALLATION_SUSPENDED
    } -> stringResource(R.string.installing_update)

    else -> stringResource(R.string.display_name)
}
