package ru.konditer_class.catalog.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_SHORT
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.content_loading
import kotlinx.android.synthetic.main.app_bar_main_list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.MediaType
import okhttp3.RequestBody
import org.koin.android.ext.android.inject
import org.threeten.bp.*
import ru.konditer_class.catalog.BuildConfig
import ru.konditer_class.catalog.BuildConfig.BUILD_TYPE
import ru.konditer_class.catalog.BuildConfig.DEBUG
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.api.Api
import ru.konditer_class.catalog.data.KonditerDatabase
import ru.konditer_class.catalog.data.User
import ru.konditer_class.catalog.screens.main.MainListActivity
import ru.konditer_class.catalog.screens.main.SimpleUserActivity
import timber.log.Timber.d
import timber.log.Timber.e
import java.io.File


class LoginActivity : AppCompatActivity() {

    companion object {
        const val LAST_LOGIN = "LAST_LOGIN"
        const val LAST_PASSWORD = "LAST_PASSWORD"
        const val KEY_REQUEST_TIMEOUT = 60L

        var lastUserId: String? = null
        var lastUser: User? = null
        var loadDataOnLogin: Boolean = true
        var lastKeyRequestAt: Long = 0
    }

    val api: Api by inject()
    val prefs get() = PreferenceManager.getDefaultSharedPreferences(this)!!
    private val db: KonditerDatabase by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        val loadDataOnFirstLogin = intent.getBooleanExtra(LOAD_DATA_ON_FIRST_LOGIN, true)

        d("onCreate 1 user $lastUser loadDataOnLogin $loadDataOnLogin savedInstanceState $savedInstanceState")

//        if (loadDataOnLogin && intent.hasExtra(LOAD_DATA_ON_FIRST_LOGIN)) {
//            loadDataOnLogin = intent.getBooleanExtra(LOAD_DATA_ON_FIRST_LOGIN, true)
//        }

        showProgress(false)
        btSignIn.setOnClickListener {
            onSingInClicked()
        }

        requestKey.setOnClickListener {
            onRequestKeyButtonClicked()
        }


