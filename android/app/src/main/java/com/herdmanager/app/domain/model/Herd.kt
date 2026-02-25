package com.herdmanager.app.domain.model

data class Herd(
    val id: String,
    val name: String,
    val farmId: String,
    val description: String? = null,
    val sortOrder: Int = 0
)
