package com.example.prokject2_tracker.core.util

import java.util.UUID

object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
