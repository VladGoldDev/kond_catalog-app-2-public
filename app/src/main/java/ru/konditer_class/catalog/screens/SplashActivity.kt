package ru.konditer_class.catalog.screens

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import ru.konditer_class.catalog.screens.main.MainListActivity
import ru.konditer_class.catalog.screens.main.SimpleUserActivity
import timber.log.Timber.d

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(this).getString("code", "")
//        if (DEBUG) {
//            startActivity(Intent(this, LoginActivity::class.java))
//        } else if (code.isNullOrEmpty()) {
        d("getOstTov onCreate current ${CacheHelper.current}")

        if (userId.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(
                Intent(
                    this,
                    if (userId.startsWith("z")) MainListActivity::class.java
                    else SimpleUserActivity::class.java
                ).apply {
                    putExtra("code", userId)
                })
        }
    }
}
