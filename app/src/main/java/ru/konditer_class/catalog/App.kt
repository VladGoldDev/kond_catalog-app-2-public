package ru.konditer_class.catalog

import android.app.Application
import android.arch.persistence.room.Room
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.startKoin
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module
import ru.konditer_class.catalog.Logger.startLogsToFile
import ru.konditer_class.catalog.api.ApiCreator
import ru.konditer_class.catalog.data.KonditerDatabase
import timber.log.Timber
import timber.log.Timber.*


class App : Application() {

    companion object {
        var appOrNull: Application? = null
        val appCtx: Application get() = appOrNull!!
        const val TEST = true
    }

    override fun onCreate() {
        super.onCreate()
        appOrNull = this

        val mainModule = module {
            single { ApiCreator().create() }

            single {
                Room.databaseBuilder(androidApplication(), KonditerDatabase::class.java, "konditer-db-1")
                    .build()
            }
        }

        startKoin(this, listOf(mainModule))
        Fabric.with(this, Crashlytics())
        AndroidThreeTen.init(this)
        Timber.plant(DebugTree())
        startLogsToFile()

        MainScope().launch {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@App)
            val allEntries: MutableMap<String, *> = prefs.all
            e("App start.")
            e("Preferences start ${allEntries.size}")
            allEntries.forEach { line ->
                d("${line.key} ${line.value}")
            }
            e("Preferences end")
        }
    }

}
