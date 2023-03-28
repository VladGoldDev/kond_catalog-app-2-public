package ru.konditer_class.catalog.screens.current_order

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.redmadrobot.gallery.util.closeKeyboard
import com.redmadrobot.gallery.util.isVisible
import kotlinx.android.synthetic.main.activity_current_order.*
import kotlinx.android.synthetic.main.activity_current_order.commentET
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.item_current_order_tov.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.INITIAL_ORDER_ID
import ru.konditer_class.catalog.data.OrderTovItem
import ru.konditer_class.catalog.data.WholeTov
import ru.konditer_class.catalog.screens.BaseActivity
import ru.konditer_class.catalog.sumByFloat
import timber.log.Timber
import java.text.DecimalFormat

class CurrentOrderActivity : BaseActivity(), CurrentOrderView {

    @InjectPresenter
    lateinit var presenter: CurrentOrderPresenter

    @State
    lateinit var orderId: String

    private val df = DecimalFormat("0.00")

    private val orderAdapter = CurrentOrderAdapter().apply {
        onRequestToChangeListener = { orderTovItem ->
            presenter.loadTovForOrderForDialog(orderTovItem)
        }
        onRequestToRemoveListener = { orderTovItem ->
            presenter.removeOrderItemTovFromCurrentOrder(orderTovItem)
        }
    }

    companion object {
        const val ORDER_ID = "ORDER_ID"
        const val ORDER_COMMENT = "ORDER_COMMENT"
    }

    override var comment = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = if (intent.hasExtra(ORDER_ID)) intent.getStringExtra(ORDER_ID) else INITIAL_ORDER_ID

        StateSaver.restoreInstanceState(this, savedInstanceState)
        setContentView(R.layout.activity_current_order)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (orderId != INITIAL_ORDER_ID) {
            supportActionBar?.title = "Заказ № $orderId"
            orderAdapter.isEditable = false
            commentLayout.isVisible = false

            commentNotEditable.text = "Комментарий: ${intent.extras.getString(ORDER_COMMENT)}"
        } else {
            commentNotEditable.isVisible = false

            commentET.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    val text = p0?.toString() ?: ""
                    comment = text
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {}
            })
        }

        presenter.loadOrderById(orderId)
        recycler.apply {
            adapter = orderAdapter
            addItemDecoration(
                SeparatorDecoration(
                    0f,
                    20f,
                    Color.TRANSPARENT
                )
            )
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        recycler.post {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        commentNotEditable.requestFocus()
    }

    override fun onCurrentOrderLoaded(list: List<OrderTovItem>) {
        tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        tvSumZ.visibility = if (list.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (list.isNotEmpty()) {
            tvSumZ.text = "Общая стоимость заказа: ${df.format(list.sumByFloat { it.kol * it.price })} \u20BD"
        } else {
            commentLayout.isVisible = false
        }
        orderAdapter.submitList(list)
    }

    override fun onOpenOrderDialog(tov: WholeTov, orderTovItem: OrderTovItem?) {
        showDialogToAddTovToCurrentOrder(tov, orderTovItem)
    }

    override fun onTovItemForCurrentOrderSaved() {
        Snackbar.make(root, "Товар сохранен в корзине", Snackbar.LENGTH_SHORT).show()
    }

    override fun saveTovItemForCurrentOrder(orderTovItem: OrderTovItem) {
        presenter.saveTovItemForCurrentOrder(orderTovItem)
    }

    override fun onOrderSent() {
        sendingOrderDialog?.dismiss()
        commentLayout.isVisible = false
        AlertDialog.Builder(this)
            .setMessage("Заказ отправлен")
            .setCancelable(false)
            .setPositiveButton("OK") { dialogInterface, i ->
                dialogInterface.dismiss()
                onBackPressed()
            }.show()
    }

    override fun onOrderSendError() {
        sendingOrderDialog?.dismiss()
        AlertDialog.Builder(this)
            .setMessage("Произошла ошибка при отправке заказа, попробуйте позже")
            .setCancelable(false)
            .setPositiveButton("OK") { dialogInterface, i ->
                dialogInterface.dismiss()
                onBackPressed()
            }.show()
//        Snackbar.make(root, "Произошла ошибка при отправке заказа, попробуйте позже", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (orderId == INITIAL_ORDER_ID)
            menuInflater.inflate(R.menu.menu_current_order, menu)
        return true
    }

    var sendingOrderDialog: AlertDialog? = null
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_send_order -> {

                val isSent = presenter.sendCurrentOrderToServer(
                    commentLayout,
                    PreferenceManager.getDefaultSharedPreferences(this).getString("code", ""),
                    comment
                )

                if (isSent) {
                    commentLayout.closeKeyboard()

                    sendingOrderDialog = AlertDialog.Builder(this)
                        .setMessage("Отправляем заказ")
                        .setCancelable(false)
                        .show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null)
            StateSaver.saveInstanceState(this, outState)
    }
}