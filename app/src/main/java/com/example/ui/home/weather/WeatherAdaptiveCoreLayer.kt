package com.example.ui.home.weather

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.state.WeykyUiState
import com.example.device.battery.ChargingState
import com.example.device.weather.WeatherSnapshot
import com.example.rendering.visual_state.CoreVisualState
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Weather condition types supported by the Adaptive Weather Core.
 */
enum class AdaptiveWeatherType {
  CLEAR,
  PARTLY_CLOUDY,
  CLOUDY,
  RAIN,
  THUNDERSTORM,
  UNKNOWN
}

/**
 * Resolves the real-time weather snapshot into one of the supported Adaptive Weather Core types.
 * Returns UNKNOWN if the weather data is missing, offline, or undetermined.
 */
fun resolveAdaptiveWeatherType(weather: WeatherSnapshot?): AdaptiveWeatherType {
  if (weather == null) return AdaptiveWeatherType.UNKNOWN
  val condition = (weather.condition ?: "").trim().lowercase()
  val code = weather.weatherCode

  return when {
    condition.contains("رعد") || condition.contains("عاصف") || condition.contains("thunder") ||
      condition.contains("storm") || code in listOf(95, 96, 99) ->
      AdaptiveWeatherType.THUNDERSTORM

    condition.contains("مطر") || condition.contains("زخات") || condition.contains("رذاذ") ||
      condition.contains("rain") || condition.contains("drizzle") || condition.contains("shower") ||
      code in 51..67 || code in 80..82 ->
      AdaptiveWeatherType.RAIN

    condition.contains("غائم كلي") || condition.contains("ملبد") || condition.contains("overcast") ||
      code == 3 ->
      AdaptiveWeatherType.CLOUDY

    condition.contains("غائم") || condition.contains("غيوم") || condition.contains("cloud") ||
      code in 1..2 ->
      AdaptiveWeatherType.PARTLY_CLOUDY

    condition.contains("صاف") || condition.contains("مشمس") || condition.contains("صحو") ||
      condition.contains("clear") || condition.contains("sunny") || code == 0 ->
      AdaptiveWeatherType.CLEAR

    condition.contains("ثلج") || condition.contains("snow") || code in 71..77 || code in 85..86 ->
      AdaptiveWeatherType.RAIN

    else ->
      if (condition.isNotEmpty() || code != null) AdaptiveWeatherType.CLEAR else AdaptiveWeatherType.UNKNOWN
  }
}

/**
 * Determines whether the current local time is nighttime based on system clock
 * and weather snapshot sunrise/sunset timings.
 */
fun isNightTime(weather: WeatherSnapshot?): Boolean {
  val cal = Calendar.getInstance()
  val hour = cal.get(Calendar.HOUR_OF_DAY)
  val minute = cal.get(Calendar.MINUTE)
  val currentMinute = hour * 60 + minute

  val sunriseMin = parseTimeToMinutes(weather?.sunriseToday) ?: (6 * 60) // 06:00 AM default
  val sunsetMin = parseTimeToMinutes(weather?.sunsetToday) ?: (18 * 60 + 30) // 06:30 PM default

  return currentMinute < sunriseMin || currentMinute >= sunsetMin
}

/**
 * Parses time strings like "06:15", "18:30", "6:15 AM", "6:30 PM" into total minutes from midnight.
 */
fun parseTimeToMinutes(timeStr: String?): Int? {
  if (timeStr.isNullOrBlank()) return null
  return try {
    val clean = timeStr.trim().lowercase()
    val isPm = clean.contains("pm") || clean.contains("م")
    val isAm = clean.contains("am") || clean.contains("ص")
    val digitsOnly = clean.replace(Regex("[^0-9:]"), "")
    val parts = digitsOnly.split(":")
    if (parts.isNotEmpty()) {
      var h = parts[0].toInt()
      val m = if (parts.size > 1) parts[1].toInt() else 0
      if (isPm && h < 12) h += 12
      if (isAm && h == 12) h = 0
      h * 60 + m
    } else null
  } catch (_: Exception) {
    null
  }
}

/**
 * Verifies if the WEYKY core is strictly in IDLE mode without any functional operations.
 * Returns false immediately if voice listening, speaking aloud, music playing, charging,
 * or any non-IDLE visual state is active.
 */
