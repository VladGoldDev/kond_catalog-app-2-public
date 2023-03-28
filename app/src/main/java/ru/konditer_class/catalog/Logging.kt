package ru.konditer_class.catalog

import android.content.Context
import android.os.Environment
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import timber.log.Timber.e
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat


/**
 * Created by @acrrono on 21.01.2023.
 */


object Logger {

    val dirFile = File("/data/data/${BuildConfig.APPLICATION_ID}/cache/logs/")
    val time = System.currentTimeMillis()
    val logFile = File(dirFile, "logs_$time.txt")

    init {
//        if (BuildConfig.DEBUG) {
//            dirFile.mkdirs()
//            logFile.createNewFile()
//            writeToFile("Time: ${LocalDateTime.now()} \n")
//        }
    }

    fun writeLogs() {
        val filename = File(Environment.getExternalStorageDirectory().toString() + "logcat_${System.currentTimeMillis()}.log")
        filename.createNewFile()
        val cmd = "logcat -d -f${filename.absolutePath}"
        Runtime.getRuntime().exec(cmd)
    }


    fun startLogsToFile() {
        val logDirectory = File("/data/data/${BuildConfig.APPLICATION_ID}/cache/logcat/")


        try {
            // create log folder
            if (logDirectory.exists()) {
                logDirectory.listFiles().forEach {
                    it.delete()
                }
            }
        } catch (e: Exception) {
            e("", Exception(e))
        }


        val logFile = File(
            logDirectory,
            "logcat-${SimpleDateFormat("MM-dd-yyyy-HH-mm-ss").format(System.currentTimeMillis())}.txt"
        )

        try {
            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir()
            }
        } catch (e: Exception) {
            e("", Exception(e))
            return
        }

        try {
            Runtime.getRuntime().exec("logcat -c")
            Runtime.getRuntime().exec("logcat -f $logFile")
        } catch (e: IOException) {
            e("", Exception(e))
        }
    }


    fun logD(message: Any) {
        writeToFile(message.toString())
    }

    fun logE(message: String) {
        writeToFile(message)
    }

    fun logE(e: Throwable) {
        writeToFile("\n")
        writeToFile(e.stackTraceToString())
    }

    fun logE(e: Throwable, message: String) {
        writeToFile("\n")
        writeToFile(message)
        writeToFile(e.stackTraceToString())
    }

    fun writeToFile(message: String) {
        if (BuildConfig.DEBUG) {
            try {
                logFile.appendBytes("$message\n".toByteArray())
            } catch (e: Exception) {
                e("writeToFile failed", Exception(e))
            }
        }
    }

    fun Throwable.stackTraceToString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }


}