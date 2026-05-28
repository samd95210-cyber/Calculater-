package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CalculatorApp()
            }
        }
    }
}

// Model the layout theme container explicitly
class ThemePalette(
    val bg: Color,
    val displayBg: Color,
    val text: Color,
    val textFaded: Color,
    val keypadBg: Color,
    val digit: Color,
    val onDigit: Color,
    val action: Color,
    val onAction: Color,
    val accent: Color,
    val onAccent: Color,
    val monospaceFont: Boolean = false
)

@Composable
fun getThemePalette(theme: CalculatorTheme): ThemePalette {
    return when (theme) {
        CalculatorTheme.CARBON_SLATE -> ThemePalette(
            bg = CarbonBg,
            displayBg = CarbonDisplayBg,
            text = CarbonText,
            textFaded = CarbonTextFaded,
            keypadBg = CarbonBg,
            digit = CarbonDigit,
            onDigit = CarbonOnDigit,
            action = CarbonAction,
            onAction = CarbonOnAction,
            accent = CarbonAccent,
            onAccent = CarbonOnAccent,
            monospaceFont = false
        )
        CalculatorTheme.SOLAR_POP -> ThemePalette(
            bg = SolarBg,
            displayBg = SolarDisplayBg,
            text = SolarText,
            textFaded = SolarTextFaded,
            keypadBg = SolarBg,
            digit = SolarDigit,
            onDigit = SolarOnDigit,
            action = SolarAction,
            onAction = SolarOnAction,
            accent = SolarAccent,
            onAccent = SolarOnAccent,
            monospaceFont = false
        )
        CalculatorTheme.RETRO_HACKER -> ThemePalette(
            bg = RetroBg,
            displayBg = RetroDisplayBg,
            text = RetroText,
            textFaded = RetroTextFaded,
            keypadBg = RetroBg,
            digit = RetroDigit,
            onDigit = RetroOnDigit,
            action = RetroAction,
            onAction = RetroOnAction,
            accent = RetroAccent,
            onAccent = RetroOnAccent,
            monospaceFont = true
        )
        CalculatorTheme.LAVENDER_DREAM -> ThemePalette(
            bg = LavenderBg,
            displayBg = LavenderDisplayBg,
            text = LavenderText,
            textFaded = LavenderTextFaded,
            keypadBg = LavenderBg,
            digit = LavenderDigit,
            onDigit = LavenderOnDigit,
            action = LavenderAction,
            onAction = LavenderOnAction,
            accent = LavenderAccent,
            onAccent = LavenderOnAccent,
            monospaceFont = false
        )
    }
}