fun isCoreEligibleForWeatherEffect(uiState: WeykyUiState, isCharging: Boolean): Boolean {
  return uiState.visualState == CoreVisualState.IDLE &&
    !uiState.isVoiceListening &&
    !uiState.isSpeakingAloud &&
    !uiState.isMusicPlaying &&
    !isCharging
}

/**
 * Adaptive Weather Core Layer.
 *
 * Wraps the protected central WEYKY Core without altering its underlying logic,
 * shape, size, scale (0.44f), or animations.
 *
 * In IDLE mode ONLY:
 * - Renders a realistic Sun (daytime) or Moon (nighttime) directly over the core.
 * - In partly cloudy or cloudy weather, realistic volumetric clouds drift IN FRONT of the sun/moon.
 * - In rainy weather, rain clouds and precipitation streams pour down IN FRONT of the core.
 * - In clear weather, a brilliant realistic solar flare and blazing incandescent corona shine forth.
 *
 * Functional state transition guarantee:
 * When moving to ANY functional state (listening, speaking, music, call, charging, etc.),
 * all weather overlays immediately disappear (150ms cutoff) with zero canvas drawing overhead,
 * revealing the untouched original WEYKY Core.
 */
@Composable
fun WeatherAdaptiveCoreLayer(
  uiState: WeykyUiState,
  coreSizeDp: Dp,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val batterySnap = uiState.batterySnapshot
  val isDeviceCharging = batterySnap?.chargingState == ChargingState.CHARGING ||
    batterySnap?.chargingState == ChargingState.FULL

  val isEligible = isCoreEligibleForWeatherEffect(uiState, isDeviceCharging)
  val weatherType = resolveAdaptiveWeatherType(uiState.weatherSnapshot)
  val isNight = remember(uiState.weatherSnapshot) { isNightTime(uiState.weatherSnapshot) }
  val shouldRender = isEligible && weatherType != AdaptiveWeatherType.UNKNOWN

  // Swift cutoff on functional state switch (150ms); smooth harmonic fade-in on return to IDLE (600ms)
  val weatherAlpha by animateFloatAsState(
    targetValue = if (shouldRender) 1f else 0f,
    animationSpec = tween(
      durationMillis = if (shouldRender) 600 else 150,
      easing = FastOutSlowInEasing
    ),
    label = "AdaptiveWeatherAlpha"
  )

  // Atmospheric infinite animations
  val transition = rememberInfiniteTransition(label = "weather_core_motion")

  // Slow continuous solar corona rotation
  val solarRayAngleDeg by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 36000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "solar_ray_angle"
  )

  // Gentle solar corona breathing pulse
  val solarPulse by transition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "solar_pulse"
  )

  // Cloud drift across the face of the sun/moon (22 seconds per pass)
  val cloudDriftProgress by transition.animateFloat(
    initialValue = -0.4f,
    targetValue = 1.4f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 22000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "cloud_drift_progress"
  )

  // Rain fall cycle
  val rainProgress by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rain_progress"
  )

  // Rare subtle sheet lightning flash
  val lightningPhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "lightning_phase"
  )

  Box(
    modifier = modifier
      .size(coreSizeDp)
      .testTag("weather_adaptive_core_layer"),
    contentAlignment = Alignment.Center
  ) {
    // 1. Ambient Background Weather Canvas (Solar Corona & Radiant Aura behind core perimeter)
    if (weatherAlpha > 0.005f) {
      Canvas(
        modifier = Modifier
          .size(coreSizeDp * 1.40f)
          .testTag("weather_adaptive_core_bg_canvas")
      ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val coreRadius = (size.minDimension / (2f * 1.40f))

        if (!isNight) {
          // Daytime solar corona rays and radiant golden halo
          drawDaytimeSolarCorona(
            center = center,
            coreRadius = coreRadius,
            pulse = solarPulse,
            rayAngleDeg = solarRayAngleDeg,
            weatherType = weatherType,
            alpha = weatherAlpha
          )
        } else {
          // Nighttime soft moonlight lunar halo
          drawNightMoonlightHalo(
            center = center,
            coreRadius = coreRadius,
            pulse = solarPulse,
            weatherType = weatherType,
            alpha = weatherAlpha
          )
        }
      }
    }

    // 2. Protected WEYKY Core (untouched, original rendering)
    content()

    // 3. FOREGROUND REALISTIC CELESTIAL & WEATHER OVERLAY (OVER THE CORE)
    // Directly renders the realistic Sun/Moon surface, drifting clouds in front, and raindrops in front!
    if (weatherAlpha > 0.005f) {
      Canvas(
        modifier = Modifier
          .size(coreSizeDp * 1.40f)
          .testTag("weather_adaptive_core_fg_canvas")
      ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val coreRadius = (size.minDimension / (2f * 1.40f))

        // A. Draw Celestial Body Surface ON TOP OF THE CORE
        if (!isNight) {
          // Realistic blazing sun photosphere & lens optics
          drawRealisticSunOverlay(
            center = center,
            coreRadius = coreRadius,
            pulse = solarPulse,
            weatherType = weatherType,
            alpha = weatherAlpha
          )
        } else {
          // Realistic glowing moon disk with lunar maria / craters
          drawRealisticMoonOverlay(
            center = center,
            coreRadius = coreRadius,
            weatherType = weatherType,
            alpha = weatherAlpha
          )
        }

        // B. Draw Clouds IN FRONT OF the Sun / Moon
        if (weatherType == AdaptiveWeatherType.PARTLY_CLOUDY ||
            weatherType == AdaptiveWeatherType.CLOUDY ||
            weatherType == AdaptiveWeatherType.RAIN ||
            weatherType == AdaptiveWeatherType.THUNDERSTORM) {
          drawFrontCloudsDrifting(
            center = center,
            coreRadius = coreRadius,
            driftProgress = cloudDriftProgress,
            weatherType = weatherType,
            isNight = isNight,
            lightningPhase = lightningPhase,
            alpha = weatherAlpha
          )
        }

        // C. Draw Rain & Precipitation IN FRONT OF the Core
        if (weatherType == AdaptiveWeatherType.RAIN || weatherType == AdaptiveWeatherType.THUNDERSTORM) {
          val precipMm = uiState.weatherSnapshot?.precipitationMm ?: 3.0f
          drawFrontRainPrecipitation(
            center = center,
            coreRadius = coreRadius,
            rainProgress = rainProgress,
            precipitationMm = precipMm,
            alpha = weatherAlpha
          )
        }
      }
    }
  }
}

