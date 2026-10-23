package com.example.ui.home.desktop

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.state.WeykyUiState
import com.example.device.apps.AppMatch
import com.example.device.battery.ChargingState
import com.example.ui.home.AppIconCache
import com.example.ui.theme.JarvisHudColors
import com.example.ui.theme.WeykyThemeMode

/**
 * Slim, Desktop-Inspired Taskbar for WEYKY Landscape Mode.
 * Precision-engineered based on the reference design:
 * - Left: System pill with Phone Dialer shortcut, live clock, and cyan battery indicator.
 * - Center-Left: Pinned application dock with active app indicator and launch/unpin actions.
 * - Center-Right: Slim, rounded pill search bar with voice mic and query execution.
 * - Right: WEYKY Start Button with circular glowing logo emblem.
 */
@Composable
fun DesktopTaskbar(
  uiState: WeykyUiState,
  isLight: Boolean,
  borderColor: Color,
  panelBg: Color,
  panelBgElevated: Color,
  textPrimary: Color,
  textSecondary: Color,
  textMuted: Color,
  isStartMenuOpen: Boolean,
  onToggleStartMenu: () -> Unit,
  onLaunchApp: (AppMatch) -> Unit,
  onUnpinApp: (String) -> Unit,
  onQueryChange: (String) -> Unit,
  onQuerySubmit: (String) -> Unit,
  onToggleVoice: () -> Unit,
  onCallClick: () -> Unit,
  keyboardController: SoftwareKeyboardController?,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val themeMode = uiState.themeMode
  val isTactical = themeMode == WeykyThemeMode.TACTICAL
  val isModern = themeMode == WeykyThemeMode.MODERN

  val taskbarShape = when {
    isTactical -> CutCornerShape(8.dp)
    isModern -> RoundedCornerShape(24.dp)
    else -> RoundedCornerShape(20.dp)
  }

  val taskbarBg = Brush.verticalGradient(
    colors = listOf(
      Color(0xFF030D1C).copy(alpha = 0.92f),
      Color(0xFF01060D).copy(alpha = 0.98f)
    )
  )

  val taskbarBorderColor = when {
    isTactical -> borderColor.copy(alpha = 0.85f)
    isModern -> borderColor.copy(alpha = 0.50f)
    else -> borderColor.copy(alpha = 0.35f)
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(48.dp)
        .shadow(elevation = 10.dp, shape = taskbarShape)
        .clip(taskbarShape)
        .background(taskbarBg)
        .border(
          width = 1.dp,
          color = taskbarBorderColor,
          shape = taskbarShape
        )
        .testTag("desktop_taskbar")
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // 1. LEFT SECTION: System Status & Phone Call Pill
        DesktopSystemPill(
          uiState = uiState,
          themeMode = themeMode,
          borderColor = borderColor,
          textPrimary = textPrimary,
          textSecondary = textSecondary,
          onCallClick = onCallClick
        )

        // 2. CENTER-LEFT: Pinned Applications Dock
        DesktopPinnedAppsDock(
          pinnedPackages = uiState.pinnedAppPackageNames,
          installedApps = uiState.installedApps,
          context = context,
          themeMode = themeMode,
          borderColor = borderColor,
          textPrimary = textPrimary,
          onLaunchApp = onLaunchApp,
          onUnpinApp = onUnpinApp
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // 3. CENTER-RIGHT: Slim Rounded Search / Voice Input Box
        DesktopSlimSearchBox(
          uiState = uiState,
          themeMode = themeMode,
          borderColor = borderColor,
          textPrimary = textPrimary,
          textSecondary = textSecondary,
          onQueryChange = onQueryChange,
          onQuerySubmit = onQuerySubmit,
          onToggleVoice = onToggleVoice,
          keyboardController = keyboardController
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 4. RIGHT SECTION: WEYKY Start Button
        DesktopWekyButton(
          isStartMenuOpen = isStartMenuOpen,
          themeMode = themeMode,
          borderColor = borderColor,
          textPrimary = textPrimary,
          onClick = onToggleStartMenu
        )
      }
    }
  }
}

