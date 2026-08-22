package com.example.mytracker.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Wraps a click so the soft keyboard is gone before it runs — every "Speichern" uses this. Clearing
 * focus alone isn't enough on all keyboards, and hiding the keyboard alone leaves the field focused
 * so the next tap anywhere brings it straight back, so both happen.
 */
@Composable
fun dismissingKeyboard(action: () -> Unit): () -> Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    return {
        focusManager.clearFocus()
        keyboardController?.hide()
        action()
    }
}
