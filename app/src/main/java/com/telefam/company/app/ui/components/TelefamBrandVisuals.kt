package com.telefam.company.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.TelefamRedBright
import com.example.ui.theme.TelefamRedDark
import com.example.ui.theme.TelefamRedDeep

/**
 * Exact recreation of the Telefam circular red paper-plane logo from Reference Image 1,
 * including the diagonal long-shadow inside the red circle and the folded paper airplane.
 */
@Composable
fun TelefamPaperPlaneLogo(
    size: Dp = 88.dp,
    animateFloatPulse: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "telefam_plane_float")
    val planeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateFloatPulse) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plane_offset"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val radius = w / 2f
            val center = Offset(radius, radius)

            val circleClip = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            }

            clipPath(circleClip) {
                // 1. Vibrant Red Circle Base Gradient
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(TelefamRedBright, TelefamRed, TelefamRedDark),
                        start = Offset(w * 0.15f, h * 0.1f),
                        end = Offset(w * 0.88f, h * 0.92f)
                    ),
                    radius = radius,
                    center = center
                )

                val shiftX = (planeOffset - 0.5f) * w * 0.018f
                val shiftY = -(planeOffset - 0.5f) * h * 0.018f

                // 2. Diagonal Long Shadow Cast Toward Bottom-Right (as in Reference Image 1)
                val shadowPath = Path().apply {
                    moveTo(w * 0.72f + shiftX, h * 0.24f + shiftY)
                    lineTo(w * 1.05f, h * 0.58f)
                    lineTo(w * 1.05f, h * 1.05f)
                    lineTo(w * 0.52f, h * 1.05f)
                    lineTo(w * 0.44f + shiftX, h * 0.75f + shiftY)
                    lineTo(w * 0.22f + shiftX, h * 0.52f + shiftY)
                    close()
                }
                drawPath(
                    path = shadowPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TelefamRedDeep.copy(alpha = 0.58f),
                            TelefamRedDeep.copy(alpha = 0.15f)
                        ),
                        start = Offset(w * 0.45f, h * 0.45f),
                        end = Offset(w, h)
                    )
                )

                // 3. White Paper Airplane Wing & Folded Tail
                // Left/Upper Wing + Right/Lower Wing with center fold valley
                val planeMainPath = Path().apply {
                    // Nose tip (top-right)
                    moveTo(w * 0.74f + shiftX, h * 0.24f + shiftY)
                    // Left wing tip (middle-left)
                    lineTo(w * 0.20f + shiftX, h * 0.49f + shiftY)
                    // Left wing inner fold
                    lineTo(w * 0.43f + shiftX, h * 0.58f + shiftY)
                    // Center tail notch
                    lineTo(w * 0.44f + shiftX, h * 0.76f + shiftY)
                    // Inner tail fold
                    lineTo(w * 0.52f + shiftX, h * 0.65f + shiftY)
                    // Right wing bottom tip
                    lineTo(w * 0.61f + shiftX, h * 0.76f + shiftY)
                    close()
                }
                drawPath(
                    path = planeMainPath,
                    color = Color.White,
                    style = Fill
                )
                drawPath(
                    path = planeMainPath,
                    color = Color.White,
                    style = Stroke(
                        width = w * 0.035f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Subtle inner crease line from nose to fold
                val creasePath = Path().apply {
                    moveTo(w * 0.72f + shiftX, h * 0.26f + shiftY)
                    lineTo(w * 0.44f + shiftX, h * 0.59f + shiftY)
                    lineTo(w * 0.45f + shiftX, h * 0.75f + shiftY)
                }
                drawPath(
                    path = creasePath,
                    color = Color(0xFFE8D5D7),
                    style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Multi-color Google 'G' logo matching Reference Image 1.
 */
@Composable
fun GoogleColorLogoIcon(
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.20f
        val inset = strokeWidth / 2f
        val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 215f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue right arc + horizontal bar
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(this.size.width * 0.50f, this.size.height * 0.41f),
            size = Size(this.size.width * 0.45f, strokeWidth * 0.92f)
        )
    }
}

/**
 * Crisp vector Kenya Flag (matching Reference Image 2 phone input flag)
 */
@Composable
fun KenyaFlagVectorIcon(
    width: Dp = 28.dp,
    height: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        // Base white
        drawRoundRect(color = Color.White, size = Size(w, h), cornerRadius = r)
        // Top black stripe
        drawRect(color = Color(0xFF111111), topLeft = Offset(0f, 0f), size = Size(w, h * 0.28f))
        // Middle red stripe
        drawRect(color = Color(0xFFBB0000), topLeft = Offset(0f, h * 0.35f), size = Size(w, h * 0.30f))
        // Bottom green stripe
        drawRect(color = Color(0xFF006600), topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f))

        // Center shield & crossed white spears
        drawLine(
            color = Color.White,
            start = Offset(w * 0.38f, h * 0.14f),
            end = Offset(w * 0.62f, h * 0.86f),
            strokeWidth = 1.3.dp.toPx()
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.62f, h * 0.14f),
            end = Offset(w * 0.38f, h * 0.86f),
            strokeWidth = 1.3.dp.toPx()
        )
        drawOval(
            color = Color(0xFFBB0000),
            topLeft = Offset(w * 0.40f, h * 0.22f),
            size = Size(w * 0.20f, h * 0.56f)
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.40f, h * 0.22f),
            size = Size(w * 0.20f, h * 0.56f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
