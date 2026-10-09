package id.smartpantry.app.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import id.smartpantry.app.R
import androidx.compose.ui.unit.sp

@Composable fun SmartPantryTheme(content: @Composable () -> Unit) {
    val editorial=FontFamily(Font(R.font.lora_regular,FontWeight.Normal),Font(R.font.lora_bold,FontWeight.Bold))
    val body=FontFamily(Font(R.font.manrope_regular,FontWeight.Normal),Font(R.font.manrope_semibold,FontWeight.SemiBold),Font(R.font.manrope_bold,FontWeight.Bold))
    val scheme = if (isSystemInDarkTheme()) darkColorScheme(
        primary=Color(0xFFB3D3BC), onPrimary=Color(0xFF173C2C),
        primaryContainer=Color(0xFF254B38), onPrimaryContainer=Color(0xFFE4F1E5),
        secondary=Color(0xFFE2BA8E), secondaryContainer=Color(0xFF354B3B),onSecondaryContainer=Color(0xFFE4F1E5), background=Color(0xFF171C18), surface=Color(0xFF171C18),
        surfaceContainer=Color(0xFF242C25), surfaceContainerLow=Color(0xFF202720), surfaceContainerHigh=Color(0xFF303B32), surfaceVariant=Color(0xFF303B32),
        onSurface=Color(0xFFF0EEE5), onSurfaceVariant=Color(0xFFBFC8BC),outline=Color(0xFF7F9C87))
    else lightColorScheme(primary=Color(0xFF254D38), onPrimary=Color.White,
        primaryContainer=Color(0xFFE4EDDF), onPrimaryContainer=Color(0xFF203C2B),
        secondary=Color(0xFFB74221),secondaryContainer=Color(0xFFE4EDDF),onSecondaryContainer=Color(0xFF203C2B), background=Color(0xFFFAF7EF), surface=Color(0xFFFFFFFF),
        surfaceContainer=Color(0xFFF0EDE3), surfaceContainerLow=Color(0xFFF5F1E7), surfaceContainerHigh=Color(0xFFECEEE3), surfaceVariant=Color(0xFFECEEE3),
        onSurface=Color(0xFF202C23), onSurfaceVariant=Color(0xFF626B5F), outline=Color(0xFFC4CABB))
    MaterialTheme(colorScheme=scheme, typography=Typography(
        headlineLarge=TextStyle(fontFamily=body,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=36.sp),
        headlineMedium=TextStyle(fontFamily=body,fontWeight=FontWeight.Bold,fontSize=26.sp,lineHeight=32.sp),
        titleLarge=TextStyle(fontFamily=body,fontWeight=FontWeight.Bold,fontSize=20.sp,lineHeight=26.sp),
        titleMedium=TextStyle(fontFamily=body,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=23.sp),
        bodyLarge=TextStyle(fontFamily=body,fontSize=16.sp,lineHeight=25.sp),
        bodyMedium=TextStyle(fontFamily=body,fontSize=14.sp,lineHeight=21.sp),
        bodySmall=TextStyle(fontFamily=body,fontSize=12.sp,lineHeight=18.sp),
        labelLarge=TextStyle(fontFamily=body,fontWeight=FontWeight.Bold,fontSize=14.sp,lineHeight=20.sp),
        labelMedium=TextStyle(fontFamily=body,fontWeight=FontWeight.SemiBold,fontSize=12.sp,lineHeight=17.sp),
        labelSmall=TextStyle(fontFamily=body,fontWeight=FontWeight.SemiBold,fontSize=11.sp,lineHeight=16.sp)
    ),content=content)
}