/**
 * Draws the realistic daytime incandescent Sun overlay directly over the core.
 * Features a glowing white-hot solar photosphere, golden chromosphere, and optical flare.
 */
private fun DrawScope.drawRealisticSunOverlay(
  center: Offset,
  coreRadius: Float,
  pulse: Float,
  weatherType: AdaptiveWeatherType,
  alpha: Float
) {
  val sunDiskRadius = coreRadius * 0.98f
  val intensity = if (weatherType == AdaptiveWeatherType.CLEAR) 0.88f else 0.72f

  // 1. Blazing solar photosphere radial gradient
  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color(0xFFFFFFFF).copy(alpha = 0.92f * intensity * alpha), // Blinding white-hot core
      0.25f to Color(0xFFFFF9C4).copy(alpha = 0.86f * intensity * alpha), // Pure solar yellow
      0.55f to Color(0xFFFFD54F).copy(alpha = 0.80f * intensity * alpha), // Rich solar amber
      0.82f to Color(0xFFFF9800).copy(alpha = 0.72f * intensity * alpha), // Chromosphere orange
      0.97f to Color(0xFFFF5722).copy(alpha = 0.65f * intensity * alpha), // Solar limb red-orange
      1.0f to Color(0x00FF5722),
      center = center,
      radius = sunDiskRadius
    ),
    radius = sunDiskRadius,
    center = center
  )

  // 2. Solar limb darkening rim
  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color.Transparent,
      0.82f to Color.Transparent,
      0.96f to Color(0xFFFF3D00).copy(alpha = 0.50f * intensity * alpha),
      1.0f to Color.Transparent,
      center = center,
      radius = sunDiskRadius
    ),
    radius = sunDiskRadius,
    center = center
  )

  // 3. Central optical solar flare glint
  val flareLen = sunDiskRadius * 0.75f * pulse
  drawLine(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.85f * alpha), Color.Transparent),
      center = center,
      radius = flareLen
    ),
    start = Offset(center.x - flareLen, center.y),
    end = Offset(center.x + flareLen, center.y),
    strokeWidth = 2.5.dp.toPx(),
    cap = StrokeCap.Round
  )
  drawLine(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.85f * alpha), Color.Transparent),
      center = center,
      radius = flareLen * 0.65f
    ),
    start = Offset(center.x, center.y - flareLen * 0.65f),
    end = Offset(center.x, center.y + flareLen * 0.65f),
    strokeWidth = 2.dp.toPx(),
    cap = StrokeCap.Round
  )
}

