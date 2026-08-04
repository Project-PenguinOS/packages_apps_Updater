/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.penguinos.updater.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.penguinos.updater.R

private val AuroraAccent = Color(0xFFC8783E)
private val AuroraGradient =
    listOf(Color(0xFF3A2E57), Color(0xFF7A4F5C), Color(0xFFC98A4F), Color(0xFFE8B25A))

@Composable
fun InstallProgressScreen(
    version: String,
    installProgress: Int,
    isFinalizing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pct = installProgress.coerceIn(0, 100)
    val almostDone = isFinalizing || pct >= 100

    Box(
        modifier = modifier.fillMaxSize().background(Brush.verticalGradient(AuroraGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_penguin),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                alpha = 0.9f,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            if (almostDone) R.string.install_screen_almost_done
                            else R.string.install_screen_title
                        ),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (version.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.header_build_version, version),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.width(230.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isFinalizing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = AuroraAccent,
                        trackColor = Color.White.copy(alpha = 0.14f),
                    )
                } else {
                    val fraction by
                        animateFloatAsState(targetValue = pct / 100f, label = "installProgress")
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(AuroraAccent)
                        )
                    }
                    Text(
                        text = "$pct%",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = stringResource(R.string.install_screen_warning),
                modifier = Modifier.width(240.dp),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
