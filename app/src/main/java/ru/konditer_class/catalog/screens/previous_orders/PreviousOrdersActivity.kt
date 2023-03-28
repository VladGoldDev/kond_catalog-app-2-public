package ru.konditer_class.catalog.screens.previous_orders

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.presenter.InjectPresenter
import kotlinx.android.synthetic.main.activity_current_order.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.OrderTovHeader
import ru.konditer_class.catalog.screens.current_order.CurrentOrderActivity
import ru.konditer_class.catalog.screens.current_order.CurrentOrderActivity.Companion.ORDER_COMMENT
import ru.konditer_class.catalog.screens.current_order.CurrentOrderActivity.Companion.ORDER_ID
import ru.konditer_class.catalog.screens.current_order.SeparatorDecoration

class PreviousOrdersActivity : MvpAppCompatActivity(), PreviousOrdersView {

    @InjectPresenter
    lateinit var presenter: PreviousOrdersPresenter

    private val orderAdapter = PreviousOrdersAdapter().apply {
        onOpenDetailsListener = { orderId, comment ->
            startActivity(
                Intent(this@PreviousOrdersActivity, CurrentOrderActivity::class.java)
                    .putExtra(ORDER_ID, orderId)
                    .putExtra(ORDER_COMMENT, comment)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_previous_orders)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        presenter.loadPreviousOrders()
        recycler.apply {
            adapter = orderAdapter
            addItemDecoration(SeparatorDecoration(
                0f,
                20f,
                Color.TRANSPARENT
            ))
        }
    }

    override fun onPreviousOrdersLoaded(list: List<OrderTovHeader>) {
        tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        orderAdapter.submitList(list)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}