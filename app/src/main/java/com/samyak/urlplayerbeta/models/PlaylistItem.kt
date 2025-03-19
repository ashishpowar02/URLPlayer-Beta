package com.samyak.urlplayerbeta.models

data class PlaylistItem(
    val title: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null
)