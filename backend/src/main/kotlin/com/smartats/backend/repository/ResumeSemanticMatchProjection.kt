package com.smartats.backend.repository

import java.util.UUID

interface ResumeSemanticMatchProjection {
    fun getResumeId(): UUID
    fun getCosineDistance(): Double
}