package ru.konditer_class.catalog

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_tov.*
import kotlinx.android.synthetic.main.item_tov_divider.*
import ru.konditer_class.catalog.data.WholeTov
import timber.log.Timber.d
import java.io.File
import java.text.DecimalFormat
import java.util.*


typealias VisibilityListener = (WholeTov, Int) -> Unit
typealias AddTovToOrderListener = (WholeTov) -> Unit

class KonditerAdapter(var showPrices: Boolean, var isZakup: Boolean) :
    ListAdapter<WholeTov, RecyclerView.ViewHolder>(DiffCallback()) {

    val ORDINARY_ITEM = 0
    val DIVIDER_ITEM = 1

    val df = DecimalFormat("0.00")

    var listener: ((WholeTov) -> Unit)? = null
    var refreshListener: ((String, Int) -> Unit)? = null
    var visibilityListener: VisibilityListener? = null
    var addTovToOrderListener: AddTovToOrderListener? = null
    var selectedText: String = ""

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isDividerStub()) DIVIDER_ITEM else ORDINARY_ITEM
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            DIVIDER_ITEM -> KonditerDividerViewHolder(
                LayoutInflater.from(viewGroup.context).inflate(R.layout.item_tov_divider, viewGroup, false)
            )
            else -> KonditerViewHolder(
                LayoutInflater.from(viewGroup.context).inflate(R.layout.item_tov, viewGroup, false),
                isZakup
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is KonditerViewHolder)
            holder.bind(
                getItem(position),
                df,
                showPrices,
                listener,
                refreshListener,
                visibilityListener,
                addTovToOrderListener,
                selectedText
            )
        else if (holder is KonditerDividerViewHolder)
            holder.bind(getItem(position).grName)
    }

    fun getItemForPos(position: Int) = getItem(position)
}

class KonditerViewHolder(
    override val containerView: View,
    val isZakup: Boolean
) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {
    fun bind(
        item: WholeTov,
        df: DecimalFormat,
        showPrices: Boolean,
        listener: ((WholeTov) -> Unit)?,
        refreshListener: ((String, Int) -> Unit)?,
        visibilityListener: VisibilityListener?,
        addTovToOrderListener: AddTovToOrderListener?,
        selectedText: String
    ) {

        tvCode.setSelectedSpan("Код: ${item.code}", selectedText)
        tvOst.text = if (isZakup) "Ост.: ${item.ost}" else null
        tvPrice.text = "${df.format(item.price)} \u20BD"
        tvPrice.visibility = if (showPrices) View.VISIBLE else View.INVISIBLE

//        d("bind item name selectedText $selectedText firstIndex $firstIndex lastIndex $lastIndex name $name")
        tvTitle.setSelectedSpan(item.name, selectedText)
        Glide.with(itemView.context)
            .load(File("${itemView.context.getExternalFilesDir(null)}${File.separator}${item.code}"))
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .signature(
                        ObjectKey("${System.currentTimeMillis()}")
                    )
            )
            .into(ivImage)
        ivImage.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                listener?.invoke(item)
            }
        })
        ivRefresh.setOnClickListener {
            refreshListener?.invoke(item.code, adapterPosition)
        }
        if (isZakup) {
            tvHide.visibility = View.GONE
            if (item.ost > 0)
                ivAddToOrder.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        addTovToOrderListener?.invoke(item)
                    }
                }
            else
                ivAddToOrder.visibility = View.GONE
        } else
            tvHide.setOnClickListener {
                visibilityListener?.invoke(item, adapterPosition)
            }
    }

    fun TextView.setSelectedSpan(sourceText: String, selectedText: String){
        val text = sourceText.trim()
        val firstIndex = text.toLowerCase(Locale.getDefault()).indexOf(selectedText.toLowerCase(Locale.getDefault()))
        val lastIndex = firstIndex + selectedText.length

        val string = SpannableString(text)
        val color = BackgroundColorSpan(containerView.context.resources.getColor(R.color.list_text_span))
        if (firstIndex > -1) {
            string.setSpan(color, firstIndex, lastIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        setText(string)
    }
}




class KonditerDividerViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(grName: String) {
        tvGrName.text = grName
    }
}

class DiffCallback : DiffUtil.ItemCallback<WholeTov>() {
    override fun areItemsTheSame(oldItem: WholeTov, newItem: WholeTov): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(oldItem: WholeTov, newItem: WholeTov): Boolean {
        return oldItem == newItem
    }
}