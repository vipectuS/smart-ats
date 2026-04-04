package com.smartats.backend.repository

import java.time.LocalDate

interface DailyCountProjection {
    fun getDay(): LocalDate

    fun getTotal(): Long
}