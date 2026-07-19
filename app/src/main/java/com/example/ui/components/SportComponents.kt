package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun FrostedMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBg)
            .drawBehind {
                // Top-Left Glowing Indigo Mesh Accent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(IndigoAccent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(-150f, -150f),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(-150f, -150f)
                )
                // Mid-Right Glowing Fuchsia Mesh Accent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FuchsiaAccent.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width + 150f, size.height * 0.5f),
                        radius = size.width * 0.95f
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width + 150f, size.height * 0.5f)
                )
            }
    ) {
        content()
    }
}

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
    ),
    backgroundBrush: Brush = Brush.linearGradient(
        colors = listOf(Color(0x13FFFFFF), Color(0x06FFFFFF))
    ),
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = modifier
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(cornerRadius.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(cornerRadius.dp)
    ) {
        Column(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowingColor: Color = IndigoAccent,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp)) // Premium Pill-shaped buttons
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (enabled) listOf(glowingColor, glowingColor.copy(alpha = 0.3f))
                    else listOf(Color.Gray.copy(alpha = 0.3f), Color.DarkGray.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(99.dp)
            )
            .background(
                if (enabled) Brush.horizontalGradient(listOf(glowingColor.copy(alpha = 0.85f), glowingColor.copy(alpha = 0.65f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = LocalIndication.current) {
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (enabled) Color.White else Color.Gray.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun StadiumPitch(
    modifier: Modifier = Modifier,
    themeColor: String = "Neon Green"
) {
    val primaryStrokeColor = when (themeColor) {
        "Neon Blue" -> NeonBlue.copy(alpha = 0.45f)
        "Crimson Hot" -> CrimsonHot.copy(alpha = 0.45f)
        "Neon Green" -> NeonGreen.copy(alpha = 0.45f)
        else -> IndigoAccent.copy(alpha = 0.45f)
    }

    val backgroundGrad = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF090E1A),
            Color(0xFF05070A),
            Color(0xFF030406)
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundGrad)
    ) {
        val width = size.width
        val height = size.height
        val strokeWidthPx = 4f

        // Outer pitch boundary
        drawRect(
            color = primaryStrokeColor,
            topLeft = Offset(15f, 15f),
            size = Size(width - 30f, height - 30f),
            style = Stroke(width = strokeWidthPx)
        )

        // Halfway Line
        drawLine(
            color = primaryStrokeColor,
            start = Offset(15f, height / 2f),
            end = Offset(width - 15f, height / 2f),
            strokeWidth = strokeWidthPx
        )

        // Center Circle
        drawCircle(
            color = primaryStrokeColor,
            center = Offset(width / 2f, height / 2f),
            radius = width * 0.15f,
            style = Stroke(width = strokeWidthPx)
        )

        // Center Spot
        drawCircle(
            color = primaryStrokeColor,
            center = Offset(width / 2f, height / 2f),
            radius = 6f
        )

        // Penalty Box Top
        drawRect(
            color = primaryStrokeColor,
            topLeft = Offset(width * 0.2f, 15f),
            size = Size(width * 0.6f, height * 0.15f),
            style = Stroke(width = strokeWidthPx)
        )

        // Goal Box Top
        drawRect(
            color = primaryStrokeColor,
            topLeft = Offset(width * 0.35f, 15f),
            size = Size(width * 0.3f, height * 0.05f),
            style = Stroke(width = strokeWidthPx)
        )

        // Penalty Box Bottom
        drawRect(
            color = primaryStrokeColor,
            topLeft = Offset(width * 0.2f, height - (height * 0.15f) - 15f),
            size = Size(width * 0.6f, height * 0.15f),
            style = Stroke(width = strokeWidthPx)
        )

        // Goal Box Bottom
        drawRect(
            color = primaryStrokeColor,
            topLeft = Offset(width * 0.35f, height - (height * 0.05f) - 15f),
            size = Size(width * 0.3f, height * 0.05f),
            style = Stroke(width = strokeWidthPx)
        )

        // Corner Arcs
        val arcRadius = 24f
        // Top Left
        drawArc(
            color = primaryStrokeColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(15f, 15f),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = strokeWidthPx)
        )
        // Top Right
        drawArc(
            color = primaryStrokeColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - (arcRadius * 2) - 15f, 15f),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = strokeWidthPx)
        )
        // Bottom Left
        drawArc(
            color = primaryStrokeColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(15f, height - (arcRadius * 2) - 15f),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = strokeWidthPx)
        )
        // Bottom Right
        drawArc(
            color = primaryStrokeColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - (arcRadius * 2) - 15f, height - (arcRadius * 2) - 15f),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = strokeWidthPx)
        )
    }
}

@Composable
fun InteractiveCoin(
    isFlipping: Boolean,
    result: String?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coin_rotation")
    
    // Constant rotation when flipping is true
    val activeAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val finalAngle = when (result) {
        "Heads" -> 0f
        "Tails" -> 180f
        else -> 45f
    }

    val displayRotation = if (isFlipping) activeAngle else finalAngle

    Box(
        modifier = modifier
            .size(160.dp)
            .graphicsLayer {
                rotationY = displayRotation
                cameraDistance = 12f * density
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                )
            )
            .border(4.dp, Color(0xFFC7D2FE), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Coin Emblem",
                tint = Color(0xFFC7D2FE),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isFlipping) "XI" else (result ?: "TAP"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MISSING XI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E7FF)
                )
            )
        }
    }
}
