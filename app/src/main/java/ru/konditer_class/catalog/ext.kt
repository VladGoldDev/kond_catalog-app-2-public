package ru.konditer_class.catalog

import android.content.Context
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import okhttp3.ResponseBody
import ru.konditer_class.catalog.data.WholeTov
import java.io.*
import java.io.File.separator


fun BottomSheetBuilder.addItems(list: List<String>) : BottomSheetBuilder {
    setMode(BottomSheetBuilder.MODE_LIST)
    expandOnStart(true)
    list.forEachIndexed { index, s ->
        addItem(index,s,null)
    }
    return this
}

fun Context.writeResponseBodyToDisk(body: ResponseBody, name: String): Boolean {
    try {
        val file = File("${getExternalFilesDir(null)}$separator$name")

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val fileReader = ByteArray(4096)

            val fileSize = body.contentLength()
            var fileSizeDownloaded: Long = 0

            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            while (true) {
                val read = inputStream!!.read(fileReader)

                if (read == -1) {
                    break
                }

                outputStream.write(fileReader, 0, read)

                fileSizeDownloaded += read.toLong()
            }

            outputStream.flush()

            return true
        } catch (e: IOException) {
            return false
        } finally {
            if (inputStream != null) {
                inputStream.close()
            }

            if (outputStream != null) {
                outputStream.close()
            }
        }
    } catch (e: IOException) {
        return false
    }

}

inline fun <reified T : Any> Any.inject(name: String = "")
        = kotlin.lazy { (org.koin.standalone.StandAloneContext.getKoin().koinContext).get<T>(name) }

fun MutableList<WholeTov>.withDividers() : MutableList<WholeTov> = this.apply {
    if (isEmpty() || this[0].isDividerStub())
        return@apply
    add(0,WholeTov.createDividerStub(this[0].grCode, this[0].grName))
    var i = 1
    while (i<size) {
        if (this[i].grCode != this[i-1].grCode) {
            add(i, WholeTov.createDividerStub(this[i].grCode, this[i].grName))
            i++
        }
        i++
    }
}

inline fun <T> Iterable<T>.sumByFloat(selector: (T) -> Float): Float {
    var sum = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}