/**
 * Draws the realistic Moon overlay directly over the core at night.
 * Features a pearlescent silver lunar surface with authentic darker lunar maria (craters and plains).
 */
private fun DrawScope.drawRealisticMoonOverlay(
  center: Offset,
  coreRadius: Float,
  weatherType: AdaptiveWeatherType,
  alpha: Float
) {
  val moonDiskRadius = coreRadius * 0.98f
  val intensity = if (weatherType == AdaptiveWeatherType.CLEAR) 0.85f else 0.70f

  // 1. Pearlescent silver-white lunar disk base
  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color(0xFFF1F5F9).copy(alpha = 0.90f * intensity * alpha),
      0.65f to Color(0xFFE2E8F0).copy(alpha = 0.85f * intensity * alpha),
      0.88f to Color(0xFFCBD5E1).copy(alpha = 0.78f * intensity * alpha),
      0.98f to Color(0xFF94A3B8).copy(alpha = 0.68f * intensity * alpha),
      1.0f to Color.Transparent,
      center = center,
      radius = moonDiskRadius
    ),
    radius = moonDiskRadius,
    center = center
  )

  // 2. Realistic Lunar Maria (dark basalt plains like Sea of Tranquility and Ocean of Storms)
  // Maria 1: Upper-left large plain
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF475569).copy(alpha = 0.52f * alpha), Color.Transparent),
      center = Offset(center.x - moonDiskRadius * 0.32f, center.y - moonDiskRadius * 0.28f),
      radius = moonDiskRadius * 0.36f
    ),
    radius = moonDiskRadius * 0.36f,
    center = Offset(center.x - moonDiskRadius * 0.32f, center.y - moonDiskRadius * 0.28f)
  )

  // Maria 2: Central-right plain
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF334155).copy(alpha = 0.45f * alpha), Color.Transparent),
      center = Offset(center.x + moonDiskRadius * 0.28f, center.y - moonDiskRadius * 0.08f),
      radius = moonDiskRadius * 0.30f
    ),
    radius = moonDiskRadius * 0.30f,
    center = Offset(center.x + moonDiskRadius * 0.28f, center.y - moonDiskRadius * 0.08f)
  )

  // Maria 3: Lower-left southern plain
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF475569).copy(alpha = 0.48f * alpha), Color.Transparent),
      center = Offset(center.x - moonDiskRadius * 0.18f, center.y + moonDiskRadius * 0.38f),
      radius = moonDiskRadius * 0.28f
    ),
    radius = moonDiskRadius * 0.28f,
    center = Offset(center.x - moonDiskRadius * 0.18f, center.y + moonDiskRadius * 0.38f)
  )

  // 3. Moon silver limb rim
  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color.Transparent,
      0.88f to Color.Transparent,
      0.97f to Color(0xFFBAE6FD).copy(alpha = 0.45f * alpha),
      1.0f to Color.Transparent,
      center = center,
      radius = moonDiskRadius
    ),
    radius = moonDiskRadius,
    center = center
  )
}

/**
 * Draws realistic volumetric clouds passing directly IN FRONT OF the Sun or Moon.
 */
