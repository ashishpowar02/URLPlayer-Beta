package com.samyak.urlplayerbeta.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Videos(
    val name: String, 
    val url: String,
    val userAgent: String? = null
) : Parcelable