        if (savedInstanceState == null) {
            val login: String? = prefs.getString(LAST_LOGIN, null)
            val password: String? = prefs.getString(LAST_PASSWORD, null)

            etLogin.setText(login)
            etPassword.setText(password)
        }
    }

    override fun onStart() {
        super.onStart()
        d("onStart user $lastUser ")
    }


    override fun onResume() {
        super.onResume()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        d("onResume user $lastUser  clipboard text: ${clipboard.text}")

        if (lastUser != null) {
            val copied = clipboard.text?.toString()
            val isDigit = copied?.all { it.isDigit() } ?: false
            if (!copied.isNullOrEmpty() && isDigit) {
                keyET.setText(copied)
                onSingInClicked()
            } else {
                keyET.text = null
                keyLayout.requestFocus()
            }
        } else {
            keyET.text = null
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            keyLayout.post {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }

//        if (DEBUG) {
//            etLogin.setText("Куприн")
//            etPassword.setText("ssDD589+")
//
//            MainScope().launch {
////                withContext(IO) {
////                    prefs.edit().clear()
////                    clearDeviceCacheFolder()
////                }
//                keyET.setText(getValidKey())
//                onRequestKeyButtonClicked()
////                test()
//            }
//        }
    }

    override fun onPause() {
        super.onPause()
        d("onPause user $lastUser ")
    }

    override fun onStop() {
        super.onStop()
        d("onStop user $lastUser")
    }

    override fun onDestroy() {
        super.onDestroy()
        d("onDestroy user $lastUser")
    }


    fun closeKeyboard() {
        etLogin.closeKeyboard()
    }


    @SuppressLint("CheckResult")
    fun test() {
        val userId = "z582a2b-36d444"

        api.getOstTov(RequestBody.create(MediaType.parse("text/plain"), "${userId}&${BuildConfig.VERSION_NAME}"))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ listOfUsers ->
                d("test getOstTov success. Response: ${listOfUsers.size}")
            }, {
                d("test getOstTov failure.")
            })

    }

    private fun showProgress(show: Boolean) {
        login_form.visibility = if (show) View.GONE else View.VISIBLE
        if (show) content_loading.show()
        else content_loading.hide()
    }


    fun onRequestKeyButtonClicked() {
        closeKeyboard()
        val login = etLogin.text?.toString()
        val pwd = etPassword.text?.toString()

        if (login.isNullOrEmpty()) {
            Snackbar.make(requestKey, "Введите логин", LENGTH_SHORT).show()
            return
        }

        if (pwd.isNullOrEmpty()) {
            Snackbar.make(requestKey, "Введите пароль", LENGTH_SHORT).show()
            return
        }

//        todo remove?
//        val now = System.currentTimeMillis()
//        val alreadySent = (now - lastKeyRequestAt) < KEY_REQUEST_TIMEOUT * 1000
//        if (alreadySent) {
//            val toNextRequest = KEY_REQUEST_TIMEOUT - (now - lastKeyRequestAt) / 1000
//            d("onRequestKeyButtonClicked alreadySent $alreadySent toNextRequest $toNextRequest")
//            Snackbar.make(requestKey, "Ключ отправлен. Повторная отправка чере ${toNextRequest} сек.", LENGTH_SHORT).show()
//            return
//        }
//        d("onRequestKeyButtonClicked lastKeyRequestAt $lastKeyRequestAt ${now - lastKeyRequestAt}")
//        lastKeyRequestAt = now

        requestId(login, pwd)
    }

    @SuppressLint("CheckResult")
    fun requestId(login: String, pwd: String) {
        d("requestId start: $login $pwd")
        showProgress(true)
        api.getIdUser(RequestBody.create(MediaType.parse("text/plain"), "$login&$pwd"))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ listOfUsers ->
//                d("requestId success. Response: $listOfUsers")
                if (listOfUsers.isEmpty()) {
                    Snackbar.make(requestKey, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                    lastKeyRequestAt = -1
                } else {
                    val user: User = listOfUsers[0]
                    lastUser = user
                    requestKey(user.id)

                    prefs.edit().putString(LAST_LOGIN, login).apply()
                    prefs.edit().putString(LAST_PASSWORD, pwd).apply()
                }
            }, {
                d("requestId failure.")
                showProgress(false)
                Snackbar.make(requestKey, "Что-то пошло не так, код ошибки: 101", Toast.LENGTH_SHORT).show()
                lastKeyRequestAt = -2
            })
    }


    @SuppressLint("CheckResult")
    fun requestKey(id: String) {
        d("requestKey start: $id")
        api.sendTelegramKey(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                d("requestKey success. response: '${response.string()}'")
                showProgress(false)
                Snackbar.make(requestKey, "Ключ отправлен в telegram", Toast.LENGTH_SHORT).show()

//              requestKey.isEnabled = false

                if (BUILD_TYPE == "debug") {
                    onSingInClicked()
                } else {
                    openTelegramWith("http://t.me/KK2022bot")
                }

//                todo remove?
//                MainScope().launch {
//                    try {
//                        var counter = KEY_REQUEST_TIMEOUT
//                        while (counter > 0) {
//                            counter -= 1
//                            val toNextRequest = KEY_REQUEST_TIMEOUT + 1 - (System.currentTimeMillis() - lastKeyRequestAt) / 1000
//                            requestKey.text = "След. запрос через $toNextRequest сек"
//                            delay(1000)
//                        }
//                        requestKey.text = this@LoginActivity.resources.getString(R.string.action_request_key)
//                        requestKey.isEnabled = true
//                    } catch (e: Exception) {
//
//                    }
//                }

            }, {
                d("requestKey failure.")
                showProgress(false)
                Snackbar.make(requestKey, "Что-то пошло не так, код ошибки: 102", Toast.LENGTH_SHORT).show()
                lastKeyRequestAt = -3
            })
    }


    @SuppressLint("CheckResult")
    fun onSingInClicked() {
//        d("onSingInClicked user ${lastUser}")
        closeKeyboard()
        val login = etLogin.text?.toString()
        val pwd = etPassword.text?.toString()
        val key = keyET.text?.toString()

        if (login.isNullOrEmpty()) {
            Snackbar.make(requestKey, "Введите логин", LENGTH_SHORT).show()
            return
        }

        if (pwd.isNullOrEmpty()) {
            Snackbar.make(requestKey, "Введите пароль", LENGTH_SHORT).show()
            return
        }

        if (key.isNullOrEmpty()) {
            Snackbar.make(requestKey, "Введите ключ из telegram", LENGTH_SHORT).show()
            return
        }
        val validKey = getValidKey()
        if (key != validKey) {
//            d("Key is not valid. Given key: $key validKey $validKey")
            Snackbar.make(requestKey, "Ключ неверный", LENGTH_SHORT).show()
            return
        } else {
            d("Key is valid. Given key: $key ")
        }

        val user = lastUser

        if (user == null) {
            Snackbar.make(requestKey, "Запросите новый ключ", LENGTH_SHORT).show()
            return
        }
        checkAccountStatus(user)

    }


    @SuppressLint("CheckResult")
    fun checkAccountStatus(user: User) {
//        d("checkAccountStatus user $user")
        showProgress(true)
        api.getAccountStatus(user.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val responseCode = response.string()
                d("checkAccountStatus success. response: '${responseCode}'")
                showProgress(false)
                when (responseCode) {
                    "1" -> login(user)
                    "2" -> showAskManagerDialog()
                    "3" -> onDeleteLocalDatabase()
                    else -> {
                        e("checkAccountStatus. Wrong responseCode: '${responseCode}'")
                        Snackbar.make(requestKey, "Что-то пошло не так, код ошибки: 104", Toast.LENGTH_SHORT).show()
                    }
                }
            }, {
                d("checkAccountStatus failure.")
                showProgress(false)
                Snackbar.make(requestKey, "Что-то пошло не так, код ошибки: 103", Toast.LENGTH_SHORT).show()
            })
    }


    fun showAskManagerDialog() {
        d("showAskManagerDialog")
        AlertDialog.Builder(this)
            .setMessage("Обратитесь к менеджеру, следует уточнить некоторые вопросы")
            .setPositiveButton("ОК") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun onDeleteLocalDatabase() {
        d("onDeleteLocalDatabase")
        CoroutineScope(IO).launch {
            try {
                db.clearAllTables()
            } catch (e: Exception) {
                e(e, "clearAllTables failed")
            }
        }
        AlertDialog.Builder(this)
            .setMessage("Вход в приложение не возможен. Производится обслуживание сервера. Попробуйте позже")
            .setPositiveButton("ОК") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    fun login(user: User) {
//        d("login user $user")
        val id = user.id

        prefs.edit().putString("code", id).apply()

        val intent = Intent(
            this,
            if (id.startsWith("z")) MainListActivity::class.java
            else SimpleUserActivity::class.java
        ).apply {
            putExtra("code", id)
        }
        lastUser = null
        startActivity(intent)
    }

    /**

    1. 353 День в году
    2. 999 - Дн = 646
    3. 9 часов.
    4. 94 - 9 = 85
    5. 6 минут
    6. Целое(6 делим 10) = 0

    646850

    От 1 до 5 цикл
    6 + 4 + 6 + 8 = 24

    24 > 9 тогда

    2 + 4 = 6

    6468506


    в цикле складываются только четные цифры
    999, и 94 константы
    Время строго московское,  если из другого региона,  то все равно берём по Москве

     */

    fun isKeyValid(key: String): Boolean {
        return key == getValidKey()
    }

    fun getValidKey(): String {
        val moscowTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"))
//        d("getValidKey start. moscowTime $moscowTime")

        val first = 999 - moscowTime.dayOfYear
        val second = 94 - moscowTime.hour
        val third = moscowTime.minute / 10
//        d("getValidKey intermediate $first $second $third")
        val intermediate = "$first$second$third"
        var sum = 0

        intermediate.take(5).forEach { char ->
            val value = char.toString().toInt()
            if (value.isEven()) {
                sum += value
//                d("getValidKey add even $char sum $sum")
            } else {
//                d("getValidKey drop odd $char")
            }
        }
//        d("getValidKey sum $sum")
        var result = ""

        if (sum > 9) {
            var sum2 = 0
            sum.toString().forEach { char ->
                val value = char.toString().toInt()
                sum2 += value
//                d("getValidKey. add value $value sum2 $sum2")
            }
            result = "$intermediate$sum2"
//            d("getValidKey. sum > 9 $result")
        } else {
            result = "$intermediate$sum"
//            d("getValidKey. sum <= 9 $result")
        }
        return result
    }


    fun Int.isEven() = this % 2 == 0


    fun openTelegramWith(path: String) {
        try {
            val uri = Uri.parse(path)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("org.telegram.messenger")
            startActivity(intent)
        } catch (ex: Exception) {
            e(Exception(ex), "Telegram open failed")
            val name = path.substringAfterLast("/")
            copyToClipboard(name)
            Snackbar.make(btSignIn, "Установите телеграм", Snackbar.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(copiedText: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(copiedText, copiedText)
        clipboard.primaryClip = clip
    }

    fun View.closeKeyboard() {
        val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun View.openKeyboard() {
        requestFocus()
        val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }


    suspend fun clearDeviceCacheFolder() {
        File(cacheDir.absolutePath).listFiles()?.forEach { it.deleteRecursively() }
        e("Cache folder deleted. ")
    }
}
