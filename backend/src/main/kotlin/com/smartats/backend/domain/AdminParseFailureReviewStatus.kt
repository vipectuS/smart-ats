package com.smartats.backend.domain

enum class AdminParseFailureReviewStatus {
    UNREVIEWED,
    NEEDS_CANDIDATE_UPDATE,
    APPROVED_FOR_RETRY,
    NO_FURTHER_ACTION,
}