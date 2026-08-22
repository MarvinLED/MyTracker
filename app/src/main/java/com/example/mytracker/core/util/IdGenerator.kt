package com.example.mytracker.core.util

import java.util.UUID

object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
