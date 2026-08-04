/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.penguinos.updater.updatescheck

import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.settingslib.spa.framework.theme.SettingsDimension
import kotlinx.coroutines.delay
import org.penguinos.updater.R
import java.util.Date

private const val MIN_CHECKING_DURATION_MILLIS = 2_000L

sealed interface UpdatesCheckState {
    data object Idle : UpdatesCheckState
    data object Checking : UpdatesCheckState
    data object NoInternet : UpdatesCheckState
    data object Error : UpdatesCheckState
}

data class UpdatesCheckModel(
    val state: UpdatesCheckState,
    val lastCheckedTimestamp: Long,
    val canCheckForUpdates: Boolean,
)

class UpdatesCheckUiState internal constructor(
    internal val displayedState: UpdatesCheckState,
) {
    val isStatusVisible = when (displayedState) {
        UpdatesCheckState.Idle -> false
        UpdatesCheckState.Checking,
        UpdatesCheckState.NoInternet,
        UpdatesCheckState.Error -> true
    }
}

@Composable
fun UpdatesCheck(
    model: UpdatesCheckModel,
    uiState: UpdatesCheckUiState,
    onCheckClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lastCheckedText = remember(model.lastCheckedTimestamp) {
        formatLastCheckedText(context, model.lastCheckedTimestamp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SettingsDimension.itemPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SettingsDimension.itemPaddingVertical),
    ) {
        when (uiState.displayedState) {
            UpdatesCheckState.Idle -> Unit
            UpdatesCheckState.Checking -> StatusMessage(R.string.checking_for_updates)
            UpdatesCheckState.NoInternet -> StatusMessage(R.string.check_your_internet_connection)
            UpdatesCheckState.Error -> StatusMessage(R.string.updates_check_failed)
        }

        Text(
            text = lastCheckedText,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        if (model.canCheckForUpdates && uiState.displayedState !is UpdatesCheckState.Checking) {
            CheckForUpdatesButton(onClick = onCheckClick)
        }
    }
}

// Matches the setup wizard's primary pill button (bg_pill_primary): solid Aurora accent,
// fully rounded, 56dp tall, white bold 16sp label.
private val PillButtonMinHeight = 56.dp

@Composable
private fun CheckForUpdatesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PillButtonMinHeight),
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = stringResource(R.string.check_for_updates),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Text-only status message (checking / error / no-internet), white for contrast on the gradient. */
@Composable
private fun StatusMessage(@StringRes textResId: Int) {
    Text(
        text = stringResource(textResId),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

/**
 * Keeps the checking state visible long enough for the progress animation to be readable.
 */
@Composable
private fun rememberStateWithMinimumCheckingDuration(
    state: UpdatesCheckState,
): UpdatesCheckState {
    var displayedState by remember { mutableStateOf(state) }
    var checkingStartedAtMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state) {
        if (state == UpdatesCheckState.Checking) {
            checkingStartedAtMillis = SystemClock.elapsedRealtime()
            displayedState = state
            return@LaunchedEffect
        }

        if (displayedState == UpdatesCheckState.Checking) {
            val elapsed = SystemClock.elapsedRealtime() - checkingStartedAtMillis
            val remaining = MIN_CHECKING_DURATION_MILLIS - elapsed
            if (remaining > 0L) delay(remaining)
        }

        displayedState = state
    }

    return displayedState
}

@Composable
internal fun rememberUpdatesCheckUiState(
    state: UpdatesCheckState,
): UpdatesCheckUiState {
    val displayedState = rememberStateWithMinimumCheckingDuration(state)
    return remember(displayedState) { UpdatesCheckUiState(displayedState) }
}

/**
 * Formats the last checked time.
 *
 * Today's checks show only the time. Older checks show both date and time.
 */
private fun formatLastCheckedText(
    context: Context,
    timestampMillis: Long,
): String {
    val time = DateFormat.getTimeFormat(context).format(Date(timestampMillis))

    if (DateUtils.isToday(timestampMillis)) {
        return context.getString(R.string.header_last_updates_check_time, time)
    }

    val date = DateUtils.formatDateTime(
        context,
        timestampMillis,
        DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_NO_YEAR,
    )

    return context.getString(R.string.header_last_updates_check, date, time)
}
