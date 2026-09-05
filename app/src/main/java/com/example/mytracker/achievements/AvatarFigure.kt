package com.example.mytracker.achievements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The level at which an attribute counts as maxed out for drawing. Beyond it the shape stops growing. */
private const val VISUAL_MAX_LEVEL = 10

/** Body proportions, all as a share of the figure's height. */
private const val HEAD_CENTER_Y = 0.16f
private const val SHOULDER_Y = 0.30f
private const val WAIST_Y = 0.52f
private const val HIP_Y = 0.60f
private const val FOOT_Y = 0.96f

/**
 * The figure itself: a body drawn from five numbers.
 *
 * Drawn rather than assembled from images, for the reason that decides it — an image set would need
 * one drawing per level per attribute, and every combination in between would be missing. As
 * geometry, "Schultern eine Stufe breiter" is a slightly larger number, so the body moves smoothly
 * and the app stays free of assets and entirely offline.
 *
 * [record] is drawn behind [current] as an outline: the best form ever held, standing behind the one
 * held now. That is what makes the decay bearable — a pause softens the figure, but it can never
 * take away the silhouette that was earned.
 */
@Composable
fun AvatarFigure(
    current: List<AttributeLevel>,
    modifier: Modifier = Modifier,
    items: Set<AvatarItem> = emptySet(),
    height: Int = 260,
) {
    val levels = current.associate { it.attribute to it.level }
    val records = current.associate { it.attribute to it.record }
    val bodyColor = MaterialTheme.colorScheme.primary
    val vitality = MaterialTheme.colorScheme.tertiary
    val gear = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val description = figureDescription(current, items)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val shape = FigureShape.of(levels)
            val bestShape = FigureShape.of(records)

            // The record first and underneath, so the body always reads as the thing in front.
            if (bestShape.isLargerThan(shape)) {
                drawFigure(bestShape, color = outline.copy(alpha = 0.45f), filled = false)
            }
            // Behind the body but in front of the silhouette, so it hangs off the shoulders.
            if (AvatarItem.UMHANG in items) drawCape(shape, gear)
            // Vitality tints the body rather than changing its shape — it is how someone looks,
            // not how they are built.
            val vitalityLevel = levels[AvatarAttribute.VITALITAET] ?: 0
            val skin = lerp(bodyColor, vitality, (vitalityLevel.toFloat() / VISUAL_MAX_LEVEL).coerceIn(0f, 1f))
            drawFigure(shape, color = skin, filled = true)

            drawGear(shape, items, gear)

            val clarity = levels[AvatarAttribute.KLARHEIT] ?: 0
            if (clarity >= 6) {
                // A halo from Stufe 6: the one flourish that is pure reward, and it only ever
                // appears on top of a head that is already well rested.
                drawCircle(
                    color = vitality.copy(alpha = 0.25f),
                    radius = shape.headRadius * size.height * 1.5f,
                    center = Offset(size.width / 2f, HEAD_CENTER_Y * size.height),
                )
            }
        }
    }
}

/**
 * The body's measurements, each a share of the drawing height. Kept apart from the drawing so the
 * mapping from levels to proportions can be read — and changed — in one place.
 */
private data class FigureShape(
    val headRadius: Float,
    val shoulderHalfWidth: Float,
    val waistHalfWidth: Float,
    val armThickness: Float,
    val legThickness: Float,
    /** How upright the figure stands: 0 slouched, 1 straight. */
    val posture: Float,
) {
    fun isLargerThan(other: FigureShape): Boolean =
        shoulderHalfWidth > other.shoulderHalfWidth + 0.002f ||
            waistHalfWidth < other.waistHalfWidth - 0.002f ||
            legThickness > other.legThickness + 0.002f

    companion object {
        fun of(levels: Map<AvatarAttribute, Int>): FigureShape {
            fun share(attribute: AvatarAttribute): Float =
                ((levels[attribute] ?: 0).toFloat() / VISUAL_MAX_LEVEL).coerceIn(0f, 1f)

            val strength = share(AvatarAttribute.KRAFT)
            val form = share(AvatarAttribute.FORM)
            val stamina = share(AvatarAttribute.AUSDAUER)
            val clarity = share(AvatarAttribute.KLARHEIT)

            return FigureShape(
                // The head grows a little with Klarheit, which is the playful part — but only a
                // little, or the figure stops reading as a person.
                headRadius = 0.055f + 0.018f * clarity,
                shoulderHalfWidth = 0.075f + 0.075f * strength,
                // Form works the other way round: the waist comes *in*. A floor keeps it a body
                // rather than an hourglass, however long the run of met goals.
                waistHalfWidth = 0.085f - 0.040f * form,
                armThickness = 0.020f + 0.024f * strength,
                legThickness = 0.028f + 0.022f * stamina,
                posture = stamina,
            )
        }
    }
}

