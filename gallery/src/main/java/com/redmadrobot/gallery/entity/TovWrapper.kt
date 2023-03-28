package com.redmadrobot.gallery.entity

import java.io.Serializable

data class TovWrapper(
    val code: String,
    val name: String,
    val image: Media,
    val price: Float,
    val ost: Int?,
    val showPrice: Boolean = false
) : Serializable