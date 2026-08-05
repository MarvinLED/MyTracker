package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun AnalogClockIcon(
    hour: Int,
    minute: Int = 0,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(24.dp),
        ) {
            drawClockFace(color, hour, minute)
        }
    }
}

private fun DrawScope.drawClockFace(color: Color, hour: Int, minute: Int) {
    val radius = size.minDimension / 2f
    val center = Offset(radius, radius)

    // Draw circle border
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2f),
    )

    // Draw center dot
    drawCircle(
        color = color,
        radius = 2.5f,
        center = center,
    )

    // Convert hour to 12-hour format and add minutes
    val hourValue = hour % 12
    val totalMinutes = hourValue * 60 + minute

    // Hour hand (shorter, thicker)
    val hourAngle = (totalMinutes / 720f) * 2 * PI - PI / 2
    val hourLength = radius * 0.5f
    drawLine(
        color = color,
        start = center,
        end = Offset(
            center.x + (cos(hourAngle) * hourLength).toFloat(),
            center.y + (sin(hourAngle) * hourLength).toFloat(),
        ),
        strokeWidth = 3f,
    )

    // Minute hand (longer, thinner)
    val minuteAngle = (minute / 60f) * 2 * PI - PI / 2
    val minuteLength = radius * 0.7f
    drawLine(
        color = color,
        start = center,
        end = Offset(
            center.x + (cos(minuteAngle) * minuteLength).toFloat(),
            center.y + (sin(minuteAngle) * minuteLength).toFloat(),
        ),
        strokeWidth = 2f,
    )
}
