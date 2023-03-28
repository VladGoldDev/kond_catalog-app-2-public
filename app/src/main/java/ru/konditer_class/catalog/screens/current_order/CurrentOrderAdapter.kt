package ru.konditer_class.catalog.screens.current_order

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.redmadrobot.gallery.util.isVisible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.app_bar_main_list.*
import kotlinx.android.synthetic.main.item_current_order_tov.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.OrderTovItem
import timber.log.Timber.d
import java.text.DecimalFormat

typealias OnRequestToChangeListener = (OrderTovItem) -> Unit
typealias OnRequestToRemoveListener = (OrderTovItem) -> Unit

class CurrentOrderAdapter : ListAdapter<OrderTovItem, CurrentOrderViewHolder>(DiffCategoryCallback()) {

    var onRequestToChangeListener: OnRequestToChangeListener? = null
    var onRequestToRemoveListener: OnRequestToRemoveListener? = null

    var isEditable = true

    private val df = DecimalFormat("0.00")

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): CurrentOrderViewHolder {
        return CurrentOrderViewHolder(
            LayoutInflater.from(viewGroup.context).inflate(R.layout.item_current_order_tov, viewGroup, false)
        )
    }

    override fun onBindViewHolder(holder: CurrentOrderViewHolder, position: Int) {
        holder.bind(getItem(position), df, isEditable, onRequestToChangeListener, onRequestToRemoveListener)
    }
}

class CurrentOrderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(
        item: OrderTovItem,
        df: DecimalFormat,
        isEditable: Boolean,
        onRequestToChangeListener: OnRequestToChangeListener?,
        onRequestToRemoveListener: OnRequestToRemoveListener?
    ) {


//        d("bind order item ${item.id} ${item.comment}    ${item}")

        tvCode.text = "Код: ${item.tovar}"
//        if (isEditable) {
//            commentNotEditable.isVisible = false
//        } else {
//            commentNotEditable.isVisible = true
//            commentNotEditable.text = "Комментарий: ${item.comment}"
//        }

        tvName.text = item.name
        tvKol.text = "Общее количество (в штуках): ${item.kol}"
        if (item.kolSht != null) {
            tvKolSht.apply {
                visibility = View.VISIBLE
                text = "Штуки: ${item.kolSht}"
            }
        } else {
            tvKolSht.visibility = View.GONE
        }
        if (item.kolBlok != null) {
            tvKolBlok.apply {
                visibility = View.VISIBLE
                text = "Блоки: ${item.kolBlok}"
            }
        } else {
            tvKolBlok.visibility = View.GONE
        }
        if (item.kolUpak != null) {
            tvKolUpak.apply {
                visibility = View.VISIBLE
                text = "Упаковки: ${item.kolUpak}"
            }
        } else {
            tvKolUpak.visibility = View.GONE
        }
        tvPrice.text = "Цена за штуку: ${df.format(item.price)} \u20BD"
        tvSum.text = "Общая стоимость: ${df.format(item.price * item.kol)} \u20BD"

        btChange.visibility = if (isEditable) View.VISIBLE else View.GONE
        btChange.setOnClickListener {
            onRequestToChangeListener?.invoke(item)
        }

        btRemove.visibility = if (isEditable) View.VISIBLE else View.GONE
        btRemove.setOnClickListener {
            onRequestToRemoveListener?.invoke(item)
        }
    }
}

class DiffCategoryCallback : DiffUtil.ItemCallback<OrderTovItem>() {
    override fun areItemsTheSame(oldItem: OrderTovItem, newItem: OrderTovItem): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(oldItem: OrderTovItem, newItem: OrderTovItem): Boolean {
        return oldItem == newItem
    }
}