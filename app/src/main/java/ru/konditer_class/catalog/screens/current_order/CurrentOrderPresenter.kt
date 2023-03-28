package ru.konditer_class.catalog.screens.current_order

import android.annotation.SuppressLint
import android.support.design.widget.Snackbar
import android.view.View
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import ru.konditer_class.catalog.BuildConfig
import ru.konditer_class.catalog.api.Api
import ru.konditer_class.catalog.data.*
import ru.konditer_class.catalog.inject
import ru.konditer_class.catalog.sumByFloat
import timber.log.Timber.d

interface CurrentOrderView : MvpView {
    fun onCurrentOrderLoaded(list: List<OrderTovItem>)
    fun onOpenOrderDialog(tov: WholeTov, orderTovItem: OrderTovItem?)
    fun onTovItemForCurrentOrderSaved()
    fun onOrderSent()
    fun onOrderSendError()
}

@InjectViewState
class CurrentOrderPresenter : MvpPresenter<CurrentOrderView>() {

    private val api: Api by inject()
    private val db: KonditerDatabase by inject()

    private var orderTovItemList = listOf<OrderTovItem>()

    private var isSending = false

    @SuppressLint("CheckResult")
    fun loadOrderById(orderId: String) {
        db.getDAO()
            .getOrderTovItemsForCurrentOrder(orderId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                orderTovItemList = list
                viewState.onCurrentOrderLoaded(list)
            }, {
            }, {
                //
            })
    }

    @SuppressLint("CheckResult")
    fun loadTovForOrderForDialog(item: OrderTovItem) {
        db.getDAO()
            .getWholeTovToAddToOrder(item.tovar, item.price)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onOpenOrderDialog(it, item)
            }, {
            }, {
            })
    }

    @SuppressLint("CheckResult")
    fun removeOrderItemTovFromCurrentOrder(item: OrderTovItem) {
        Single.fromCallable {
            db.getDAO().removeOrderItemTovFromCurrentOrder(item.tovar, INITIAL_ORDER_ID)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { })
    }

    @SuppressLint("CheckResult")
    fun saveTovItemForCurrentOrder(orderTovItem: OrderTovItem) {
        Single.fromCallable {
            db.getDAO().saveTovItemForCurrentOrder(orderTovItem)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onTovItemForCurrentOrderSaved()
            }, { })
    }

    @SuppressLint("CheckResult")
    fun sendCurrentOrderToServer(view: View, userId: String, comment: String):Boolean {
        if (orderTovItemList.isEmpty()) {
            Snackbar.make(view, "Ваша корзина пуста", Snackbar.LENGTH_LONG).show()
            return false
        }
        if (!isSending) {
            isSending = true
            val data = arrayListOf<HashMap<String, Any>>()
            val sumZ = orderTovItemList.sumByFloat { it.kol * it.price }
            val date = getCurrentDateFormatted()
            data.add(
                hashMapOf(
                    "TypeR" to "Doc",
                    "Nomer" to "0",
                    "Data" to date,
                    "IdUser" to "${userId}&${BuildConfig.VERSION_NAME}",
                    "SumZ" to sumZ,
                    "mycomment" to comment
                )
            )
            data.addAll(orderTovItemList.map { it.toMap() })
            d("orderTovItemList send 1 ${orderTovItemList.map { "\n $it " }} ")
            d("orderTovItemList send 2 ${data} ")
            api.sendOrder(RequestBody.create(MediaType.parse("text/plain"), com.google.gson.Gson().toJson(data)))
                .flatMap { responseBody ->
                    val orderId = responseBody.string()
                    Single.merge(
                        Single.fromCallable {
                            db.getDAO().saveTovItemsToPreviousOrder(orderTovItemList.map {
                                it.copyWithNewId(orderId)
                            })
                        },
                        Single.fromCallable {
                            db.getDAO().saveTovHeaderForPreviousOrder(
                                OrderTovHeader(
                                    id = orderId,
                                    date = date,
                                    idUser = userId,
                                    sumZ = sumZ,
                                    comment = comment
                                )
                            )
                        },
                        Single.fromCallable {
                            db.getDAO().removeTovItemsFromCurrentOrder(orderTovItemList)
                        }
                    ).toList()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewState.onOrderSent()
                    isSending = false
                }, {
                    viewState.onOrderSendError()
                    isSending = false
                })
            return true
        }
        return false
    }

    private fun getCurrentDateFormatted(): String {
        val todayDate = java.util.Calendar.getInstance().getTime()
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy")
        return formatter.format(todayDate)
    }
}