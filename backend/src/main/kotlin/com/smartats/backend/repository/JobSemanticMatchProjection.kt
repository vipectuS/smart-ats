package com.smartats.backend.repository

import java.util.UUID

interface JobSemanticMatchProjection {
    fun getJobId(): UUID

    fun getCosineDistance(): Double
}