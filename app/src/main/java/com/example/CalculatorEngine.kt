package com.example

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Stack

object CalculatorEngine {

    /**
     * Evaluates a math expression string and returns the result or throws an exception.
     * Replaces human-friendly signs with standard compiler-friendly operators.
     */
    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0

        // Normalize string
        val normalized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        return parseAndEvaluate(normalized)
    }

    /**
     * Format Double to a highly clean, readable string.
     * Prevents scientific notation for normal size numbers, removes trailing zeros,
     * and caps precision at 10 decimal places to prevent float-point artifact errors like 0.30000000004
     */
    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Infinity"

        return try {
            val bd = BigDecimal(value.toString())
            // Round to 10 decimal digits to remove floating-point artifacts
            val rounded = bd.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
            
            // If the value is a whole number, represent as integer
            if (rounded.scale() <= 0) {
                rounded.toPlainString()
            } else {
                rounded.toPlainString()
            }
        } catch (e: Exception) {
            // Fallback
            val df = DecimalFormat("#.##########")
            df.format(value)
        }
    }

    private fun parseAndEvaluate(str: String): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw IllegalArgumentException("Unexpected character: " + ch.toChar())
                return x
            }

            // expression = term | expression `+` term | expression `-` term
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) {
                        x += parseTerm() // addition
                    } else if (eat('-'.code)) {
                        x -= parseTerm() // subtraction
                    } else {
                        return x
                    }
                }
            }

            // term = factor | term `*` factor | term `/` factor
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) {
                        x *= parseFactor() // multiplication
                    } else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor // division
                    } else {
                        return x
                    }
                }
            }

            // factor = `+` factor | `-` factor | `(` expression `)` | number | factor `%`
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                        nextChar()
                    }
                    val numStr = str.substring(startPos, this.pos)
                    x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number format: $numStr")
                } else {
                    throw IllegalArgumentException("Expected expression, got: " + ch.toChar())
                }

                // Handle percentage operand suffix (e.g., 50% = 0.5)
                if (eat('%'.code)) {
                    x *= 0.01
                }

                return x
            }
        }.parse()
    }
}