private fun DrawScope.drawFigure(shape: FigureShape, color: Color, filled: Boolean) {
    val w = size.width
    val h = size.height
    val midX = w / 2f
    // A slouch leans the whole upper body forward a touch; standing up straight takes it back.
    val lean = (1f - shape.posture) * 0.02f * h

    val headRadius = shape.headRadius * h
    val shoulderHalf = shape.shoulderHalfWidth * h
    val waistHalf = shape.waistHalfWidth * h
    val stroke = Stroke(width = h * 0.012f)

    fun draw(path: Path) {
        if (filled) drawPath(path, color = color) else drawPath(path, color = color, style = stroke)
    }

    if (filled) {
        drawCircle(color, radius = headRadius, center = Offset(midX + lean, HEAD_CENTER_Y * h))
    } else {
        drawCircle(color, radius = headRadius, center = Offset(midX + lean, HEAD_CENTER_Y * h), style = stroke)
    }

    // Torso: shoulders out, waist in, hips a little wider than the waist again.
    val torso = Path().apply {
        moveTo(midX - shoulderHalf + lean, SHOULDER_Y * h)
        lineTo(midX + shoulderHalf + lean, SHOULDER_Y * h)
        lineTo(midX + waistHalf, WAIST_Y * h)
        lineTo(midX + waistHalf * 1.15f, HIP_Y * h)
        lineTo(midX - waistHalf * 1.15f, HIP_Y * h)
        lineTo(midX - waistHalf, WAIST_Y * h)
        close()
    }
    draw(torso)

    // Neck, so the head is not floating above the shoulders.
    val neck = Path().apply {
        moveTo(midX - headRadius * 0.35f + lean, HEAD_CENTER_Y * h)
        lineTo(midX + headRadius * 0.35f + lean, HEAD_CENTER_Y * h)
        lineTo(midX + headRadius * 0.35f, SHOULDER_Y * h)
        lineTo(midX - headRadius * 0.35f, SHOULDER_Y * h)
        close()
    }
    draw(neck)

    val armThickness = shape.armThickness * h
    listOf(-1f, 1f).forEach { side ->
        val fromX = midX + side * (shoulderHalf - armThickness * 0.2f) + lean
        val toX = midX + side * (waistHalf + armThickness * 1.4f)
        val arm = Path().apply {
            moveTo(fromX, SHOULDER_Y * h)
            lineTo(fromX + side * armThickness, SHOULDER_Y * h)
            lineTo(toX + side * armThickness, (WAIST_Y + 0.06f) * h)
            lineTo(toX, (WAIST_Y + 0.06f) * h)
            close()
        }
        draw(arm)
    }

    val legThickness = shape.legThickness * h
    listOf(-1f, 1f).forEach { side ->
        val hipX = midX + side * waistHalf * 0.55f
        val leg = Path().apply {
            moveTo(hipX - legThickness / 2f, HIP_Y * h)
            lineTo(hipX + legThickness / 2f, HIP_Y * h)
            lineTo(hipX + legThickness / 2f, FOOT_Y * h)
            lineTo(hipX - legThickness / 2f, FOOT_Y * h)
            close()
        }
        draw(leg)
    }

    if (filled) {
        // Ground, so the figure stands on something instead of hanging in the frame.
        drawRect(
            color = color.copy(alpha = 0.25f),
            topLeft = Offset(midX - shoulderHalf * 1.6f, FOOT_Y * h),
            size = Size(shoulderHalf * 3.2f, h * 0.012f),
        )
    }
}

