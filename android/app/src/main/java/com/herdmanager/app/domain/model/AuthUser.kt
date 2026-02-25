package com.herdmanager.app.domain.model

/**
 * Represents the currently signed-in user (single user per device for MVP).
 */
data class AuthUser(
    val uid: String,
    val email: String?
)
