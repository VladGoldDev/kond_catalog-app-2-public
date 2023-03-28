package ru.konditer_class.catalog.screens.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.app_bar_main_list.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.removeFromHiddenSubject

open class SimpleUserActivity : MainListActivity() {

    override val menuResId = R.menu.menu_simple_user
    override val isZakup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.apply {
            isZakup = false
            showWith0Ost = true
        }
        subscribeOnRemoveFromHidden()
    }

    @SuppressLint("CheckResult")
    open fun subscribeOnRemoveFromHidden() {
        removeFromHiddenSubject.subscribe({
            if (it == presenter.chosenPriceType) {
                //workaround for strange behavior
                firstTimeRemoveItem = true
                presenter.loadFromDb()
            }
        },{})
    }

    override fun setupShowPricesFromMenu(menu: Menu) {}

    override fun setup0OstFromMenu(menu: Menu) {}

    override fun setupShowPrices() {}

    override fun setupShowWith0Ost() {}

    override fun setupKonditerAdapterListeners() {
        super.setupKonditerAdapterListeners()
        setupTovVisibilityListener()
    }

    override fun setupAddTovToOrderListener() {}

    open fun setupTovVisibilityListener() {
        konditerAdapter.visibilityListener = { item, pos ->
            presenter.moveTovToHidden(item, pos)
            Snackbar.make(root,"Товар помещен в раздел \"Скрытые\"",Snackbar.LENGTH_SHORT).show()
            if (firstTimeRemoveItem) {
                //workaround for strange behavior
                konditerAdapter.notifyItemRemoved(pos)
                firstTimeRemoveItem = false
            }
            konditerAdapter.submitList(presenter.getCurrentList().toMutableList())
        }
    }

    override fun logout() {
        logoutWithoutDialog(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_hidden_tovs -> {
                startActivity(
                    Intent(this,HiddenTovsActivity::class.java)
                        .putExtra(HiddenTovsActivity.CHOSEN_PRICE_TYPE,presenter.chosenPriceType)
                        .putStringArrayListExtra(HiddenTovsActivity.PRICE_TYPE_LIST,priceTypeList)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}