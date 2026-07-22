package iti.grad.nutriscan.presentation.common

import android.util.Patterns
import iti.grad.presentation.R

object Validation {
    fun validateEmail(email: String): Int? {
        if (email.isBlank()) {
            return R.string.error_empty_field
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (!email.matches(emailRegex)) {
            return R.string.error_invalid_email
        }
        return null
    }

    fun validatePasswordLength(password: String): Int? {
        if (password.isBlank()) {
            return R.string.error_empty_field
        }
        if (password.length < 8) {
            return R.string.error_empty_field // Change this to a better error later if needed
        }
        return null
    }
}