@Composable
fun CalculatorApp(
    viewModel: CalculatorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = getThemePalette(uiState.currentTheme)
    val scope = rememberCoroutineScope()

    // Base text style mapping based on dynamic theme configuration (Retro hacker gets digital look)
    val baseFontFamily = if (palette.monospaceFont) FontFamily.Monospace else FontFamily.SansSerif

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .padding(innerPadding)
        ) {
            
            // 1. Sleek Theme Switcher Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline Theme Buttons Grid
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.displayBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalculatorTheme.values().forEach { t ->
                        val isSelected = uiState.currentTheme == t
                        val activeIndicatorCol = if (isSelected) palette.accent else Color.Transparent
                        val activeTextCol = if (isSelected) palette.onAccent else palette.textFaded

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeIndicatorCol)
                                .clickable { viewModel.changeTheme(t) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("theme_btn_${t.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (t) {
                                    CalculatorTheme.CARBON_SLATE -> "Carbon"
                                    CalculatorTheme.SOLAR_POP -> "Solar"
                                    CalculatorTheme.RETRO_HACKER -> "Retro"
                                    CalculatorTheme.LAVENDER_DREAM -> "Dream"
                                },
                                style = TextStyle(
                                    fontFamily = baseFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = activeTextCol
                                )
                            )
                        }
                    }
                }

                // History Toggle Button
                IconButton(
                    onClick = { viewModel.toggleHistory() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.displayBg)
                        .testTag("toggle_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.desc_history),
                        tint = if (uiState.showHistory) palette.accent else palette.text
                    )
                }
            }

            // Outer wrapper for Display Screen & History Drawer
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(
                        elevation = if (palette.monospaceFont) 0.dp else 4.dp,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.displayBg)
            ) {
                
                // Normal view state content: Interactive display screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    // Small local history scroll track (always visible inline at top of display)
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxWidth()
                    ) {
                        if (uiState.history.isEmpty()) {
                            Text(
                                text = "No history yet",
                                style = TextStyle(
                                    fontFamily = baseFontFamily,
                                    color = palette.textFaded.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        } else {
                            val listState = rememberLazyListState()
                            
                            // Auto scroll history to bottom when new entries arrive
                            LaunchedEffect(uiState.history.size) {
                                if (uiState.history.isNotEmpty()) {
                                    listState.animateScrollToItem(0)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.history.take(4)) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onHistoryItemSelect(item) }
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.expression,
                                            style = TextStyle(
                                                fontFamily = baseFontFamily,
                                                color = palette.textFaded,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "= ${item.result}",
                                            style = TextStyle(
                                                fontFamily = baseFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                color = palette.accent,
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom: Active Expression panel and Real-time estimate
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        
                        // Smart font resizing for long mathematical formulas
                        val expressionLen = uiState.expression.length
                        val exprTextSize = when {
                            expressionLen < 12 -> 48.sp
                            expressionLen < 22 -> 34.sp
                            else -> 22.sp
                        }

                        // Horizontal scrolling display for infinite expressions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState(), reverseScrolling = true),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.expression.isEmpty()) "0" else uiState.expression,
                                style = TextStyle(
                                    fontFamily = baseFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = exprTextSize,
                                    color = if (uiState.expression.isEmpty()) palette.textFaded.copy(alpha = 0.5f) else palette.text,
                                    textAlign = TextAlign.End
                                ),
                                maxLines = 1,
                                modifier = Modifier.testTag("expression_display")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Estimate / Error Panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Backspace button inside display panel
                            IconButton(
                                onClick = { viewModel.onBackspaceClick() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("backspace_button")
                            ) {
                                Text(
                                    text = "⌫",
                                    style = TextStyle(
                                        fontFamily = baseFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.expression.isEmpty()) palette.textFaded.copy(alpha = 0.3f) else palette.accent
                                    )
                                )
                            }

                            // Output
                            Text(
                                text = when {
                                    uiState.isErrorState -> uiState.errorMessage
                                    uiState.livePreview.isNotEmpty() -> uiState.livePreview
                                    else -> ""
                                },
                                style = TextStyle(
                                    fontFamily = baseFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isErrorState) Color(0xFFE57373) else palette.textFaded
                                ),
                                modifier = Modifier.testTag("result_display")
                            )
                        }
                    }
                }

                // Expanded Drawer History Panel
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showHistory,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.displayBg)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Full Session History",
                                style = TextStyle(
                                    fontFamily = baseFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = palette.text
                                )
                            )
                            
                            if (uiState.history.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearHistory() },
                                    modifier = Modifier.testTag("clear_history_sub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.desc_history_clear),
                                        tint = Color(0xFFE57373)
                                    )
                                }
                            }
                        }

                        Divider(color = palette.textFaded.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                        if (uiState.history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "History is empty",
                                    style = TextStyle(
                                        fontFamily = baseFontFamily,
                                        color = palette.textFaded,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.history) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onHistoryItemSelect(item) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = palette.bg.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = item.expression,
                                                style = TextStyle(
                                                    fontFamily = baseFontFamily,
                                                    color = palette.text,
                                                    fontSize = 14.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "= ${item.result}",
                                                style = TextStyle(
                                                    fontFamily = baseFontFamily,
                                                    color = palette.accent,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                ),
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Pad Grid section (Keys Layout)
            Column(
                modifier = Modifier
                    .weight(2.7f)
                    .fillMaxWidth()
                    .background(palette.keypadBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                
                // Rows defining the calculator keys block
                val keys = listOf(
                    listOf("AC", "()", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("±", "0", ".", "=")
                )

                keys.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { label ->
                            val isOperator = label == "÷" || label == "×" || label == "-" || label == "+" || label == "="
                            val isAction = label == "AC" || label == "()" || label == "%" || label == "±"

                            val itemBgColor = when {
                                label == "=" -> palette.accent
                                isOperator -> palette.action.copy(alpha = 0.5f)
                                isAction -> palette.action
                                else -> palette.digit
                            }

                            val itemTextColor = when {
                                label == "=" -> palette.onAccent
                                isOperator -> palette.accent
                                isAction -> palette.accent
                                else -> palette.onDigit
                            }

                            val buttonWeight = if (label == "0" && false) 2f else 1f // Keeping uniform 1f structure for clean grid
                            
                            CellKey(
                                label = label,
                                contentColor = itemTextColor,
                                containerColor = itemBgColor,
                                fontFamily = baseFontFamily,
                                modifier = Modifier
                                    .weight(buttonWeight)
                                    .fillMaxHeight()
                                    .padding(vertical = 5.dp),
                                onClick = {
                                    when (label) {
                                        "AC" -> viewModel.onClearClick()
                                        "()" -> viewModel.onBracketClick()
                                        "%" -> viewModel.onPercentClick()
                                        "÷", "×", "-", "+" -> viewModel.onOperatorClick(label)
                                        "±" -> viewModel.onSignToggleClick()
                                        "." -> viewModel.onDecimalClick()
                                        "=" -> viewModel.onEqualClick()
                                        else -> viewModel.onDigitClick(label)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CellKey(
    label: String,
    contentColor: Color,
    containerColor: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Collect interactions to trigger soft tactile spring scales
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "click_bounce"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = true, color = contentColor.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .testTag("key_pad_$label"),
        contentAlignment = Alignment.Center
    ) {
        // Adjust display sizing based on key content size
        val labelSize = when {
            label.length > 2 -> 15.sp
            label == "()" -> 19.sp
            else -> 22.sp
        }

        Text(
            text = label,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = labelSize,
                color = contentColor
            )
        )
    }
}

// Ensure pre-coded greeting previews compile beautifully
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        CalculatorApp()
    }
}