/** The cape, drawn behind the body so it reads as hanging off the shoulders. */
private fun DrawScope.drawCape(shape: FigureShape, color: Color) {
    val h = size.height
    val midX = size.width / 2f
    val shoulderHalf = shape.shoulderHalfWidth * h
    val cape = Path().apply {
        moveTo(midX - shoulderHalf, SHOULDER_Y * h)
        lineTo(midX + shoulderHalf, SHOULDER_Y * h)
        lineTo(midX + shoulderHalf * 1.7f, (HIP_Y + 0.14f) * h)
        lineTo(midX - shoulderHalf * 1.7f, (HIP_Y + 0.14f) * h)
        close()
    }
    drawPath(cape, color = color.copy(alpha = 0.55f))
}

/**
 * Everything worn on top of the body. Each piece is a couple of shapes rather than an image, for the
 * same reason the body is: it has to sit correctly on a figure whose proportions keep moving.
 *
 * Only one head piece is drawn — the rarest earned — since a crown over a cap over a headband is a
 * pile, not an outfit.
 */
private fun DrawScope.drawGear(shape: FigureShape, items: Set<AvatarItem>, color: Color) {
    val h = size.height
    val w = size.width
    val midX = w / 2f
    val headRadius = shape.headRadius * h
    val headY = HEAD_CENTER_Y * h
    val shoulderHalf = shape.shoulderHalfWidth * h
    val waistHalf = shape.waistHalfWidth * h

    when {
        AvatarItem.KRONE in items -> {
            val crown = Path().apply {
                val top = headY - headRadius * 1.75f
                val base = headY - headRadius * 0.85f
                moveTo(midX - headRadius, base)
                lineTo(midX - headRadius, top)
                lineTo(midX - headRadius * 0.5f, base - headRadius * 0.35f)
                lineTo(midX, top)
                lineTo(midX + headRadius * 0.5f, base - headRadius * 0.35f)
                lineTo(midX + headRadius, top)
                lineTo(midX + headRadius, base)
                close()
            }
            drawPath(crown, color = color)
        }
        AvatarItem.MUETZE in items -> drawRect(
            color = color,
            topLeft = Offset(midX - headRadius, headY - headRadius * 1.25f),
            size = Size(headRadius * 2f, headRadius * 0.85f),
        )
        AvatarItem.STIRNBAND in items -> drawRect(
            color = color,
            topLeft = Offset(midX - headRadius, headY - headRadius * 0.55f),
            size = Size(headRadius * 2f, headRadius * 0.35f),
        )
    }

    if (AvatarItem.KETTE in items) {
        drawCircle(
            color = color,
            radius = headRadius * 0.22f,
            center = Offset(midX, SHOULDER_Y * h + headRadius * 0.5f),
        )
    }

    if (AvatarItem.GUERTEL in items) {
        drawRect(
            color = color,
            topLeft = Offset(midX - waistHalf * 1.1f, WAIST_Y * h),
            size = Size(waistHalf * 2.2f, h * 0.018f),
        )
    }

    if (AvatarItem.ARMBAND in items) {
        val armThickness = shape.armThickness * h
        listOf(-1f, 1f).forEach { side ->
            val x = midX + side * (waistHalf + armThickness * 1.4f)
            drawRect(
                color = color,
                topLeft = Offset(minOf(x, x + side * armThickness), (WAIST_Y + 0.03f) * h),
                size = Size(armThickness, h * 0.015f),
            )
        }
    }

    if (AvatarItem.LAUFSCHUHE in items) {
        val legThickness = shape.legThickness * h
        listOf(-1f, 1f).forEach { side ->
            val hipX = midX + side * waistHalf * 0.55f
            drawRect(
                color = color,
                topLeft = Offset(hipX - legThickness * 0.8f, FOOT_Y * h - h * 0.018f),
                size = Size(legThickness * 1.6f, h * 0.018f),
            )
        }
    }
}

/**
 * The figure in words. A drawing is exactly the kind of thing a screen reader cannot pass on, and
 * the levels underneath it are read one at a time — this is the sentence that says what it all adds
 * up to.
 */
private fun figureDescription(levels: List<AttributeLevel>, items: Set<AvatarItem>): String {
    if (levels.isEmpty()) return "Figur ohne Stufen"
    val parts = levels.joinToString(", ") { level ->
        buildString {
            append("${level.attribute.label} Stufe ${level.level}")
            if (level.record > level.level) append(" von bisher ${level.record}")
        }
    }
    val worn = if (items.isEmpty()) "" else ". Trägt ${items.joinToString(", ") { it.label }}"
    return "Deine Figur: $parts$worn"
}
