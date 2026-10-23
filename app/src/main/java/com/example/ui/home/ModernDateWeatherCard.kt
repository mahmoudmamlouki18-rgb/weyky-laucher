package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.state.WeykyUiState
import com.example.ui.theme.WeykyAdaptiveCard

/**
 * Weather icon with sun peeking behind the cloud as depicted in the reference mockup.
 */
@Composable
fun ModernSunCloudIcon(
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.size(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.WbSunny,
      contentDescription = null,
      tint = Color(0xFFFBBF24),
      modifier = Modifier
        .size(16.dp)
        .align(Alignment.TopEnd)
    )
    Icon(
      imageVector = Icons.Default.CloudQueue,
      contentDescription = "Partly Cloudy",
      tint = Color(0xFFF1F5F9),
      modifier = Modifier
        .size(24.dp)
        .align(Alignment.BottomStart)
    )
  }
}

/**
 * Modern Combined Date & Weather Card.
 * Matches exact visual details from the user's reference mockup:
 * - Left: Calendar squircle icon, Arabic date, English date.
 * - Center: Vertical hairline glass divider.
 * - Right: Sun+Cloud icon, Temperature (18°), condition, location pin + "الجزائر".
 */
@Composable
fun ModernCombinedDateWeatherCard(
  uiState: WeykyUiState,
  borderColor: Color,
  textPrimary: Color,
  textSecondary: Color,
  textMuted: Color,
  onQuerySubmit: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val weather = uiState.weatherSnapshot
  val tempVal = weather?.temperatureCelsius
  val tempText = if (weather != null && weather.isAvailable && tempVal != null) {
    "${tempVal.toInt()}°"
  } else {
    "18°"
  }
  val condition = weather?.condition?.ifBlank { null } ?: "غائم جزئياً"
  val locationName = "الجزائر"

  val primaryDate = uiState.currentDateString.ifBlank { "الخميس، 22 أكتوبر" }
  val englishDate = uiState.currentEnglishDateString.ifBlank { "THURSDAY, 22 OCT" }

  WeykyAdaptiveCard(
    uiState = uiState,
    accentColor = borderColor,
    onClick = { onQuerySubmit("ما حالة الطقس وتاريخ اليوم؟") },
    modifier = modifier
      .fillMaxWidth()
      .testTag("modern_combined_date_weather_card")
  ) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. DATE SECTION (Left side of card)
        Row(
          modifier = Modifier.weight(1.05f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Squircle Calendar Icon Container
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0x2800D2FF)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = "Date",
              tint = Color(0xFF00D2FF),
              modifier = Modifier.size(22.dp)
            )
          }

          Column(verticalArrangement = Arrangement.Center) {
            Text(
              text = primaryDate,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = englishDate.uppercase(),
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp,
              color = Color(0xFF8EB2D6),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Vertical Soft Glass Divider
        Box(
          modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(38.dp)
            .background(Color(0x2800D2FF))
        )

        // 2. WEATHER SECTION (Right side of card)
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Sun + Cloud Icon
          ModernSunCloudIcon(
            modifier = Modifier.size(38.dp)
          )

          Column(verticalArrangement = Arrangement.Center) {
            Text(
              text = tempText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = textPrimary,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
              text = condition,
              fontSize = 10.5.sp,
              color = Color(0xFF8EB2D6),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Location",
                tint = Color(0xFF00D2FF),
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = locationName,
                fontSize = 9.5.sp,
                color = Color(0xFF8EB2D6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}