/**
 * System and Phone Call pill on the far-left of the taskbar.
 * Layout: [ Phone Icon | Time | Battery % | Battery Glyph ]
 */
@Composable
private fun DesktopSystemPill(
  uiState: WeykyUiState,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  textPrimary: Color,
  textSecondary: Color,
  onCallClick: () -> Unit
) {
  val isTactical = themeMode == WeykyThemeMode.TACTICAL
  val isModern = themeMode == WeykyThemeMode.MODERN

  val pillShape = when {
    isTactical -> CutCornerShape(4.dp)
    isModern -> RoundedCornerShape(16.dp)
    else -> RoundedCornerShape(12.dp)
  }

  Row(
    modifier = Modifier
      .height(36.dp)
      .clip(pillShape)
      .background(Color.White.copy(alpha = 0.04f))
      .border(1.dp, borderColor.copy(alpha = 0.25f), pillShape)
      .padding(horizontal = 8.dp, vertical = 2.dp)
      .testTag("desktop_system_tray"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Phone Call / Dialer Button
    IconButton(
      onClick = onCallClick,
      modifier = Modifier
        .size(28.dp)
        .testTag("desktop_call_button")
    ) {
      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Phone Dialer",
        tint = Color(0xFF00F0FF),
        modifier = Modifier.size(16.dp)
      )
    }

    // Live Clock
    val timeText = uiState.currentTimeString.ifBlank { "07:43" }
    Text(
      text = timeText,
      color = textPrimary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = if (isTactical) FontFamily.Monospace else FontFamily.SansSerif
    )

    // Battery percentage & indicator
    val battery = uiState.batterySnapshot
    val percentage = battery?.percentage ?: 36
    val isCharging = battery?.chargingState == ChargingState.CHARGING

    Text(
      text = "$percentage%",
      color = Color(0xFF00F0FF),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      fontFamily = if (isTactical) FontFamily.Monospace else FontFamily.SansSerif
    )

    Icon(
      imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
      contentDescription = "Battery",
      tint = Color(0xFF00F0FF),
      modifier = Modifier.size(15.dp)
    )
  }
}

/**
 * Pinned apps dock on the taskbar with smooth rounded icons and active cyan indicator.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DesktopPinnedAppsDock(
  pinnedPackages: List<String>,
  installedApps: List<AppMatch>,
  context: Context,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  textPrimary: Color,
  onLaunchApp: (AppMatch) -> Unit,
  onUnpinApp: (String) -> Unit
) {
  val itemShape = RoundedCornerShape(10.dp)

  Row(
    modifier = Modifier
      .horizontalScroll(rememberScrollState())
      .testTag("desktop_pinned_apps_tray")
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    for (pkg in pinnedPackages) {
      val appMatch = installedApps.firstOrNull { it.packageName == pkg }
      val appName = appMatch?.label ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
      val iconBitmap = remember(pkg) { AppIconCache.getIcon(context, pkg) }

      var showMenu by remember { mutableStateOf(false) }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(itemShape)
            .background(Color.White.copy(alpha = 0.05f))
            .combinedClickable(
              onClick = {
                if (appMatch != null) {
                  onLaunchApp(appMatch)
                } else {
                  val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                  if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                  }
                }
              },
              onLongClick = {
                showMenu = true
              }
            )
            .padding(2.dp)
            .testTag("pinned_app_$pkg"),
          contentAlignment = Alignment.Center
        ) {
          if (iconBitmap != null) {
            Image(
              bitmap = iconBitmap,
              contentDescription = appName,
              modifier = Modifier.size(26.dp)
            )
          } else {
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(borderColor.copy(alpha = 0.25f))
                .border(1.dp, borderColor.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = appName.take(1).uppercase(),
                color = textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("إزالة من شريط المهام") },
              onClick = {
                showMenu = false
                onUnpinApp(pkg)
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  tint = Color.Red
                )
              }
            )
          }
        }

        // Active app indicator light bar
        Box(
          modifier = Modifier
            .padding(top = 1.dp)
            .width(12.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color(0xFF00F0FF).copy(alpha = 0.6f))
        )
      }
    }
  }
}

/**
 * Slim, rounded pill search bar directly in the taskbar.
 * Matching the reference image: [ ...Ask WEYKY   🎙️ ]
 */