private fun DrawScope.drawFrontCloudsDrifting(
  center: Offset,
  coreRadius: Float,
  driftProgress: Float,
  weatherType: AdaptiveWeatherType,
  isNight: Boolean,
  lightningPhase: Float,
  alpha: Float
) {
  val span = coreRadius * 2.8f
  val baseCloudX = center.x - span * 0.5f + driftProgress * span

  val isStorm = weatherType == AdaptiveWeatherType.THUNDERSTORM
  val isRain = weatherType == AdaptiveWeatherType.RAIN
  val isOvercast = weatherType == AdaptiveWeatherType.CLOUDY

  // Sheet lightning illumination
  val isLightningFlashing = isStorm && (lightningPhase in 0.94f..0.97f)
  val flashBoost = if (isLightningFlashing) 0.65f else 0.0f

  // Colors for clouds
  val topHighlightColor = when {
    isLightningFlashing -> Color(0xFFA5B4FC).copy(alpha = 0.85f * alpha)
    isNight -> Color(0xFF94A3B8).copy(alpha = 0.62f * alpha)
    isStorm || isRain -> Color(0xFF64748B).copy(alpha = 0.70f * alpha)
    else -> Color(0xFFFFFFFF).copy(alpha = 0.84f * alpha) // Pure white fluffy cumulus highlight
  }

  val bodyColor = when {
    isLightningFlashing -> Color(0xFF4338CA).copy(alpha = 0.80f * alpha)
    isNight -> Color(0xFF1E293B).copy(alpha = 0.76f * alpha)
    isStorm -> Color(0xFF0F172A).copy(alpha = 0.85f * alpha)
    isRain -> Color(0xFF334155).copy(alpha = 0.80f * alpha)
    isOvercast -> Color(0xFFB0BEC5).copy(alpha = 0.74f * alpha)
    else -> Color(0xFFECEFF1).copy(alpha = 0.72f * alpha) // Soft daytime cloud belly
  }

  val silverLiningColor = if (!isNight && !isStorm) {
    Color(0xFFFFD54F).copy(alpha = 0.65f * alpha) // Golden sunlight edge backlighting
  } else {
    Color(0xFFE2E8F0).copy(alpha = 0.45f * alpha) // Silver moonlight edge
  }

  // Cloud Cluster 1: Drifting across the upper-center face of the sun/moon
  val cluster1Y = center.y - coreRadius * 0.20f
  val puff1Radius = coreRadius * (if (isOvercast || isRain || isStorm) 0.56f else 0.44f)

  // Cloud puffs in cluster 1
  val puffs1 = listOf(
    Offset(baseCloudX - puff1Radius * 0.7f, cluster1Y + puff1Radius * 0.1f) to (puff1Radius * 0.8f),
    Offset(baseCloudX, cluster1Y - puff1Radius * 0.15f) to (puff1Radius * 1.05f),
    Offset(baseCloudX + puff1Radius * 0.8f, cluster1Y + puff1Radius * 0.12f) to (puff1Radius * 0.85f),
    Offset(baseCloudX + puff1Radius * 1.5f, cluster1Y + puff1Radius * 0.25f) to (puff1Radius * 0.65f)
  )

  for ((pos, r) in puffs1) {
    // Silver / golden lining rim
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(silverLiningColor, Color.Transparent),
        center = Offset(pos.x, pos.y - r * 0.25f),
        radius = r * 1.15f
      ),
      radius = r * 1.15f,
      center = Offset(pos.x, pos.y - r * 0.25f)
    )

    // Main cloud puff volume
    drawCircle(
      brush = Brush.radialGradient(
        0.0f to topHighlightColor,
        0.55f to bodyColor,
        0.95f to bodyColor.copy(alpha = bodyColor.alpha * 0.8f),
        1.0f to Color.Transparent,
        center = Offset(pos.x - r * 0.15f, pos.y - r * 0.2f),
        radius = r
      ),
      radius = r,
      center = pos
    )
  }

  // Cloud Cluster 2: Secondary lower drifting cloud band (if cloudy or stormy or staggered)
  if (isOvercast || isRain || isStorm || weatherType == AdaptiveWeatherType.PARTLY_CLOUDY) {
    val cluster2X = baseCloudX - coreRadius * 0.85f
    val cluster2Y = center.y + coreRadius * 0.32f
    val puff2Radius = coreRadius * 0.42f

    val puffs2 = listOf(
      Offset(cluster2X - puff2Radius * 0.5f, cluster2Y) to (puff2Radius * 0.75f),
      Offset(cluster2X + puff2Radius * 0.3f, cluster2Y - puff2Radius * 0.1f) to (puff2Radius * 0.95f),
      Offset(cluster2X + puff2Radius * 1.1f, cluster2Y + puff2Radius * 0.1f) to (puff2Radius * 0.80f)
    )

    for ((pos, r) in puffs2) {
      drawCircle(
        brush = Brush.radialGradient(
          0.0f to topHighlightColor,
          0.60f to bodyColor,
          1.0f to Color.Transparent,
          center = Offset(pos.x, pos.y - r * 0.2f),
          radius = r
        ),
        radius = r,
        center = pos
      )
    }
  }
}

/**
 * Draws realistic rain streams falling downward across and in front of the core.
 */
