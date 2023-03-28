package ru.konditer_class.catalog

import android.support.v4.content.ContextCompat
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_category.view.*
import ru.konditer_class.catalog.data.Category

class KonditerCategoriesAdapter : ListAdapter<Category,KonditerCategoriesViewHolder>(DiffCategoryCallback()) {

    var listener: ((Category, Int) -> Unit)? = null

    var selectedPos = 0
    set(value) {
        notifyItemChanged(field)
        field=value
        notifyItemChanged(field)
    }

    override fun submitList(list: List<Category>?) {
        selectedPos = 0
        super.submitList(list)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): KonditerCategoriesViewHolder {
        return KonditerCategoriesViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_category,viewGroup,false))
    }

    override fun onBindViewHolder(holder: KonditerCategoriesViewHolder, position: Int) {
        holder.bind(getItem(position), position, selectedPos, listener)
    }
}

class KonditerCategoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: Category, position: Int, selectedPos: Int, listener: ((Category, Int) -> Unit)?) {
        with(itemView) {
            itemView.setBackgroundColor(if (position==selectedPos) ContextCompat.getColor(itemView.context, R.color.material_grey_300)
                else ContextCompat.getColor(itemView.context, android.R.color.white))
            text1.text = item.grCode
            text2.text = item.grName
            setOnClickListener {
                listener?.invoke(item, position)
            }
        }
    }
}

class DiffCategoryCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}