package ru.konditer_class.catalog.screens.change_kol_for_categories

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_kol_for_category.view.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.CategoryWrapper

class ChangeKolForCategoryAdapter : ListAdapter<CategoryWrapper, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    var data: List<CategoryWrapper> = listOf()

    private val ORDINARY_ITEM = 0
    private val HEADER_ITEM = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ORDINARY_ITEM -> ChangeKolForCategoryViewHolder(layoutInflater.inflate(R.layout.item_kol_for_category, parent, false))
            else -> ChangeKolForCategoryHeaderViewHolder(layoutInflater.inflate(R.layout.item_kol_for_category, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChangeKolForCategoryHeaderViewHolder -> {
                holder.bind()
                holder.itemView.numberPicker.setValueChangedListener { value, action ->
                    data.onEach {
                        it.kolFotoInRow = value
                    }
                    notifyItemRangeChanged(1,super.getItemCount())
                }
            }
            is ChangeKolForCategoryViewHolder -> {
                holder.bind(data[position-1])
            }
        }
    }

    override fun submitList(list: List<CategoryWrapper>?) {
        list?.let {
            data = it
        }
        super.submitList(list)
    }

    override fun getItemCount(): Int {
        return super.getItemCount()+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position==0) HEADER_ITEM else ORDINARY_ITEM
    }
}

class ChangeKolForCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: CategoryWrapper) {
        with(itemView) {
            tvName.text = item.grName
            numberPicker.apply {
                min = 1
                max = 3
                value = item.kolFotoInRow ?: 3
                setValueChangedListener { value, action ->
                    item.kolFotoInRow = value
                }
            }
        }
    }
}

class ChangeKolForCategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind() {
        with(itemView) {
            tvName.text = "Все"
            numberPicker.apply {
                min = 1
                max = 3
                value = 2
            }
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryWrapper>() {
    override fun areItemsTheSame(oldItem: CategoryWrapper, newItem: CategoryWrapper): Boolean {
        return oldItem.grCode == newItem.grCode
    }

    override fun areContentsTheSame(oldItem: CategoryWrapper, newItem: CategoryWrapper): Boolean {
        return oldItem == newItem
    }
}