private fun DrawScope.drawFrontRainPrecipitation(
  center: Offset,
  coreRadius: Float,
  rainProgress: Float,
  precipitationMm: Float,
  alpha: Float
) {
  val dropCount = if (precipitationMm > 6f) 32 else if (precipitationMm > 2f) 22 else 14
  val dropLength = 22.dp.toPx()
  val widthSpan = coreRadius * 2.4f
  val heightSpan = coreRadius * 2.8f

  for (i in 0 until dropCount) {
    val xFraction = ((i * 0.1618f) % 1.0f) // golden ratio distribution across width
    val dropX = center.x - widthSpan * 0.5f + xFraction * widthSpan

    val yStagger = ((i * 0.37f) % 1.0f)
    val effectiveProgress = (rainProgress + yStagger) % 1.0f
    val startY = center.y - heightSpan * 0.5f + effectiveProgress * heightSpan
    val endY = startY + dropLength

    // Smooth boundary fade
    val normY = (startY - (center.y - heightSpan * 0.5f)) / heightSpan
    val edgeFade = sin(normY * Math.PI).toFloat().coerceIn(0f, 1f)

    val dropColor = Color(0xFF00F0FF).copy(alpha = 0.60f * edgeFade * alpha)

    drawLine(
      color = dropColor,
      start = Offset(dropX, startY),
      end = Offset(dropX - dropLength * 0.15f, endY), // gentle natural wind slant
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round
    )
  }
}

/**
 * Draws daytime solar corona and ambient solar rays radiating behind the core perimeter.
 */
private fun DrawScope.drawDaytimeSolarCorona(
  center: Offset,
  coreRadius: Float,
  pulse: Float,
  rayAngleDeg: Float,
  weatherType: AdaptiveWeatherType,
  alpha: Float
) {
  val haloRadius = coreRadius * 1.35f * pulse
  val intensity = if (weatherType == AdaptiveWeatherType.CLEAR) 1.0f else 0.65f

  // 1. Soft glowing outer corona
  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color.Transparent,
      0.68f to Color.Transparent,
      0.85f to Color(0xFFFFD54F).copy(alpha = 0.36f * intensity * alpha),
      0.96f to Color(0xFFFF9800).copy(alpha = 0.22f * intensity * alpha),
      1.0f to Color.Transparent,
      center = center,
      radius = haloRadius
    ),
    radius = haloRadius,
    center = center
  )

  // 2. Radiant solar rays (12 rays extending outward)
  val rayCount = 12
  val innerR = coreRadius * 0.95f
  val outerR = coreRadius * 1.35f * pulse

  for (i in 0 until rayCount) {
    val angle = Math.toRadians((i * (360.0 / rayCount) + rayAngleDeg).toDouble())
    val startX = center.x + (cos(angle) * innerR).toFloat()
    val startY = center.y + (sin(angle) * innerR).toFloat()
    val endX = center.x + (cos(angle) * outerR).toFloat()
    val endY = center.y + (sin(angle) * outerR).toFloat()

    val rayColor = if (i % 2 == 0) {
      Color(0xFFFFEE58).copy(alpha = 0.30f * intensity * alpha)
    } else {
      Color(0xFFFF9800).copy(alpha = 0.22f * intensity * alpha)
    }

    drawLine(
      brush = Brush.linearGradient(
        colors = listOf(rayColor, Color.Transparent),
        start = Offset(startX, startY),
        end = Offset(endX, endY)
      ),
      start = Offset(startX, startY),
      end = Offset(endX, endY),
      strokeWidth = 2.4.dp.toPx(),
      cap = StrokeCap.Round
    )
  }
}

/**
 * Draws nighttime celestial moonlight halo radiating behind the core perimeter.
 */
private fun DrawScope.drawNightMoonlightHalo(
  center: Offset,
  coreRadius: Float,
  pulse: Float,
  weatherType: AdaptiveWeatherType,
  alpha: Float
) {
  val haloRadius = coreRadius * 1.32f * pulse
  val intensity = if (weatherType == AdaptiveWeatherType.CLEAR) 0.85f else 0.55f

  drawCircle(
    brush = Brush.radialGradient(
      0.0f to Color.Transparent,
      0.72f to Color.Transparent,
      0.88f to Color(0xFFBAE6FD).copy(alpha = 0.28f * intensity * alpha),
      0.97f to Color(0xFF60A5FA).copy(alpha = 0.18f * intensity * alpha),
      1.0f to Color.Transparent,
      center = center,
      radius = haloRadius
    ),
    radius = haloRadius,
    center = center
  )
}