@Composable
private fun DesktopSlimSearchBox(
  uiState: WeykyUiState,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  textPrimary: Color,
  textSecondary: Color,
  onQueryChange: (String) -> Unit,
  onQuerySubmit: (String) -> Unit,
  onToggleVoice: () -> Unit,
  keyboardController: SoftwareKeyboardController?
) {
  val isTactical = themeMode == WeykyThemeMode.TACTICAL
  val boxShape = RoundedCornerShape(16.dp)
  val activeAccent = if (uiState.isVoiceListening) JarvisHudColors.MagentaPulse else borderColor

  Row(
    modifier = Modifier
      .height(34.dp)
      .widthIn(min = 180.dp, max = 280.dp)
      .clip(boxShape)
      .background(Color(0xFF030A14).copy(alpha = 0.85f))
      .border(1.dp, activeAccent.copy(alpha = if (uiState.isVoiceListening) 0.9f else 0.45f), boxShape)
      .padding(horizontal = 6.dp)
      .testTag("home_bottom_bar"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Input text field with "...Ask WEYKY" hint
    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 6.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      if (uiState.inputQuery.isEmpty()) {
        val hint = if (uiState.isVoiceListening) "تكلم الآن..." else "...Ask WEYKY"
        Text(
          text = hint,
          fontSize = 12.sp,
          color = if (uiState.isVoiceListening) JarvisHudColors.MagentaPulse else Color(0xFF6E8299),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      BasicTextField(
        value = uiState.inputQuery,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
          color = textPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(borderColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
          if (uiState.inputQuery.isNotBlank()) {
            onQuerySubmit(uiState.inputQuery)
            keyboardController?.hide()
          }
        }),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ask_weyky_input")
      )
    }

    // Voice Mic button inside search pill
    IconButton(
      onClick = onToggleVoice,
      modifier = Modifier
        .size(26.dp)
        .testTag("voice_input_button")
    ) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = if (uiState.isVoiceListening) "Listening..." else "Voice Input",
        tint = if (uiState.isVoiceListening) JarvisHudColors.MagentaPulse else Color(0xFF00F0FF),
        modifier = Modifier.size(16.dp)
      )
    }

    // Send button if text is entered
    if (uiState.inputQuery.isNotBlank()) {
      IconButton(
        onClick = {
          onQuerySubmit(uiState.inputQuery)
          keyboardController?.hide()
        },
        modifier = Modifier
          .size(26.dp)
          .testTag("query_submit_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send",
          tint = Color(0xFF00F0FF),
          modifier = Modifier.size(15.dp)
        )
      }
    }
  }
}

/**
 * WEYKY Start Button on the right of the taskbar.
 * Layout: [ WEYKY  🌐 ]
 */
@Composable
private fun DesktopWekyButton(
  isStartMenuOpen: Boolean,
  themeMode: WeykyThemeMode,
  borderColor: Color,
  textPrimary: Color,
  onClick: () -> Unit
) {
  val buttonShape = RoundedCornerShape(16.dp)
  val activeBg = if (isStartMenuOpen) {
    borderColor.copy(alpha = 0.35f)
  } else {
    Color.White.copy(alpha = 0.05f)
  }

  val activeBorder = if (isStartMenuOpen) {
    Color(0xFF00F0FF)
  } else {
    Color(0xFF00F0FF).copy(alpha = 0.45f)
  }

  Row(
    modifier = Modifier
      .height(34.dp)
      .clip(buttonShape)
      .background(activeBg)
      .border(1.dp, activeBorder, buttonShape)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 2.dp)
      .testTag("desktop_start_button"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = "WEYKY",
      color = textPrimary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp
    )

    // WEYKY Circular Emblem
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0xFF00F0FF).copy(alpha = 0.45f),
              Color(0xFF0055FF).copy(alpha = 0.20f),
              Color.Transparent
            )
          )
        )
        .border(1.dp, Color(0xFF00F0FF), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "WEYKY Logo",
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
