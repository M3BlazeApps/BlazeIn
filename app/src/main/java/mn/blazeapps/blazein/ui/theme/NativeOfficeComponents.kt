package mn.blazeapps.blazein.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
 * NativeOffice-style Dreamscape Background:
 * In Light Mode: Gentle pastel wash (lavender -> peach/rose -> pearl) with soft ambient glow orbs.
 * In Dark Mode: Deep midnight obsidian mesh with subtle violet & indigo orbs.
 */
@Composable
fun NativeOfficeBackground(
    isDark: Boolean = LocalIsDarkTheme.current,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgBrush = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                PastelBgTop,
                PastelBgMid,
                PastelBgBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DarkBgTop,
                DarkBgMid,
                DarkBgBottom
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Top-right ambient glowing orb
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (!isDark) {
                            listOf(
                                Color(0x35F472B6),
                                Color(0x10F472B6),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x358B5CF6),
                                Color(0x108B5CF6),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        // Top-left ambient glowing orb
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-30).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (!isDark) {
                            listOf(
                                Color(0x40818CF8),
                                Color(0x15818CF8),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x406366F1),
                                Color(0x126366F1),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        content()
    }
}

/**
 * Large rounded white container card (24dp corners) featured prominently in NativeOffice screens.
 */
@Composable
fun NativeOfficeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = LocalIsDarkTheme.current,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg = backgroundColor ?: if (isDark) CardDark else CardWhite
    val cardBorder = borderColor ?: if (isDark) BorderDark else BorderLight

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = NativeViolet.copy(alpha = 0.15f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = if (isDark) Color(0x40000000) else Color(0x186C5CE7),
                ambientColor = if (isDark) Color(0x30000000) else Color(0x0C6C5CE7)
            )
            .clip(shape)
            .background(cardBg, shape)
            .border(BorderStroke(1.dp, cardBorder), shape)
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 4 Category Squircle Card ("Make something new" section):
 * Clean card containing glowing icon squircle, bold title, and small uppercase sub-label.
 */
@Composable
fun CategorySquircleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgTint: Color,
    iconColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDarkTheme.current,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isDark) Color(0x30000000) else Color(0x106C5CE7),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) CardDark else CardWhite)
            .border(
                BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = iconColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Squircle icon container with colored glow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(15.dp),
                        spotColor = glowColor,
                        ambientColor = glowColor
                    )
                    .clip(RoundedCornerShape(15.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isDark) TextLightPrimary else TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp
                ),
                color = if (isDark) TextLightMuted else TextDarkMuted
            )
        }
    }
}

/**
 * Full-width capsule action button ("Open from your device" in NativeOffice):
 * In BlazeIn: "Browse from Telegram Channels" / "Open from Telegram"
 */
@Composable
fun NativeHeroActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(50),
                spotColor = Color(0x606C5CE7),
                ambientColor = Color(0x306C5CE7)
            )
            .clip(RoundedCornerShape(50))
            .background(NativeViolet)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = Color.White
            )
        }
    }
}

/**
 * Status Pill Badge ("🛡 YOUR QUIET HOURS" style)
 */
@Composable
fun StatusPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Shield,
    isDark: Boolean = LocalIsDarkTheme.current,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = NativeViolet.copy(alpha = 0.2f)),
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isDark) PillBgDark else PillBgLight)
            .border(
                BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                RoundedCornerShape(50)
            )
            .then(clickModifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NativeViolet,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = if (isDark) NativeVioletLight else NativeViolet
        )
    }
}

/**
 * Top App Bar with brand logo and right-hand capsule containing Theme Toggle (moon/sun) and Settings.
 */
@Composable
fun NativeTopBar(
    title: String = "BlazeIn",
    isDark: Boolean = LocalIsDarkTheme.current,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand logo + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(0x606C5CE7),
                        ambientColor = Color.Transparent
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                NativeViolet,
                                Color(0xFF00C0FF),
                                Color(0xFF00E676),
                                Color(0xFFFF9100)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "BlazeIn Brand",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.6).sp
                ),
                color = if (isDark) TextLightPrimary else TextDarkPrimary
            )
        }

        // Right Capsule: Moon / Sun Toggle + Settings Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isDark) CardDark else CardWhite)
                .border(
                    BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                    RoundedCornerShape(50)
                )
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(50),
                    spotColor = if (isDark) Color(0x30000000) else Color(0x106C5CE7)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.NightsStay,
                    contentDescription = "Toggle Theme",
                    tint = if (isDark) Color(0xFFFFB74D) else TextDarkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = if (isDark) TextLightPrimary else TextDarkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Floating Bottom Navigation Dock:
 * Floating pill bar (Home, Media, Tools) with separate circular (+) FAB on the right.
 */
@Composable
fun FloatingBottomDock(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onFabClick: () -> Unit,
    isDark: Boolean = LocalIsDarkTheme.current,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Floating Navigation Capsule
        Row(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(50),
                    spotColor = if (isDark) Color(0x50000000) else Color(0x206C5CE7),
                    ambientColor = if (isDark) Color(0x30000000) else Color(0x106C5CE7)
                )
                .clip(RoundedCornerShape(50))
                .background(if (isDark) CardDark else CardWhite)
                .border(
                    BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockItem(
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                isSelected = selectedTab == "home",
                isDark = isDark,
                onClick = { onSelectTab("home") }
            )

            DockItem(
                label = "Files",
                selectedIcon = Icons.Filled.Folder,
                unselectedIcon = Icons.Outlined.Folder,
                isSelected = selectedTab == "files",
                isDark = isDark,
                onClick = { onSelectTab("files") }
            )

            DockItem(
                label = "Tools",
                selectedIcon = Icons.Filled.Widgets,
                unselectedIcon = Icons.Outlined.Widgets,
                isSelected = selectedTab == "tools",
                isDark = isDark,
                onClick = { onSelectTab("tools") }
            )
        }

        // Circular (+) Floating Action Button
        FloatingActionButton(
            onClick = onFabClick,
            shape = CircleShape,
            containerColor = NativeViolet,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            modifier = Modifier.size(58.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Actions",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val pillBg = if (isSelected) {
        if (isDark) PillBgDark else PillBgLight
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        if (isDark) NativeVioletLight else NativeViolet
    } else {
        if (isDark) TextLightMuted else TextDarkMuted
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NativeViolet.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        if (isSelected) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}
