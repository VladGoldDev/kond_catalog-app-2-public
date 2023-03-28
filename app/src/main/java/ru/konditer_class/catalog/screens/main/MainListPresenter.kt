package ru.konditer_class.catalog.screens.main

import android.annotation.SuppressLint
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import ru.konditer_class.catalog.*
import ru.konditer_class.catalog.App.Companion.appCtx
import ru.konditer_class.catalog.api.Api
import ru.konditer_class.catalog.data.*
import ru.konditer_class.catalog.screens.CacheHelper
import timber.log.Timber.d
import timber.log.Timber.e
import java.io.File

@StateStrategyType(AddToEndSingleStrategy::class)
interface MainListView : MvpView {
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun onUpdateSinglePhoto(pos: Int)
    fun showHideDialog(show: Boolean, message: String)

    fun updatePhotoProgress(downloadedCount: Int, initialSize: Int)
    fun onDataLoaded(
        data: MutableList<WholeTov>,
        fromLogin: Boolean = false,
        errorAfterLoadingPhotos: Boolean = false,
        listOfPhotosLeft: List<String>? = null
    )

    fun submitNewList(it: MutableList<WholeTov>)
    fun submitListWithNewPriceType(it: MutableList<WholeTov>)

    fun onCategoriesLoaded(it: List<Category>?)

    fun updatePricesInMenu(it: List<String>)

    fun toggleLoading(on: Boolean)
    fun prepareProgressDialog(size: Int)
    fun onOstsUpdated()

    fun onOpenOrderDialog(tov: WholeTov, orderTovItem: OrderTovItem?)
    fun onTovItemForCurrentOrderSaved()

    fun onError(it: Throwable)

    fun logoutWithoutDialog(loadData: Boolean)

}

@InjectViewState
class MainListPresenter : MvpPresenter<MainListView>() {
    private val api: Api by inject()
    private val db: KonditerDatabase by inject()

    var showWith0Ost: Boolean = false
    var isZakup: Boolean = true
    var showHiddenOnly: Boolean = false
    var chosenPriceType: String = "цена базовая"

    var actualList = mutableListOf<WholeTov>()
    var actualListWithoutZeroOst = mutableListOf<WholeTov>()

    fun getCurrentList() = if (showWith0Ost) actualList else actualListWithoutZeroOst

    private fun getWholeTovs(): Single<MutableList<WholeTov>> =
        if (isZakup) {
            if (showWith0Ost) {
                db.getDAO().getWholeTovsForPriceType(chosenPriceType)
            } else {
                db.getDAO().getWholeTovsForPriceTypeWithout0Ost(chosenPriceType)
            }
        } else if (!showHiddenOnly) {
            db.getDAO().getWholeTovsForPriceTypeExcludeHidden(chosenPriceType)
        } else {
            db.getDAO().getWholeTovsForPriceTypeHidden(chosenPriceType)
        }

