# Obsidian Glass & Electric Dual-Accent Design System Guide

A complete guide for porting the **Figma Make / BlazeIn UI theme** to any Android (Jetpack Compose) or modern application.

---

## 1. Design Philosophy & Aesthetic Core

The design system combines **Deep Obsidian Canvas**, **Multi-Point Ambient Radial Mesh Glow**, and a **High-Energy Dual-Accent Palette**:

- **Obsidian Dark Canvas**: Grounded in deep charcoal/midnight tones (`#080B1A` and `#0D1230`) rather than pure `#000000`, creating depth and reducing eye strain.
- **Ambient Radial Mesh Glow**: A 4-point glowing light mesh beneath the UI that radiates subtle hues of Indigo, Violet, Azure, and Warm Orange.
- **Dual-Accent Color Energy**:
  - **Electric Blue-Violet (`#6366F1`)**: Symbolizes intelligence, tech, navigation, active tabs, and secondary accents.
  - **Electric Orange (`#F97316`)**: Reserved for primary calls-to-action (CTAs), playback controls, download triggers, and alerts.
- **Frosted Glass Layering**: UI cards and sheets float above the mesh background with ultra-thin hairline borders (`0.75dp`), high translucency (`4% - 6%` white fill), and subtle corner radii (`16dp`).
- **Squircle Badging**: 10–12dp rounded squircle icon containers with colored glow drop shadows.

---

## 2. Color Palette Tokens

```kotlin
// ==========================================
// BACKGROUNDS & CANVAS
// ==========================================
val BgDeep            = Color(0xFF080B1A) // Deepest canvas base
val BgMid             = Color(0xFF0D1230) // Gradient secondary base
val BgCard            = Color(0x09FFFFFF) // ~3.5% white overlay for cards
val BgCardElevated    = Color(0x12FFFFFF) // ~7% white overlay for popups/menus
val BgInput           = Color(0x0DFFFFFF) // Form fields / search bars

// ==========================================
// AMBIENT MESH GLOW COLORS
// ==========================================
val BgMeshIndigo      = Color(0xFF1E1B4B) // Top-left glow
val BgMeshOrange      = Color(0xFF451A03) // Top-right glow
val BgMeshViolet      = Color(0xFF2E1065) // Bottom-right glow
val BgMeshAmber       = Color(0xFF331400) // Bottom-left glow

// ==========================================
// PRIMARY ACCENTS: ELECTRIC BLUE-VIOLET
// ==========================================
val ColorBlueViolet      = Color(0xFF6366F1) // Indigo 500 (Primary Brand)
val ColorBlueVioletLight = Color(0xFF818CF8) // Indigo 400 (Hover/Tint)
val ColorBlueVioletDark  = Color(0xFF4338CA) // Indigo 700 (Gradients)

// ==========================================
// PRIMARY ACTION: ELECTRIC ORANGE
// ==========================================
val ColorOrange          = Color(0xFFF97316) // Orange 500 (CTA & Play)
val ColorOrangeLight     = Color(0xFFFB923C) // Orange 400 (Gradients/Highlights)
val ColorOrangeDark      = Color(0xFFEA580C) // Orange 600 (Pressed/Shadow)

// ==========================================
// SEMANTIC & STATUS ACCENTS
// ==========================================
val ColorPurple          = Color(0xFF8B5CF6) // Accent / Badges
val ColorCyan            = Color(0xFF06B6D4) // Informational
val ColorGreen           = Color(0xFF10B981) // Online / Success
val ColorRose            = Color(0xFFF43F5E) // Destructive / Danger / Delete
val ColorAmber           = Color(0xFFF59E0B) // Warning / Attention

// ==========================================
// FROSTED GLASS BORDERS & FILLS
// ==========================================
val GlassBg              = Color(0x0AFFFFFF) // 4% White fill
val GlassBorder          = Color(0x14FFFFFF) // 8% White hairline border
val GlassBorderLight     = Color(0x24FFFFFF) // 14% White border (Focused/Hover)

// ==========================================
// TEXT HIERARCHY
// ==========================================
val TextPrimary          = Color(0xFFF8FAFC) // Slate 50 (Headings)
val TextSecondary        = Color(0xFF94A3B8) // Slate 400 (Subtitles/Meta)
val TextMuted            = Color(0xFF64748B) // Slate 500 (Footers/Hints)
```

---

## 3. Core Jetpack Compose Components

### A. The 4-Point Radial Mesh Canvas (`GlassBackground`)
Wrap your entire screen or Scaffold content inside this composable. It creates an organic, illuminated backdrop without using heavy bitmap images.

```kotlin
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeep)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Top-Left: Indigo ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BgMeshIndigo.copy(alpha = 0.50f), Color.Transparent),
                        center = Offset(canvasWidth * 0.15f, canvasHeight * 0.10f),
                        radius = canvasWidth * 0.85f
                    )
                )

                // Top-Right: Electric Orange subtle glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BgMeshOrange.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(canvasWidth * 0.85f, canvasHeight * 0.20f),
                        radius = canvasWidth * 0.70f
                    )
                )

                // Bottom-Right: Violet ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BgMeshViolet.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(canvasWidth * 0.85f, canvasHeight * 0.80f),
                        radius = canvasWidth * 0.80f
                    )
                )

                // Bottom-Left: Warm Amber glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BgMeshAmber.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(canvasWidth * 0.10f, canvasHeight * 0.90f),
                        radius = canvasWidth * 0.65f
                    )
                )
            }
    ) {
        content()
    }
}
```

