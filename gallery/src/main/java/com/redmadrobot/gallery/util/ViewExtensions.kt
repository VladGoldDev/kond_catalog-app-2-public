package com.redmadrobot.gallery.util

import android.content.Context.INPUT_METHOD_SERVICE
import android.support.annotation.Dimension
import android.support.annotation.Dimension.DP
import android.util.DisplayMetrics
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager

internal fun View.dpToPx(@Dimension(unit = DP) dp: Float): Float =
    dpToPx(resources.displayMetrics, dp)

private fun dpToPx(displayMetrics: DisplayMetrics, @Dimension(unit = DP) dp: Float) =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        displayMetrics
    )

inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

/** Return an iterator over the collection's values. */
fun <T> SparseArray<T>.valueIterator(): Iterator<T> = object : Iterator<T> {
    var index = 0
    override fun hasNext() = index < size()
    override fun next() = valueAt(index++)
}

fun View.closeKeyboard(){
    val im = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    im.hideSoftInputFromWindow(windowToken, 0)
}

fun View.openKeyboard() {
    requestFocus()
    val im = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    im.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
