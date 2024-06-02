package org.example.chatkopring.common.exception

class InvalidInputException(
    val fieldName: String = "",
    message: String = "Invalid Input"
): RuntimeException(message) {
}