/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.penguinos.updater.deviceinfo

import android.content.res.Configuration
import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.penguinos.updater.R
import org.penguinos.updater.deviceinfo.actions.DeviceInfoTvAction
import org.penguinos.updater.util.StringUtil
import java.util.Date

private val MarkSize = 40.dp

private const val LABEL_ALPHA = 0.7f

@Composable
fun DeviceInfoBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(context, configuration.locales) { StringUtil.getCurrentLocale(context) }
    val buildVersion = remember { DeviceInfoUtils.buildVersion }
    val androidVersion = remember { DeviceInfoUtils.androidVersion }
    val buildDate = remember(locale) {
        DateFormat.getInstanceForSkeleton("MMMd", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(DeviceInfoUtils.buildDateTimestamp * 1000L))
    }
    val securityPatch = remember(locale) {
        StringUtil.formatSecurityPatch(context, DeviceInfoUtils.buildSecurityPatch)
    }

    DeviceInfoBanner(
        buildVersion = buildVersion,
        androidVersion = androidVersion,
        buildDate = buildDate,
        securityPatch = securityPatch,
        modifier = modifier,
    )
}

@Composable
fun DeviceInfoBanner(
    buildVersion: String,
    androidVersion: String,
    buildDate: String,
    securityPatch: String,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalConfiguration.current.uiMode
    val isTv = (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val onGradient = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SettingsDimension.itemPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsSpace.medium1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_penguin),
                contentDescription = stringResource(R.string.brand_name),
                modifier = Modifier.size(MarkSize),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(onGradient),
            )
            Spacer(Modifier.width(SettingsSpace.small1))
            Text(
                text = stringResource(R.string.header_build_version, buildVersion),
                style = MaterialTheme.typography.headlineSmall,
                color = onGradient,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall2)) {
            Text(
                text = stringResource(R.string.header_android_version, androidVersion),
                style = MaterialTheme.typography.titleMedium,
                color = onGradient,
            )
            InfoRow(
                label = stringResource(R.string.build_date),
                value = buildDate,
                color = onGradient,
            )
            InfoRow(
                label = stringResource(R.string.security_update),
                value = securityPatch,
                color = onGradient,
            )
        }

        if (isTv) {
            DeviceInfoTvAction()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color.copy(alpha = LABEL_ALPHA),
        )
        Spacer(Modifier.width(SettingsSpace.small1))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES
)
@UiModePreviews
@Composable
private fun DeviceInfoBannerPreview() {
    SettingsTheme {
        DeviceInfoBanner(
            buildVersion = "23.2",
            androidVersion = "16",
            buildDate = "Feb 20",
            securityPatch = "Feb 2026",
        )
    }
}
