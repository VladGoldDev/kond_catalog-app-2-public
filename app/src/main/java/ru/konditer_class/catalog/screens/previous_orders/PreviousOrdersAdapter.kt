package ru.konditer_class.catalog.screens.previous_orders

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_previous_orders_header.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.OrderTovHeader
import java.text.DecimalFormat

typealias OnOpenDetailsListener = (String, String) -> Unit

class PreviousOrdersAdapter : ListAdapter<OrderTovHeader,PreviousOrdersViewHolder>(DiffCategoryCallback()) {

    private val df = DecimalFormat("0.00")

    var onOpenDetailsListener: OnOpenDetailsListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): PreviousOrdersViewHolder {
        return PreviousOrdersViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_previous_orders_header,viewGroup,false))
    }

    override fun onBindViewHolder(holder: PreviousOrdersViewHolder, position: Int) {
        holder.bind(getItem(position), df, onOpenDetailsListener)
    }
}

class PreviousOrdersViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: OrderTovHeader, df: DecimalFormat, onOpenDetailsListener: OnOpenDetailsListener?) {
        tvId.text = "ID: ${item.id}"
        tvDate.text = "Дата: ${item.date}"
        tvSum.text = "Стоимость заказа: ${df.format(item.sumZ)} \u20BD"
        btViewDetails.setOnClickListener {
            onOpenDetailsListener?.invoke(item.id, item.comment)
        }
    }
}

class DiffCategoryCallback : DiffUtil.ItemCallback<OrderTovHeader>() {
    override fun areItemsTheSame(oldItem: OrderTovHeader, newItem: OrderTovHeader): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(oldItem: OrderTovHeader, newItem: OrderTovHeader): Boolean {
        return oldItem == newItem
    }
}