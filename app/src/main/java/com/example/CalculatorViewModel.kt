package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryItem(
    val id: Long,
    val expression: String,
    val result: String
)

enum class CalculatorTheme(val displayName: String) {
    CARBON_SLATE("Carbon Slate"),
    SOLAR_POP("Solar Pop"),
    RETRO_HACKER("Retro Hacker"),
    LAVENDER_DREAM("Lavender Dream")
}

data class CalculatorUiState(
    val expression: String = "",
    val livePreview: String = "",
    val isErrorState: Boolean = false,
    val errorMessage: String = "",
    val history: List<HistoryItem> = emptyList(),
    val showHistory: Boolean = false,
    val currentTheme: CalculatorTheme = CalculatorTheme.CARBON_SLATE
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var historyCounter = 0L

    // For keeping track of the last committed result so we can clear/extend it appropriately
    private var justEvaluated = false

    fun onDigitClick(digit: String) {
        _uiState.update { state ->
            val newExpr = if (justEvaluated || state.isErrorState) {
                justEvaluated = false
                digit
            } else {
                state.expression + digit
            }
            state.copy(
                expression = newExpr,
                isErrorState = false,
                errorMessage = "",
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onOperatorClick(op: String) {
        _uiState.update { state ->
            if (state.isErrorState) {
                return@update state
            }
            
            val currentExpr = state.expression
            val newExpr = if (currentExpr.isEmpty()) {
                if (op == "-") "-" else "" // Safe standard: allow leading negative
            } else {
                val lastChar = currentExpr.last().toString()
                if (isOperatorChar(lastChar)) {
                    // Replace previous operator with new operator
                    currentExpr.dropLast(1) + op
                } else {
                    justEvaluated = false
                    currentExpr + op
                }
            }

            state.copy(
                expression = newExpr,
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onDecimalClick() {
        _uiState.update { state ->
            if (justEvaluated || state.isErrorState) {
                justEvaluated = false
                return@update state.copy(
                    expression = "0.",
                    isErrorState = false,
                    errorMessage = "",
                    livePreview = ""
                )
            }

            val currentExpr = state.expression
            if (currentExpr.isEmpty()) {
                return@update state.copy(expression = "0.", livePreview = "")
            }

            // Find the last segment (token) to ensure it doesn't already contain a decimal point
            val lastToken = currentExpr.split(Regex("[+\\-×÷()]")).lastOrNull() ?: ""
            val newExpr = if (!lastToken.contains(".")) {
                currentExpr + "."
            } else {
                currentExpr
            }

            state.copy(
                expression = newExpr,
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onClearClick() {
        _uiState.update { state ->
            justEvaluated = false
            state.copy(
                expression = "",
                livePreview = "",
                isErrorState = false,
                errorMessage = ""
            )
        }
    }

    fun onBackspaceClick() {
        _uiState.update { state ->
            if (state.isErrorState) {
                justEvaluated = false
                return@update state.copy(expression = "", isErrorState = false, errorMessage = "", livePreview = "")
            }

            if (state.expression.isNotEmpty()) {
                val newExpr = state.expression.dropLast(1)
                state.copy(
                    expression = newExpr,
                    livePreview = generateLivePreview(newExpr)
                )
            } else {
                state
            }
        }
    }

    fun onBracketClick() {
        _uiState.update { state ->
            val expr = if (justEvaluated || state.isErrorState) "" else state.expression
            justEvaluated = false

            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }

            val newExpr = if (expr.isEmpty()) {
                "("
            } else {
                val lastChar = expr.last()
                val isLastDigitOrClose = lastChar.isDigit() || lastChar == ')' || lastChar == '%'

                if (openCount > closeCount && isLastDigitOrClose) {
                    // Close the bracket if we have open brackets still pending
                    expr + ")"
                } else {
                    // Start a new open bracket. Insert an implicit multiply if preceding content is digit or close paren
                    if (isLastDigitOrClose || lastChar == '.') {
                        expr + "×("
                    } else {
                        expr + "("
                    }
                }
            }

            state.copy(
                expression = newExpr,
                isErrorState = false,
                errorMessage = "",
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onPercentClick() {
        _uiState.update { state ->
            if (state.expression.isEmpty() || state.isErrorState) return@update state

            val lastChar = state.expression.last()
            val newExpr = if (lastChar.isDigit() || lastChar == ')') {
                state.expression + "%"
            } else {
                state.expression
            }

            state.copy(
                expression = newExpr,
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onSignToggleClick() {
        _uiState.update { state ->
            if (state.isErrorState || state.expression.isEmpty()) return@update state

            // Parse out the last number/token block to toggle its unary minus prefix
            val expr = state.expression
            
            // Check if full expression is just a single number
            val numericVal = expr.toDoubleOrNull()
            val newExpr = if (numericVal != null) {
                if (expr.startsWith("-")) {
                    expr.substring(1)
                } else {
                    "-$expr"
                }
            } else {
                // Otherwise find last operator index and toggle sign of trailing segment
                val lastOpIdx = expr.lastIndexOfAny(charArrayOf('+', '-', '×', '÷', '('))
                if (lastOpIdx == -1) {
                    "-$expr"
                } else if (lastOpIdx == expr.length - 1) {
                    // Preceding was operator, appending minus
                    expr + "-"
                } else {
                    // Toggle the element after the operator
                    val prefix = expr.substring(0, lastOpIdx + 1)
                    val suffix = expr.substring(lastOpIdx + 1)
                    if (suffix.startsWith("-")) {
                        prefix + suffix.substring(1)
                    } else if (prefix.endsWith("-") && lastOpIdx == prefix.length - 1 && prefix.length > 1 && isOperatorChar(prefix[prefix.length - 2].toString())) {
                        // Special negative handling backoff
                        prefix.dropLast(1) + suffix
                    } else {
                        prefix + "-" + suffix
                    }
                }
            }

            state.copy(
                expression = newExpr,
                livePreview = generateLivePreview(newExpr)
            )
        }
    }

    fun onEqualClick() {
        _uiState.update { state ->
            if (state.expression.isBlank()) return@update state

            try {
                // Evaluate
                val doubleRes = CalculatorEngine.evaluate(state.expression)
                val formattedRes = CalculatorEngine.formatResult(doubleRes)

                // Save to history
                val updatedHistory = listOf(
                    HistoryItem(
                        id = ++historyCounter,
                        expression = state.expression,
                        result = formattedRes
                    )
                ) + state.history

                justEvaluated = true

                state.copy(
                    expression = formattedRes,
                    livePreview = "",
                    isErrorState = false,
                    errorMessage = "",
                    history = updatedHistory
                )
            } catch (e: ArithmeticException) {
                state.copy(
                    isErrorState = true,
                    errorMessage = "Divide by zero",
                    livePreview = ""
                )
            } catch (e: Exception) {
                state.copy(
                    isErrorState = true,
                    errorMessage = "Syntax Error",
                    livePreview = ""
                )
            }
        }
    }

    fun onHistoryItemSelect(item: HistoryItem) {
        _uiState.update { state ->
            justEvaluated = false
            state.copy(
                expression = item.expression,
                livePreview = generateLivePreview(item.expression),
                isErrorState = false,
                errorMessage = "",
                showHistory = false
            )
        }
    }

    fun clearHistory() {
        _uiState.update { state ->
            state.copy(history = emptyList(), showHistory = false)
        }
    }

    fun toggleHistory() {
        _uiState.update { state ->
            state.copy(showHistory = !state.showHistory)
        }
    }

    fun changeTheme(theme: CalculatorTheme) {
        _uiState.update { state ->
            state.copy(currentTheme = theme)
        }
    }

    private fun generateLivePreview(expression: String): String {
        if (expression.isBlank()) return ""
        
        // Don't estimate single numbers
        if (expression.toDoubleOrNull() != null) return ""

        // Check if there's any actual operators
        if (!expression.contains(Regex("[+\\-×÷%]"))) return ""

        // Try soft evaluation for background preview without hard-failing mid-typing
        return try {
            val doubleRes = CalculatorEngine.evaluate(expression)
            if (doubleRes.isInfinite() || doubleRes.isNaN()) {
                ""
            } else {
                "= " + CalculatorEngine.formatResult(doubleRes)
            }
        } catch (e: Exception) {
            "" // Silently ignore calculation failures on half-written formulas
        }
    }

    private fun isOperatorChar(char: String): Boolean {
        return char == "+" || char == "-" || char == "×" || char == "÷"
    }
}
