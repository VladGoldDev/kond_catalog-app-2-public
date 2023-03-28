package com.redmadrobot.gallery.entity

import java.io.File
import java.io.Serializable

data class Media(
    val file: File,
    val type: MediaType,
    val url: String
) : Serializable