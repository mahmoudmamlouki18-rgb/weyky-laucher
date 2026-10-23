package com.example.ui.home.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.example.device.battery.ChargingState
import com.example.core.state.WeykyUiState
import com.example.device.apps.AppMatch
import com.example.ui.adaptive.DeviceProfile
import com.example.ui.home.HomeContextAlertCard
import com.example.ui.home.HomeCoreSection
import com.example.ui.home.HomeResponseCard
import com.example.ui.theme.JarvisHudColors
import com.example.ui.theme.WeykyThemeMode

/**
 * Redesigned Landscape Desktop Experience for WEYKY Launcher.
 * Specifically adapted for Samsung Galaxy A24 and mobile widescreen ergonomics:
 * - Upper-Left: Compact, desktop-style music player widget with transport controls.
 * - Upper-Right: Compact information pills (Location, Weather, Date).
 * - Center Stage: Completely protected, spacious living WEYKY Core.
 * - Bottom: Slim, professional desktop taskbar with embedded search box and dialer shortcut.
 */
@Composable
fun DesktopLandscapeHomeScreen(
  uiState: WeykyUiState,
  deviceProfile: DeviceProfile,
  borderColor: Color,
  panelBg: Color,
  panelBgElevated: Color,
  textPrimary: Color,
  textSecondary: Color,
  textMuted: Color,
  isLight: Boolean,
  onQuerySubmit: (String) -> Unit,
  onQueryChange: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onRequestPermission: (String) -> Unit,
  onToggleVoice: () -> Unit,
  onConfirmOption: (String) -> Unit,
  onToggleAppDrawer: () -> Unit,
  onLaunchApp: (AppMatch) -> Unit,
  onToggleStartMenu: (Boolean?) -> Unit,
  onPinApp: (String) -> Unit,
  onUnpinApp: (String) -> Unit,
  onCallClick: () -> Unit,
  onRequestDefaultLauncher: () -> Unit,
  onToggleMediaPlayback: () -> Unit,
  onNextMediaTrack: () -> Unit,
  onPreviousMediaTrack: () -> Unit,
  keyboardController: SoftwareKeyboardController?,
  modifier: Modifier = Modifier
) {
  val dynamicCoreSize = deviceProfile.calculateAdaptiveCoreSize(uiState.coreScale)
  val hasResponse = !uiState.lastResponseText.isNullOrBlank()

  val battery = uiState.batterySnapshot
  val isLowBattery = battery != null && battery.isAvailable && battery.percentage <= 20 &&
    battery.chargingState != ChargingState.CHARGING &&
    battery.chargingState != ChargingState.FULL
  val isBatteryFull = battery != null && battery.isAvailable && battery.percentage == 100 &&
    battery.chargingState == ChargingState.FULL
  val hasMissedCall = (uiState.missedCallsSummary?.missedCallsCount ?: 0) > 0
  val hasUrgentNotif = (uiState.notificationSummary?.totalImportantCount ?: 0) > 0
  val hasFallbackEvent = uiState.importantEvents.isNotEmpty()
  val hasAlert = isLowBattery || isBatteryFull || hasMissedCall || hasUrgentNotif || hasFallbackEvent

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("desktop_landscape_root")
  ) {
    // 1. DESKTOP WORKSPACE (Upper widgets + Spacious Central Core + Response Bubble)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = 14.dp,
          end = 14.dp,
          top = 6.dp,
          bottom = 54.dp // Reserves clean height for the 48dp floating taskbar
        ),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // TOP ROW: Music Player (Upper-Left) and Info Widgets (Upper-Right)
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          // Upper-Left: Compact Music Player Widget
          DesktopCompactMusicWidget(
            uiState = uiState,
            themeMode = uiState.themeMode,
            borderColor = borderColor,
            panelBg = panelBgElevated,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onToggleMediaPlayback = onToggleMediaPlayback,
            onPreviousMediaTrack = onPreviousMediaTrack,
            onNextMediaTrack = onNextMediaTrack
          )

          // Upper-Right: Compact Information Pills (Location, Weather, Date)
          DesktopCompactInfoPills(
            uiState = uiState,
            themeMode = uiState.themeMode,
            borderColor = borderColor,
            panelBg = panelBgElevated,
            textPrimary = textPrimary,
            textSecondary = textSecondary
          )
        }
      }

      // Contextual Alert (if present, rendered as a compact floating card)
      if (hasAlert) {
        Box(
          modifier = Modifier
            .widthIn(max = 520.dp)
            .padding(vertical = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          HomeContextAlertCard(
            uiState = uiState,
            borderColor = borderColor,
            onQuerySubmit = onQuerySubmit
          )
        }
      }

      // Center Stage: The protected, untouched WEYKY Living Core
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f, fill = true)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        val maxAvailableDim = minOf(maxHeight, maxWidth)
        val calculatedCoreSize = minOf(dynamicCoreSize, maxAvailableDim * 0.95f)
          .coerceIn(minOf(80.dp, maxAvailableDim * 0.75f), 350.dp)

        HomeCoreSection(
          uiState = uiState,
          coreSizeDp = calculatedCoreSize,
          onToggleVoice = onToggleVoice
        )
      }

      // Assistant Response Card (if active, floating neatly above taskbar)
      if (hasResponse) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 660.dp)
            .heightIn(max = 84.dp)
            .padding(bottom = 2.dp)
            .verticalScroll(rememberScrollState())
        ) {
          HomeResponseCard(
            uiState = uiState,
            borderColor = borderColor,
            panelBg = panelBg,
            panelBgElevated = panelBgElevated,
            textPrimary = textPrimary,
            onQuerySubmit = onQuerySubmit,
            onConfirmOption = onConfirmOption,
            onRequestPermission = onRequestPermission
          )
        }
      }
    }

    // 2. BOTTOM DESKTOP TASKBAR (Floating slim bar across the bottom)
    DesktopTaskbar(
      uiState = uiState,
      isLight = isLight,
      borderColor = borderColor,
      panelBg = panelBg,
      panelBgElevated = panelBgElevated,
      textPrimary = textPrimary,
      textSecondary = textSecondary,
      textMuted = textMuted,
      isStartMenuOpen = uiState.isStartMenuOpen,
      onToggleStartMenu = { onToggleStartMenu(null) },
      onLaunchApp = onLaunchApp,
      onUnpinApp = onUnpinApp,
      onQueryChange = onQueryChange,
      onQuerySubmit = onQuerySubmit,
      onToggleVoice = onToggleVoice,
      onCallClick = onCallClick,
      keyboardController = keyboardController,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp)
    )

    // 3. START MENU OVERLAY (Opens above the taskbar when WEYKY button is tapped)
    if (uiState.isStartMenuOpen) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {
            onToggleStartMenu(false)
          }
      )

      AnimatedVisibility(
        visible = uiState.isStartMenuOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 12.dp, bottom = 54.dp)
      ) {
        DesktopStartMenu(
          uiState = uiState,
          isLight = isLight,
          borderColor = borderColor,
          panelBg = panelBg,
          panelBgElevated = panelBgElevated,
          textPrimary = textPrimary,
          textSecondary = textSecondary,
          textMuted = textMuted,
          onDismiss = { onToggleStartMenu(false) },
          onLaunchApp = onLaunchApp,
          onTogglePinApp = onPinApp,
          onOpenSettings = onOpenSettings,
          onRequestDefaultLauncher = onRequestDefaultLauncher
        )
      }
    }
  }
}