---

### B. Frosted Glass Cards (`GlassCard`)
Use for list items, settings sections, stat cards, and dialogue boxes.

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 0.75.dp,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = GlassBg,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                border = BorderStroke(borderWidth, borderColor),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
```

---

### C. Squircle Glowing Icon Box (`SquircleIconBox`)
Used in navigation headers, card leading icons, and category badges.

```kotlin
@Composable
fun SquircleIconBox(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    gradientColors: List<Color> = listOf(ColorBlueViolet, ColorPurple),
    glowColor: Color = ColorBlueViolet.copy(alpha = 0.4f),
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = glowColor,
                ambientColor = glowColor.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
```

---

### D. Pill Status Badges (`PillBadge`)
Used for tags, online indicators, category chips, and file formats.

```kotlin
@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    badgeColor: Color = ColorBlueViolet,
    textColor: Color = TextSecondary
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.12f))
            .border(BorderStroke(0.75.dp, badgeColor.copy(alpha = 0.25f)), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.3.sp
        )
    }
}
```

---

### E. Electric Orange Primary Action Button (`ElectricOrangeButton`)
The signature call-to-action button with a warm radiant drop glow.

```kotlin
@Composable
fun ElectricOrangeButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x66F97316),
                ambientColor = Color(0x33F97316)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(ColorOrange, ColorOrangeLight)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
```

---

## 4. UI Layout Recipes & Patterns

### 1. App Bar Header Pattern
- **Left**: 44dp Squircle icon with gradient brand symbol (e.g. Bolt / Logo).
- **Title**: Bold `TextPrimary` 20sp + electric uppercase subtitle in `ColorBlueVioletLight` 10sp with letter spacing `1.5sp`.
- **Right Actions**: 36dp squircle glass buttons with badges.

### 2. Search / Input Fields
- Avoid default solid text fields.
- Use a `Box` or `TextField` with `RoundedCornerShape(24.dp)` (pill) or `14.dp`.
- Background: `BgInput` (`rgba(255,255,255,0.05)`).
- Border: `GlassBorder` (`rgba(255,255,255,0.08)`).
- Prefix icons tinted in `ColorBlueVioletLight`.

### 3. Media / Item Cards
- Aspect ratio thumbnail with 10–12dp rounded corners and duration pill in bottom-right.
- Two-line title + metadata row with `TextSecondary` (12sp).
- Trailing action row:
  - 36dp circular `btn-orange` Play/Run button.
  - 36dp glass action button (Download, More, or Delete).

### 4. Settings & Form Cards
- Group related items inside 16dp `GlassCard` containers.
- Each section begins with a leading row containing a 28dp `SquircleIconBox` + Section Title.
- Divider lines use `GlassBorder` (`0.75.dp` stroke).
- Destructive items (e.g., Log Out, Delete Cache) use `ColorRose.copy(alpha = 0.08f)` background and `ColorRose` text/icons.

---

## 5. Web & Tailwind CSS Adaptations

If porting to React, Vue, or Tailwind CSS:

```css
/* Tailwind Configuration Tokens */
:root {
  --bg-deep: #080B1A;
  --bg-mid: #0D1230;
  --color-violet: #6366F1;
  --color-orange: #F97316;
  --glass-bg: rgba(255, 255, 255, 0.04);
  --glass-border: rgba(255, 255, 255, 0.08);
}

/* 4-Point Mesh Canvas */
.mesh-canvas {
  background-color: var(--bg-deep);
  background-image: 
    radial-gradient(at 15% 10%, rgba(30, 27, 75, 0.55) 0px, transparent 60%),
    radial-gradient(at 85% 20%, rgba(69, 26, 3, 0.35) 0px, transparent 50%),
    radial-gradient(at 85% 80%, rgba(46, 16, 101, 0.45) 0px, transparent 60%),
    radial-gradient(at 10% 90%, rgba(51, 20, 0, 0.30) 0px, transparent 50%);
  min-height: 100vh;
}

/* Frosted Glass Card */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
}

/* Electric Orange Button */
.btn-orange {
  background: linear-gradient(135deg, #F97316 0%, #FB923C 100%);
  box-shadow: 0 4px 14px rgba(249, 115, 22, 0.40);
  border-radius: 12px;
  color: #FFFFFF;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn-orange:hover {
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.60);
  transform: translateY(-1px);
}
```

---

## 6. Checklist for New Apps

1. [ ] Add color tokens to your theme (`BgDeep`, `ColorBlueViolet`, `ColorOrange`, etc.).
2. [ ] Wrap your screen roots in `GlassBackground`.
3. [ ] Replace default cards with `GlassCard` (16dp radius, hairline border).
4. [ ] Standardize icons in headers and list items using `SquircleIconBox` (12dp radius, gradient + colored glow).
5. [ ] Highlight the main action on every screen with the `ColorOrange` / `ColorOrangeLight` gradient and orange glow shadow.
6. [ ] Tag metadata (categories, statuses, years, genres) using `PillBadge` chips.
7. [ ] Maintain typography contrast: `TextPrimary` (`#F8FAFC`) for titles, `TextSecondary` (`#94A3B8`) for body, and `TextMuted` (`#64748B`) for footnotes.
