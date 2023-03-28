package ru.konditer_class.catalog.screens

import android.content.SharedPreferences
import android.preference.PreferenceManager
import ru.konditer_class.catalog.App.Companion.appCtx
import timber.log.Timber.e


/**
 * Created by @acrrono on 21.01.2023.
 */
object CacheHelper {

    val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(appCtx)

    const val KEY_CLEAR_CACHE = "KEY_CLEAR_CACHE"

    val current: Boolean?
        get() = if (prefs.contains(KEY_CLEAR_CACHE)) prefs.getBoolean(KEY_CLEAR_CACHE, true) else null

    fun saveClearCacheFlag(value: Boolean) {
        e("getOstTov saveClearCacheFlag $current -> $value")

        prefs.edit().putBoolean(KEY_CLEAR_CACHE, value).apply()
    }
}