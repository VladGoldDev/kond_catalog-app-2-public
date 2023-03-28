package ru.konditer_class.catalog.screens.previous_orders

import android.annotation.SuppressLint
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.konditer_class.catalog.data.KonditerDatabase
import ru.konditer_class.catalog.data.OrderTovHeader
import ru.konditer_class.catalog.inject

interface PreviousOrdersView: MvpView {
    fun onPreviousOrdersLoaded(list: List<OrderTovHeader>)
}

@InjectViewState
class PreviousOrdersPresenter: MvpPresenter<PreviousOrdersView>() {

    private val db: KonditerDatabase by inject()

    @SuppressLint("CheckResult")
    fun loadPreviousOrders() {
        db.getDAO()
            .getOrderHeadersForPreviousOrders()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                viewState.onPreviousOrdersLoaded(list)
            }, {})
    }
}