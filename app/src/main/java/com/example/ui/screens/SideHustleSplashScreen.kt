package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.Emerald10
import com.example.ui.theme.Emerald15
import com.example.ui.theme.Emerald40
import com.example.ui.theme.Emerald80
import com.example.ui.theme.Emerald90
import com.example.ui.theme.Gold40
import com.example.ui.theme.Gold80
import com.example.ui.theme.Gold90
import com.example.ui.theme.SpaceGroteskFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_DURATION_MILLIS = 2400L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SideHustleSplashScreen(
    onFinished: () -> Unit,
) {
    val emblemScale = remember { Animatable(0.72f) }
    val contentAlpha = remember { Animatable(0f) }
    val progressSweep = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splashAura")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseFactor",
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitAngle",
    )

    LaunchedEffect(Unit) {
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            )
        }
        launch {
            progressSweep.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SPLASH_DURATION_MILLIS.toInt(),
                    easing = LinearEasing,
                ),
            )
        }
        delay(SPLASH_DURATION_MILLIS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            )
            .testTag("side_hustle_splash"),
    ) {
        // Generated Luxury Emerald & Gold Splash Artwork
        Image(
            painter = painterResource(id = R.drawable.img_splash_side_hustle),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.78f),
        )

        // Contrast-protecting Vignette Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Emerald10.copy(alpha = 0.72f),
                            DarkBackground.copy(alpha = 0.52f),
                            DarkBackground.copy(alpha = 0.90f),
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .align(Alignment.Center)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Status Capsule
            Surface(
                color = Emerald15.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Emerald80.copy(alpha = 0.45f)),
                modifier = Modifier.alpha(contentAlpha.value),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Gold80,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "INSTANT CASH REWARDS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald90,
                    )
                }
            }

            // Center Hero Emblem + Brand Wordmark + Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .scale(emblemScale.value)
                    .alpha(contentAlpha.value),
            ) {
                Box(
                    modifier = Modifier.size(152.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Emerald40.copy(alpha = 0.45f),
                                    Gold40.copy(alpha = 0.14f),
                                    Color.Transparent,
                                )
                            ),
                            radius = radius * pulseFactor,
                        )
                        drawCircle(
                            color = Emerald80.copy(alpha = 0.32f),
                            radius = radius * 0.80f * pulseFactor,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Gold80,
                                    Emerald80,
                                    Color.Transparent,
                                    Gold80,
                                )
                            ),
                            startAngle = orbitAngle,
                            sweepAngle = 210f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
                            size = size * 0.76f,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    // Metallic Emerald & Gold Center Crest
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.5.dp, Gold80.copy(alpha = 0.75f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(92.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Emerald15,
                                            Emerald40,
                                            Gold40,
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payments,
                                contentDescription = stringResource(R.string.splash_title),
                                tint = Color.White,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.splash_title),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 46.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.0).sp,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Emerald80, Gold80)
                                )
                            ),
                    )

                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp,
                        ),
                        color = Gold90,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SplashFeaturePill("Watch & Earn")
                    SplashFeaturePill("Daily Analytics")
                    SplashFeaturePill("Fast Payouts")
                }
            }

            // Bottom Loading Bar & Tap-to-Enter Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progressSweep.value },
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Gold80,
                    trackColor = Color.White.copy(alpha = 0.16f),
                )

                TextButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("splash_continue_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Emerald80,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap to enter Side Hustle",
                        style = MaterialTheme.typography.labelLarge,
                        color = Emerald90,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashFeaturePill(label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.09f),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
