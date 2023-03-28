package ru.konditer_class.catalog.screens.main

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import kotlinx.android.synthetic.main.app_bar_main_list.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.addItems
import ru.konditer_class.catalog.data.removeFromHiddenSubject

class HiddenTovsActivity : SimpleUserActivity() {

    override val menuResId = R.menu.menu_hidden_tovs

    @State
    lateinit var initialChosenPriceType: String

    @State
    var someTovsWereRestoredForInitialPriceType = false

    companion object {
        val CHOSEN_PRICE_TYPE = "CHOSEN_PRICE_TYPE"
        val PRICE_TYPE_LIST = "PRICE_TYPE_LIST"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this,savedInstanceState)
        content_loading.hide()
        appbar.visibility = View.VISIBLE
        eearchEditText.visibility = View.VISIBLE
//        btSearch.setOnClickListener {
//            showDialogToSearch(presenter.getCurrentList(), recycler)
//        }
        initialChosenPriceType = intent.getStringExtra(CHOSEN_PRICE_TYPE)
        presenter.apply {
            showHiddenOnly = true
            chosenPriceType = intent.getStringExtra(CHOSEN_PRICE_TYPE)
            loadFromDb()
            loadCategories()
        }
    }

    override fun onBackPressed() {
        if (someTovsWereRestoredForInitialPriceType)
            removeFromHiddenSubject.onNext(initialChosenPriceType)
        finish()
    }

    override fun setupTovVisibilityListener() {
        konditerAdapter.visibilityListener = { item, pos ->
            presenter.removeTovFromHidden(item, pos)
            Snackbar.make(root,"Товар возвращен на основной экран", Snackbar.LENGTH_SHORT).show()
            if (item.typePice == initialChosenPriceType)
                someTovsWereRestoredForInitialPriceType = true
            if (firstTimeRemoveItem) {
                //workaround for strange behavior
                konditerAdapter.notifyItemRemoved(pos)
                firstTimeRemoveItem = false
            }
            konditerAdapter.submitList(presenter.getCurrentList().toMutableList())
        }
    }

    override fun finalActionOnInflatingMenu() {
        priceTypeList.clear()
        priceTypeList.addAll(intent.getStringArrayListExtra(PRICE_TYPE_LIST))
        if (priceTypeList.isNotEmpty()) {
            priceTypeMenuItem.apply {
                title = presenter.chosenPriceType
                setOnMenuItemClickListener {
                    BottomSheetBuilder(this@HiddenTovsActivity)
                        .addItems(priceTypeList)
                        .setItemClickListener { pos ->
                            if (it.title != pos.title) {
                                it.title = pos.title
                                presenter.chosenPriceType = pos.title.toString()
                                presenter.actualList = mutableListOf()
                                presenter.actualListWithoutZeroOst = mutableListOf()
                                presenter.loadFromDbWithNewPriceType()
                            }
                        }
                        .createDialog()
                        .show()
                    return@setOnMenuItemClickListener true
                }
            }
        }
    }

    override fun subscribeOnRemoveFromHidden() {}

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null)
            StateSaver.saveInstanceState(this, outState)
    }
}