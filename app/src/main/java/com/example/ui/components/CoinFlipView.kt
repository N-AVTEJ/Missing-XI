package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.MatchTossEngine
import com.example.util.TossChoice
import com.example.util.TossStatus
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * High-fidelity 3D coin flip component built with Jetpack Compose Animatable + graphicsLayer.
 *
 * Guarantees that the final resting coin face always matches [targetResult]:
 *  - HEADS settles at 0° (mod 360°) displaying the burnished gold Heads face.
 *  - TAILS settles at 180° (mod 360°) displaying the platinum-indigo Tails face.
 */
@Composable
fun RealisticCoinFlipView(
    tossStatus: TossStatus,
    targetResult: TossChoice?,
    flipSequenceNumber: Int,
    modifier: Modifier = Modifier,
    reducedMotionOverride: Boolean? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val isReducedMotion = reducedMotionOverride ?: remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val rotationYAnim = remember { Animatable(0f) }
    val tossHeightAnim = remember { Animatable(0f) } // 0f at rest, 1f at peak height
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(flipSequenceNumber, targetResult, tossStatus) {
        when (tossStatus) {
            TossStatus.IN_PROGRESS -> {
                if (targetResult != null) {
                    val finalRemainder = if (targetResult == TossChoice.HEADS) 0f else 180f
                    if (isReducedMotion) {
                        rotationYAnim.snapTo(finalRemainder)
                        scaleAnim.animateTo(1.08f, animationSpec = tween(150))
                        scaleAnim.animateTo(1.0f, animationSpec = tween(150))
                    } else {
                        val totalDuration = MatchTossEngine.FLIP_ANIMATION_DURATION_MS.toInt()
                        // Start from clean 0° or 180° baseline and perform 6 full 3D flips + final face alignment
                        val targetDegrees = 6f * 360f + finalRemainder
                        rotationYAnim.snapTo(0f)
                        tossHeightAnim.snapTo(0f)
                        scaleAnim.snapTo(1f)

                        // Launch spin, vertical arc, and depth scale concurrently
                        launch {
                            rotationYAnim.animateTo(
                                targetValue = targetDegrees,
                                animationSpec = tween(
                                    durationMillis = totalDuration,
                                    easing = CubicBezierEasing(0.16f, 0.88f, 0.28f, 1.0f)
                                )
                            )
                        }
                        launch {
                            // Rise quickly to apex, hang briefly, fall and settle
                            tossHeightAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = (totalDuration * 0.44f).toInt(),
                                    easing = CubicBezierEasing(0.22f, 0.9f, 0.4f, 1.0f)
                                )
                            )
                            tossHeightAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = (totalDuration * 0.46f).toInt(),
                                    easing = CubicBezierEasing(0.55f, 0.05f, 0.78f, 0.95f)
                                )
                            )
                            // Subtle landing micro-bounce
                            tossHeightAnim.animateTo(0.08f, animationSpec = tween(100))
                            tossHeightAnim.animateTo(0f, animationSpec = tween(120))
                        }
                        launch {
                            scaleAnim.animateTo(
                                targetValue = 1.18f,
                                animationSpec = tween(
                                    durationMillis = (totalDuration * 0.45f).toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                            scaleAnim.animateTo(
                                targetValue = 1.0f,
                                animationSpec = tween(
                                    durationMillis = (totalDuration * 0.55f).toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                }
            }

            TossStatus.COMPLETED -> {
                if (targetResult != null) {
                    val restingAngle = if (targetResult == TossChoice.HEADS) 0f else 180f
                    rotationYAnim.snapTo(restingAngle)
                    tossHeightAnim.snapTo(0f)
                    scaleAnim.snapTo(1f)
                }
            }

            TossStatus.NOT_STARTED -> {
                val restingAngle = when (targetResult) {
                    TossChoice.TAILS -> 180f
                    else -> 0f
                }
                rotationYAnim.snapTo(restingAngle)
                tossHeightAnim.snapTo(0f)
                scaleAnim.snapTo(1f)
            }
        }
    }

    val currentAngle = rotationYAnim.value
    val normalizedAngle = ((currentAngle % 360f) + 360f) % 360f
    val showingTailsFace = normalizedAngle > 90f && normalizedAngle < 270f
    val elevationProgress = tossHeightAnim.value
    val liftPx = with(density) { (-78.dp).toPx() * elevationProgress }

    val coinDescription = when (tossStatus) {
        TossStatus.IN_PROGRESS -> "Coin flipping in the air"
        TossStatus.COMPLETED -> "Coin landed on ${targetResult?.displayName ?: "result"}"
        TossStatus.NOT_STARTED -> "Ready to flip coin"
    }

    Column(
        modifier = modifier.semantics { contentDescription = coinDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(184.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle outer metallic aura when settled or in flight
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowRadius = size.minDimension * (0.46f + 0.06f * elevationProgress)
                val glowColor = if (showingTailsFace) {
                    Color(0xFF6366F1).copy(alpha = 0.22f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = 0.24f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = glowRadius * 1.25f
                    ),
                    radius = glowRadius * 1.25f,
                    center = center
                )
            }

            // 3D Rotating Coin Disc
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .graphicsLayer {
                        translationY = liftPx
                        rotationY = currentAngle
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        cameraDistance = 14f * this.density
                    }
                    .clip(CircleShape)
            ) {
                if (showingTailsFace) {
                    // Counter-flip horizontally so Tails artwork & typography are never mirrored
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = -1f }
                    ) {
                        CoinFaceContent(
                            isHeads = false,
                            sheenPhase = normalizedAngle
                        )
                    }
                } else {
                    CoinFaceContent(
                        isHeads = true,
                        sheenPhase = normalizedAngle
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Ground Shadow that shrinks and softens while the coin is airborne
        Canvas(
            modifier = Modifier
                .size(width = 136.dp, height = 18.dp)
        ) {
            val shadowScale = 1f - (0.38f * elevationProgress)
            val shadowAlpha = 0.42f - (0.24f * elevationProgress)
            val ovalW = size.width * shadowScale
            val ovalH = size.height * shadowScale
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = shadowAlpha.coerceIn(0.08f, 0.5f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = ovalW / 2f
                ),
                topLeft = Offset((size.width - ovalW) / 2f, (size.height - ovalH) / 2f),
                size = androidx.compose.ui.geometry.Size(ovalW, ovalH)
            )
        }
    }
}

@Composable
private fun CoinFaceContent(
    isHeads: Boolean,
    sheenPhase: Float
) {
    val outerRimColor = if (isHeads) Color(0xFFFDE68A) else Color(0xFFC7D2FE)
    val rimEdgeColor = if (isHeads) Color(0xFFB45309) else Color(0xFF3730A3)
    val gradientColors = if (isHeads) {
        listOf(
            Color(0xFFFCD34D),
            Color(0xFFF59E0B),
            Color(0xFFB45309)
        )
    } else {
        listOf(
            Color(0xFFA5B4FC),
            Color(0xFF4F46E5),
            Color(0xFF1E1B4B)
        )
    }

    val sheenHighlight = (abs(sin(Math.toRadians(sheenPhase.toDouble()))).toFloat() * 0.25f)
        .coerceIn(0f, 0.25f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(420f, 420f)
                )
            )
            .border(5.dp, outerRimColor, CircleShape)
            .border(2.dp, rimEdgeColor.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Milled inner ring & metallic sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val insetRadius = size.minDimension * 0.42f
            drawCircle(
                color = outerRimColor.copy(alpha = 0.55f),
                radius = insetRadius,
                style = Stroke(width = 2.dp.toPx())
            )
            // Specular highlight overlay
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f + sheenHighlight),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.16f)
                    )
                ),
                radius = size.minDimension * 0.48f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isHeads) Icons.Default.Star else Icons.Default.Shield,
                contentDescription = if (isHeads) "Heads Emblem" else "Tails Emblem",
                tint = if (isHeads) Color(0xFFFFFBEB) else Color(0xFFE0E7FF),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHeads) "HEADS" else "TAILS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    fontSize = 22.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "MISSING XI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}