    @SuppressLint("CheckResult")
    fun loadFromDb() {
        getWholeTovs()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val itWithDiv = it.withDividers()
                if (showWith0Ost) {
                    actualList = itWithDiv
                } else {
                    actualListWithoutZeroOst = itWithDiv
                }
                viewState.submitNewList(itWithDiv)
            }, {})
    }

    @SuppressLint("CheckResult")
    fun loadFromDbWithNewPriceType() {
        getWholeTovs()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val itWithDiv = it.withDividers()
                if (showWith0Ost)
                    actualList = itWithDiv
                else
                    actualListWithoutZeroOst = itWithDiv
                viewState.submitListWithNewPriceType(itWithDiv)
            }, {})
    }


    private fun getOstTov(body: RequestBody) =
        api.getOstTov(body).map { list ->
            list.onEach { ostTov ->
                ItemEITransformer.transform(ostTov)
            }
        }

    @SuppressLint("CheckResult")
    fun loadTovsList(userId: String) {
        d("getOstTov loadTovsList 1 current ${CacheHelper.current}")
        val clearCache = CacheHelper.current ?: true
        CacheHelper.saveClearCacheFlag(false)
        viewState.toggleLoading(true)
        if (clearCache) {
            d("getOstTov loadTovsList 2 start userId $userId clearCache $clearCache")
            Single.merge(
                Single.fromCallable {
                    db.getDAO().clearPrices()
                },
                Single.fromCallable {
                    db.getDAO().clearTovs()
                }
            ).toList()
                .flatMap {
                    Single.merge(
                        getOstTov(
                            RequestBody.create(
                                MediaType.parse("text/plain"),
                                "${userId}&${BuildConfig.VERSION_NAME}"
                            )
                        ).flatMap { ostTovs ->
                            Single.fromCallable {
                                db.getDAO().addOstTovs(ostTovs)
                            }
                        },
                        api.getPrice(
                            RequestBody.create(
                                MediaType.parse("text/plain"),
                                "${userId}&${BuildConfig.VERSION_NAME}"
                            )
                        ).flatMap { prices ->
                            Single.fromCallable {
                                db.getDAO().addPrices(prices)
                            }
                        }
                    ).toList()
                        .flatMap {
                            db.getDAO().getPriceTypes()
                        }
                        .flatMap {
                            if (it.isNotEmpty()){
                                chosenPriceType = it[0]
                            }
                            viewState.updatePricesInMenu(it)
                            getWholeTovs()
                        }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    viewState.onDataLoaded(data, true)
                }, {
                    viewState.toggleLoading(false)
                    viewState.onError(it)
                })
        } else {
            d("getOstTov loadTovsList start 1 userId $userId clearCache $clearCache")
            db.getDAO()
                .getPriceTypes()
                .flatMap {
                    if (it.isNotEmpty()) {
                        chosenPriceType = it[0]
                    }
                    viewState.updatePricesInMenu(it)
                    getWholeTovs()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    if (list.isNotEmpty()) {
                        d("getOstTov loadTovsList getWholeTovs list size ${list.size} viewState.onDataLoaded")
                        viewState.onDataLoaded(list)
                    } else {
                        d("getOstTov loadTovsList getWholeTovs list is empty. start load getOstTov")
                        Single.merge(
                            getOstTov(
                                RequestBody.create(
                                    MediaType.parse("text/plain"),
                                    "${userId}&${BuildConfig.VERSION_NAME}"
                                )
                            ).flatMap { ostTovs ->
                                Single.fromCallable {
                                    db.getDAO().addOstTovs(ostTovs)
                                }
                            },
                            api.getPrice(
                                RequestBody.create(
                                    MediaType.parse("text/plain"),
                                    "${userId}&${BuildConfig.VERSION_NAME}"
                                )
                            ).flatMap { prices ->
                                Single.fromCallable {
                                    db.getDAO().addPrices(prices)
                                }
                            }
                        ).toList()
                            .flatMap {
                                db.getDAO().getPriceTypes()
                            }
                            .flatMap {
                                viewState.updatePricesInMenu(it)
                                getWholeTovs()
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ data ->
                                viewState.onDataLoaded(data)
                            }, {})
                    }
                }, {
                    e("loadTovsList failed.")
                })
        }
    }

    @SuppressLint("CheckResult")
    fun updateTovsList(userId: String) {
        d("getOstTov updateTovsList 1 start userId $userId ")
        viewState.toggleLoading(true)
        Single.merge(
            Single.fromCallable {
                db.getDAO().clearPrices()
            },
            Single.fromCallable {
                db.getDAO().clearTovs()
            }
        ).toList()
            .flatMap {
                Single.merge(
                    getOstTov(RequestBody.create(MediaType.parse("text/plain"), "${userId}&${BuildConfig.VERSION_NAME}"))
                        .flatMap { ostTovs ->
                            Single.fromCallable {
                                db.getDAO().addOstTovs(ostTovs)
                            }
                        },
                    api.getPrice(RequestBody.create(MediaType.parse("text/plain"), "${userId}&${BuildConfig.VERSION_NAME}"))
                        .flatMap { prices ->
                            Single.fromCallable {
                                db.getDAO().addPrices(prices)
                            }
                        }
                ).toList()
                    .flatMap {
                        db.getDAO().getPriceTypes()
                    }
                    .flatMap {
                        viewState.updatePricesInMenu(it)
                        getWholeTovs()
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ data ->
                viewState.onOstsUpdated()
                viewState.onDataLoaded(data, false)
            }, {
                viewState.toggleLoading(false)
                viewState.onError(it)
            })
    }

    private fun getCategories(): Flowable<List<Category>> =
        if (isZakup) {
            if (showWith0Ost) db.getDAO().getCategoriesForPriceType(chosenPriceType) else db.getDAO()
                .getCategoriesForPriceTypeWithout0Ost(
                    chosenPriceType
                )
        } else if (!showHiddenOnly) {
            db.getDAO().getCategoriesForPriceTypeExcludeHidden(chosenPriceType)
        } else {
            db.getDAO().getCategoriesForPriceTypeHidden(chosenPriceType)
        }

    @SuppressLint("CheckResult")
    fun loadCategories() {
        getCategories()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onCategoriesLoaded(it)
            }, { })
    }

    @SuppressLint("CheckResult")
    fun moveTovToHidden(item: WholeTov, pos: Int) {
        val item1 = getCurrentList().removeAt(pos) //workaround for strange behavior
        Single.fromCallable {
            db.getDAO().moveTovToHidden(TovHidden(item.code, item.typePice))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, { })
    }

    @SuppressLint("CheckResult")
    fun removeTovFromHidden(item: WholeTov, pos: Int) {
        val item1 = getCurrentList().removeAt(pos) //workaround for strange behavior
        Single.fromCallable {
            db.getDAO().removeTovFromHidden(item.code, item.typePice)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, { })
    }

    @SuppressLint("CheckResult")
    fun loadTovForOrderForDialog(item: WholeTov) {
        db.getDAO()
            .getOrderTovItemForCurrentOrder(item.code, INITIAL_ORDER_ID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onOpenOrderDialog(item, it)
            }, {
            }, {
                viewState.onOpenOrderDialog(item, null)
            })
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
    fun clearOrdersOnLogout() {
        Single.merge(
            Single.fromCallable {
                db.getDAO().clearOrderItems()
            },
            Single.fromCallable {
                db.getDAO().clearOrderHeaders()
            }
        ).toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.logoutWithoutDialog(true)
            }, { })
    }

    @SuppressLint("CheckResult")
    fun updatePhoto(name: String, pos: Int) {
        api.getPhoto(RequestBody.create(MediaType.parse("text/plain"), name))
            .flatMap { response ->
                Single.fromCallable {
                    appCtx.writeResponseBodyToDisk(response, name)
                    Unit
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onUpdateSinglePhoto(pos)
            }, {})
    }


    fun loadPhotosThemselves(it: List<String>, initialSize: Int, data: MutableList<WholeTov>) {
        it.take(if (QUEUE_SIZE <= it.size) QUEUE_SIZE else it.size).forEachIndexed { i, name ->
            loadNextPhoto(it, initialSize, data, i, name)
        }
    }

    private var photoDisposable: CompositeDisposable = CompositeDisposable()

    /**
     * список успешно загруженных фото в рамках текущей загрузки,
     * чтобы не загружать их повторно в случае ошибки
     */
    private val alreadyLoaded = mutableListOf<String>()

    /**
     * количество загруженных фото в рамках текущей загрузки
     */
    var downloadedCount = 0

    /**
     * сдвиг для прогресс-бара при возобновлении загрузки после ошибки
     * на количество ранее загруженных фото
     */
    var downloadedCountShift = 0

    /**
     * количество параллельно загружаемых фотографий
     */
    private val QUEUE_SIZE = 50

    @SuppressLint("CheckResult")
    fun loadPhotos(data: MutableList<WholeTov>) {
        db.getDAO().getPhotoNames()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.prepareProgressDialog(it.size)
                downloadedCount = 0
                loadPhotosThemselves(it, it.size, data)
            }, {
                viewState.onDataLoaded(data = data, errorAfterLoadingPhotos = true)
            })
    }

    @SuppressLint("CheckResult")
    fun loadRestOfPhotos() {
        viewState.toggleLoading(true)
        db.getDAO().getPhotoNames()
            .map {
                it.filter { name ->
                    val file = File("${appCtx.getExternalFilesDir(null)}${File.separator}$name")
                    !file.exists() || file.length() == 0L
                }.toMutableList()
            }
            .flatMap {
                Single.zip(
                    getWholeTovs(),
                    Single.just(it),
                    BiFunction<MutableList<WholeTov>, MutableList<String>, Pair<MutableList<WholeTov>, MutableList<String>>> { wholeTovs, photoNames ->
                        Pair(wholeTovs, photoNames)
                    }
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.toggleLoading(false)
                viewState.prepareProgressDialog(it.second.size)
                downloadedCount = 0
                loadPhotosThemselves(it.second, it.second.size, it.first)
            }, {
                viewState.toggleLoading(false)
            })
    }

    @SuppressLint("CheckResult")
    private fun loadNextPhoto(it: List<String>, initialSize: Int, data: MutableList<WholeTov>, index: Int, name: String) {
        photoDisposable.add(
            api.getPhoto(RequestBody.create(MediaType.parse("text/plain"), name))
                .flatMap { response ->
                    Single.fromCallable {
                        val writtenToDisk = appCtx.writeResponseBodyToDisk(response, name)
                        Unit
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ unit ->
                    downloadedCount++
                    alreadyLoaded.add(name)
                    viewState.updatePhotoProgress(downloadedCount + downloadedCountShift, initialSize + downloadedCountShift)
                    if (downloadedCount >= initialSize - 1) {
                        alreadyLoaded.clear()
                        viewState.updatePhotoProgress(-1, -1)
                        photoDisposable.clear()
                        downloadedCountShift = 0
                        viewState.onDataLoaded(data)
                    } else if (QUEUE_SIZE + downloadedCount <= initialSize) {
                        loadNextPhoto(
                            it,
                            initialSize,
                            data,
                            QUEUE_SIZE + downloadedCount - 1,
                            it[QUEUE_SIZE + downloadedCount - 1]
                        )
                    }
                }, { error ->
                    if (!photoDisposable.isDisposed)
                        photoDisposable.dispose()
                    photoDisposable = CompositeDisposable()
                    downloadedCountShift += downloadedCount
                    viewState.updatePhotoProgress(-1, -1)
                    viewState.onDataLoaded(
                        data = data,
                        errorAfterLoadingPhotos = true,
                        listOfPhotosLeft = it.filterNot { it in alreadyLoaded })
                })
        )
    }
}