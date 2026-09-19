package com.example.agrinext.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Speedometer(
    currentValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    // We will use a gradient brush instead of a single color
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 20.dp,
    label: String = "Score"
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(currentValue) {
        animatedValue.animateTo(
            targetValue = currentValue,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val sweepAngle = 240f
    val startAngle = 150f

    // Define Gradient Colors (Red -> Yellow -> Green)
    val gradientColors = listOf(
        Color(0xFFFF5252), // Red
        Color(0xFFFFD740), // Yellow
        Color(0xFF69F0AE), // Green
        Color(0xFF00E676)  // Bright Green
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
            val componentSize = size.minDimension
            val strokeWidthPx = strokeWidth.toPx()

            val topLeftOffset = Offset(
                (size.width - componentSize) / 2,
                (size.height - componentSize) / 2
            )
            val arcSize = Size(componentSize, componentSize)

            // 1. Background Ring
            drawArc(
                color = backgroundColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = topLeftOffset
            )

            // 2. Gradient Progress Ring
            val progressSwipe = (animatedValue.value / maxValue) * sweepAngle

            // Use a SweepGradient centered on the arc
            val gradientBrush = Brush.sweepGradient(
                colors = gradientColors,
                center = center
            )

            drawArc(
                brush = gradientBrush, // Use brush instead of color
                startAngle = startAngle,
                sweepAngle = progressSwipe,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = topLeftOffset
            )
        }

        // 3. Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animatedValue.value.toInt()}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}