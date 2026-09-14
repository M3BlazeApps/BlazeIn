package mn.blazeapps.blazein.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ambient background container that produces subtle depth behind frosted glass elements.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleCanvasDark)
    ) {
        // Ambient subtle glow top-left / center (indigo/violet depth)
        Box(
            modifier = Modifier
                .fillMaxWidth(1.2f)
                .height(420.dp)
                .offset(x = (-60).dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x353B2D71),
                            Color(0x18201A4A),
                            Color.Transparent
                        )
                    )
                )
        )

        // Ambient subtle glow top-right (deep oceanic azure/cyan sheen)
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x280A588C),
                            Color(0x100A3860),
                            Color.Transparent
                        )
                    )
                )
        )

        content()
    }
}

/**
 * Modifier that applies Apple liquid glass styling:
 * - Frosted translucent background gradient
 * - Rounded corner clip
 * - Specular border reflection gradient (top-to-bottom rim light)
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x1F222F46),
    borderColor: Color = Color.White,
    borderAlphaTop: Float = 0.35f,
    borderAlphaBottom: Float = 0.08f,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = backgroundColor.alpha.coerceAtLeast(0.18f)),
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.65f).coerceAtLeast(0.08f))
            )
        ),
        shape = shape
    )
    .border(
        border = BorderStroke(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderAlphaTop),
                    borderColor.copy(alpha = borderAlphaBottom)
                )
            )
        ),
        shape = shape
    )

/**
 * Reusable Glass Card with Apple curvature, frosted sheen, and optional click response.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x1E24334C),
    borderAlphaTop: Float = 0.32f,
    borderAlphaBottom: Float = 0.08f,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White.copy(alpha = 0.2f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, spotColor = Color(0x40000000), ambientColor = Color(0x20000000))
            .glassEffect(
                shape = shape,
                backgroundColor = backgroundColor,
                borderAlphaTop = borderAlphaTop,
                borderAlphaBottom = borderAlphaBottom
            )
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Circular Frosted Glass Icon Button (for action bars, player controls, etc.)
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 36.dp,
    containerColor: Color = Color(0x28FFFFFF),
    borderAlphaTop: Float = 0.40f,
    contentColor: Color = AppleTextPrimary,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .glassEffect(
                shape = CircleShape,
                backgroundColor = containerColor,
                borderAlphaTop = borderAlphaTop,
                borderAlphaBottom = 0.12f
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Pill-shaped Glass Badge/Chip (for duration, tags, counts)
 */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = Color(0x4D000000),
    contentColor: Color = AppleTextPrimary
) {
    Row(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = backgroundColor,
                borderAlphaTop = 0.35f,
                borderAlphaBottom = 0.10f
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Apple Frosted Glass Top App Bar with bottom specular hairline
 */
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    leadingBrandIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCC0D1017),
                        Color(0xB3101522)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(10.dp))
            } else if (leadingBrandIcon != null) {
                leadingBrandIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                title()
                if (subtitle != null) {
                    subtitle()
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions
                )
            }
        }

        // Specular hairline separator at the bottom of the glass top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x10FFFFFF),
                            Color(0x30FFFFFF),
                            Color(0x10FFFFFF)
                        )
                    )
                )
        )
    }
}
