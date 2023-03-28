package ru.konditer_class.catalog

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.konditer_class.catalog.screens.main.MainListActivity
import timber.log.Timber.d
import timber.log.Timber.e


/**
 * Created by @acrrono on 24.12.2022.
 */
class UserInteractionManager(val activity: MainListActivity) {

    companion object {
         const val NO_INTERACTION_LIMIT = 40 * 60 * 1000L
//         const val NO_INTERACTION_LIMIT = 1 * 80 * 1000L
    }

    var lastInteractionAt = System.currentTimeMillis()

    fun ping() {
//        46:15
        lastInteractionAt = System.currentTimeMillis()
        d("ping $lastInteractionAt")
    }


    val interactionWatcher: Job = MainScope().launch {
        while (isRunning) {
            delay(3 * 1000)

            val sinceLastInteraction = System.currentTimeMillis() - lastInteractionAt
            val sinceLastInteractionSeconds = sinceLastInteraction / 1000
            val sinceLastInteractionMinutes = sinceLastInteractionSeconds / 60

            d("startWatcher since last interaction ${sinceLastInteractionMinutes} min ${sinceLastInteractionSeconds} sec ")
            if (sinceLastInteraction > NO_INTERACTION_LIMIT) {
                e("startWatcher force logout ")
                activity.logoutWithoutDialog(false)
                isRunning = false
            }
        }
    }

    var isRunning = true

    fun destroy() {
        isRunning = false
    }

}