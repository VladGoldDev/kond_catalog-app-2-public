package ru.konditer_class.catalog.screens

import android.content.DialogInterface
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.TextView
import com.arellomobile.mvp.MvpAppCompatActivity
import com.travijuu.numberpicker.library.NumberPicker
import kotlinx.android.synthetic.main.dialog_add_tov_to_order.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.INITIAL_ORDER_ID
import ru.konditer_class.catalog.data.OrderTovItem
import ru.konditer_class.catalog.data.WholeTov

abstract class BaseActivity : MvpAppCompatActivity() {

    fun showDialogToAddTovToCurrentOrder(tov: WholeTov, orderTovItem: OrderTovItem?) {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_add_tov_to_order)
            .setPositiveButton("Сохранить") { _, _ -> }
            .setNegativeButton("Отмена") { _, _ -> }
            .create()
        var count = 0
        dialog.apply {
            show()
            val tvOst = findViewById<TextView>(R.id.tvOst)!!
            var initialOst = tov.ost
            val numberPickerSht = findViewById<NumberPicker>(R.id.numberPickerSht)!!
            val numberPickerBlok = findViewById<NumberPicker>(R.id.numberPickerBlok)!!
            val numberPickerUpak = findViewById<NumberPicker>(R.id.numberPickerUpak)!!
            tov.shtK?.let { shtK ->
                groupSht.visibility = View.VISIBLE
                initialOst -= shtK * (orderTovItem?.kolSht ?: 0)
                numberPickerSht.apply {
                    max = tov.ost
                    value = orderTovItem?.kolSht ?: 0
                    setValueChangedListener { value, _ ->
                        count = value
                        val minusShtK = value * shtK
                        val minusBlokK = numberPickerBlok.value * (tov.blokK ?: 0)
                        val minusUpakK = numberPickerUpak.value * (tov.upakK ?: 0)
                        val currentOst = tov.ost - minusShtK - minusBlokK - minusUpakK
                        tvOst.text = "Остаток с учетом заказа (шт.): $currentOst"

                        if (tov.blokK != null)
                            numberPickerBlok.max = currentOst / tov.blokK!! + numberPickerBlok.value
                        if (tov.upakK != null)
                            numberPickerUpak.max = currentOst / tov.upakK!! + numberPickerUpak.value
                    }
                }
            }

            tov.blokK?.let { blokK ->
                groupBlok.visibility = View.VISIBLE
                initialOst -= blokK * (orderTovItem?.kolBlok ?: 0)
                numberPickerBlok.apply {
                    max = tov.ost / blokK
                    value = orderTovItem?.kolBlok ?: 0
                    setValueChangedListener { value, _ ->
                        count = value
                        val minusShtK = numberPickerSht.value * (tov.shtK ?: 0)
                        val minusBlokK = value * blokK
                        val minusUpakK = numberPickerUpak.value * (tov.upakK ?: 0)
                        val currentOst = tov.ost - minusShtK - minusBlokK - minusUpakK
                        tvOst.text = "Остаток с учетом заказа (шт.): $currentOst"

                        if (tov.shtK != null)
                            numberPickerSht.max = currentOst / tov.shtK!! + numberPickerSht.value
                        if (tov.upakK != null)
                            numberPickerUpak.max = currentOst / tov.upakK!! + numberPickerUpak.value
                    }
                }
            }

            tov.upakK?.let { upakK ->
                groupUpak.visibility = View.VISIBLE
                initialOst -= upakK * (orderTovItem?.kolUpak ?: 0)
                numberPickerUpak.apply {
                    max = tov.ost / upakK
                    value = orderTovItem?.kolUpak ?: 0
                    setValueChangedListener { value, _ ->
                        count = value
                        val minusShtK = numberPickerSht.value * (tov.shtK ?: 0)
                        val minusBlokK = numberPickerBlok.value * (tov.blokK ?: 0)
                        val minusUpakK = value * upakK
                        val currentOst = tov.ost - minusShtK - minusBlokK - minusUpakK
                        tvOst.text = "Остаток с учетом заказа (шт.): $currentOst"

                        if (tov.blokK != null)
                            numberPickerBlok.max = currentOst / tov.blokK!! + numberPickerBlok.value
                        if (tov.shtK != null)
                            numberPickerSht.max = currentOst / tov.shtK!! + numberPickerSht.value
                    }
                }
            }

            if (initialOst < 0) {
                numberPickerBlok.value = 0
                numberPickerSht.value = 0
                numberPickerUpak.value = 0
                initialOst = tov.ost
                Snackbar.make(rootDialog, "Превышение остатка на складе. Пересоберите, пожалуйста, заказ", Snackbar.LENGTH_LONG)
                    .show()
            }
            tvOst.text = "Остаток с учетом заказа (шт.): $initialOst"

            setButton(DialogInterface.BUTTON_POSITIVE, "Сохранить") { _, _ ->
                if (count > 0) {
                    val orderTovItemToSave = OrderTovItem(
                        id = INITIAL_ORDER_ID,
                        name = tov.name,
                        tovar = tov.code,
                        kol = 0,
                        kolSht = if (tov.shtK == null) null else numberPickerSht.value,
                        kolUpak = if (tov.upakK == null) null else numberPickerUpak.value,
                        kolBlok = if (tov.blokK == null) null else numberPickerBlok.value,
                        price = tov.price
                    ).apply {
                        kol = (kolSht ?: 0) * (tov.shtK ?: 0) + (kolUpak ?: 0) * (tov.upakK ?: 0) + (kolBlok ?: 0) * (tov.blokK
                            ?: 0)
                    }
                    saveTovItemForCurrentOrder(orderTovItemToSave)
                }
            }
        }
    }

    abstract var comment: String

    abstract fun saveTovItemForCurrentOrder(orderTovItem: OrderTovItem)
}