/**
 * Compact Desktop Music Player Widget (Upper-Left).
 * Strictly mirrors the supplied reference design:
 * - Gradient album art / play icon on left.
 * - Track title and artist with thin volume progress bar.
 * - Transport controls (Previous, circular cyan Play/Pause, Next).
 */
@Composable
private fun DesktopCompactMusicWidget(
  uiState: WeykyUiState,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  panelBg: Color,
  textPrimary: Color,
  textSecondary: Color,
  onToggleMediaPlayback: () -> Unit,
  onPreviousMediaTrack: () -> Unit,
  onNextMediaTrack: () -> Unit
) {
  val mediaMeta = uiState.activeMediaMetadata
  val isMusicActivelyPlaying = uiState.isMusicPlaying || (mediaMeta?.isPlaying == true)

  val trackTitle = mediaMeta?.title?.takeIf { it.isNotBlank() } ?: "تشغيل الموسيقى"
  val trackArtist = mediaMeta?.artist?.takeIf { it.isNotBlank() } ?: "مشغل الصوت"
  val artworkBitmap = mediaMeta?.artworkBitmap

  val cardShape = RoundedCornerShape(14.dp)

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
      modifier = Modifier
        .widthIn(min = 250.dp, max = 290.dp)
        .height(56.dp)
        .shadow(elevation = 6.dp, shape = cardShape)
        .clip(cardShape)
        .background(Color(0xFF030C1A).copy(alpha = 0.90f))
        .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.50f), cardShape)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .testTag("music_top_hud_card")
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // 1. Album Artwork / Icon
        if (artworkBitmap != null) {
          Image(
            bitmap = artworkBitmap.asImageBitmap(),
            contentDescription = "Album Art",
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(8.dp))
          )
        } else {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    Color(0xFF8A2387),
                    Color(0xFFE94057),
                    Color(0xFF00D2FF)
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Music",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Track Title, Artist, and Thin Volume Line
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = trackTitle,
            color = textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = trackArtist,
            color = textSecondary,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(3.dp))

          // Subtle cyan volume / progress bar with speaker icon
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "Volume",
              tint = Color(0xFF00F0FF).copy(alpha = 0.8f),
              modifier = Modifier.size(10.dp)
            )
            Box(
              modifier = Modifier
                .width(44.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFF00F0FF).copy(alpha = 0.7f))
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 3. Transport Controls: [ Previous | (Play/Pause) | Next ]
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          IconButton(
            onClick = onPreviousMediaTrack,
            modifier = Modifier
              .size(28.dp)
              .testTag("music_prev_button")
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous Track",
              tint = Color(0xFF00F0FF),
              modifier = Modifier.size(18.dp)
            )
          }

          // Circular Cyan Play/Pause button
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF00F0FF).copy(alpha = 0.15f))
              .border(1.5.dp, Color(0xFF00F0FF), CircleShape)
              .clickable(onClick = onToggleMediaPlayback)
              .testTag("music_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isMusicActivelyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isMusicActivelyPlaying) "Pause" else "Play",
              tint = Color(0xFF00F0FF),
              modifier = Modifier.size(16.dp)
            )
          }

          IconButton(
            onClick = onNextMediaTrack,
            modifier = Modifier
              .size(28.dp)
              .testTag("music_next_button")
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next Track",
              tint = Color(0xFF00F0FF),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Compact Top-Right Information Pills:
 * [ 📍 موقعي الحالي ]  [ ☀️ 14° غائم ]  [ الجمعة، 23 أكتوبر / FRIDAY 📅 ]
 */
@Composable
private fun DesktopCompactInfoPills(
  uiState: WeykyUiState,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  panelBg: Color,
  textPrimary: Color,
  textSecondary: Color
) {
  val pillShape = RoundedCornerShape(12.dp)
  val pillBg = Color(0xFF030C1A).copy(alpha = 0.88f)
  val pillBorder = Color(0xFF00F0FF).copy(alpha = 0.45f)

  val weather = uiState.weatherSnapshot
  val tempText = if (weather != null && weather.isAvailable && weather.temperatureCelsius != null) {
    "${weather.temperatureCelsius.toInt()}°"
  } else {
    "14°"
  }
  val conditionText = weather?.condition ?: "غائم"
  val locationText = weather?.locationName?.takeIf { it.isNotBlank() } ?: "موقعي الحالي"
  val dateText = uiState.currentDateString.ifBlank { "الجمعة، 23 أكتوبر" }
  val engDateText = uiState.currentEnglishDateString.ifBlank { "FRIDAY, 23 OCTOBER 2026" }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Location Pill: [ 📍 موقعي الحالي ]
      Row(
        modifier = Modifier
          .height(36.dp)
          .clip(pillShape)
          .background(pillBg)
          .border(1.dp, pillBorder, pillShape)
          .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = "Location",
          tint = Color(0xFF00F0FF),
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = locationText,
          color = textPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // 2. Weather Pill: [ ☀️ 14° غائم ]
      Row(
        modifier = Modifier
          .height(36.dp)
          .clip(pillShape)
          .background(pillBg)
          .border(1.dp, pillBorder, pillShape)
          .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = if (conditionText.contains("مطر")) Icons.Default.Cloud else Icons.Default.WbSunny,
          contentDescription = "Weather",
          tint = Color(0xFF00F0FF),
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = tempText,
          color = textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = conditionText,
          color = textSecondary,
          fontSize = 11.sp
        )
      }

      // 3. Date Pill: [ الجمعة، 23 أكتوبر \n FRIDAY, 23 OCTOBER 2026 📅 ]
      Row(
        modifier = Modifier
          .height(36.dp)
          .clip(pillShape)
          .background(pillBg)
          .border(1.dp, pillBorder, pillShape)
          .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = dateText,
            color = textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
          Text(
            text = engDateText.uppercase(),
            color = textSecondary.copy(alpha = 0.8f),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
          )
        }

        Icon(
          imageVector = Icons.Default.CalendarToday,
          contentDescription = "Date",
          tint = Color(0xFF00F0FF),
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}
