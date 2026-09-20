package mn.blazeapps.blazein.ui.theme

import androidx.compose.ui.graphics.Color

// ── Figma Make Redesign Palette ─────────────────────────────────────────────

// Background Obsidian & Navy
val BgDeep = Color(0xFF080B1A)
val BgMid = Color(0xFF0D1230)
val BgMeshDark = Color(0xFF0A0D1F)
val BgMeshIndigo = Color(0xFF1A0A3A)
val BgMeshOrange = Color(0xFF2A0E1A)
val BgMeshAzure = Color(0xFF0F1A3D)

// Primary: Blue-Violet / Indigo
val ColorBlueViolet = Color(0xFF6366F1)
val ColorBlueVioletLight = Color(0xFF818CF8)
val ColorBlueVioletDim = Color(0x2E6366F1)       // rgba(99, 102, 241, 0.18)
val ColorBlueVioletSubtle = Color(0xFFA5B4FC)
val ColorPurple = Color(0xFF8B5CF6)
val ColorPurpleDim = Color(0x2E8B5CF6)

// Accent: Electric Orange / Amber
val ColorOrange = Color(0xFFF97316)
val ColorOrangeLight = Color(0xFFFB923C)
val ColorOrangeDim = Color(0x2EF97316)           // rgba(249, 115, 22, 0.18)

// Status Colors
val ColorGreen = Color(0xFF22C55E)
val ColorGreenLight = Color(0xFF86EFAC)
val ColorGreenDim = Color(0x2E22C55E)           // rgba(34, 197, 94, 0.18)

val ColorRed = Color(0xFFEF4444)
val ColorRedLight = Color(0xFFFCA5A5)
val ColorRedDim = Color(0x38EF4444)             // rgba(239, 68, 68, 0.22)

// Glass Surfaces & Borders
val GlassBg = Color(0x1AFFFFFF)                  // rgba(255, 255, 255, 0.10)
val GlassBgStrong = Color(0x24FFFFFF)            // rgba(255, 255, 255, 0.14)
val GlassBorder = Color(0x2EFFFFFF)              // rgba(255, 255, 255, 0.18)
val GlassBorderSubtle = Color(0x14FFFFFF)        // rgba(255, 255, 255, 0.08)
val InputBg = Color(0x0DFFFFFF)                  // rgba(255, 255, 255, 0.05)
val InputBorder = Color(0x1AFFFFFF)              // rgba(255, 255, 255, 0.10)

// Typography Colors
val TextPrimary = Color(0xFFF1F5F9)              // Clean Crisp White
val TextSecondary = Color(0x99FFFFFF)            // ~60% White
val TextMuted = Color(0x66FFFFFF)                // ~40% White
val TextSubtle = Color(0x40FFFFFF)               // ~25% White

// ── Compatibility Aliases (maps legacy tokens to new redesign) ──────────────
val AppleBlue = ColorBlueViolet
val AppleBlueLight = ColorBlueVioletLight
val AppleCyan = ColorBlueVioletLight
val AppleIndigo = ColorBlueViolet
val AppleGreen = ColorGreen
val AppleRed = ColorRed
val AppleOrange = ColorOrange

val AppleCanvasDark = BgDeep
val AppleCanvasDeeper = BgMid
val AppleAmbientIndigo = BgMeshIndigo
val AppleAmbientBlue = BgMeshAzure

val AppleGlassBackground = GlassBg
val AppleGlassCardBackground = GlassBg
val AppleGlassHighlight = Color(0x40FFFFFF)
val AppleGlassBorder = GlassBorder
val AppleGlassCardBorder = GlassBorder

val AppleTextPrimary = TextPrimary
val AppleTextSecondary = TextSecondary
val AppleTextTertiary = TextMuted
val AppleIconTint = Color(0xCCFFFFFF)

val Purple80 = ColorBlueViolet
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = ColorBlueVioletLight
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)