package com.example.jaskier.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Always-light, bright palette: a kids' app stays cheerful in system dark mode too.
private val KidColorScheme = lightColorScheme(
    primary = CleanTeal,
    onPrimary = InkText,
    secondary = WarmOrange,
    onSecondary = InkText,
    tertiary = SkyBlue,
    background = SunnyBackground,
    onBackground = InkText,
    surface = SunnyBackground,
    onSurface = InkText,
)

private val KidShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
)

private val KidTypography = Typography(
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun JaskierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        shapes = KidShapes,
        typography = KidTypography,
        content = content,
    )
}
