package ru.konditer_class.catalog.screens.change_kol_for_categories

import android.annotation.SuppressLint
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.konditer_class.catalog.data.CategoryWrapper
import ru.konditer_class.catalog.data.KolFotoInRow
import ru.konditer_class.catalog.data.KonditerDatabase
import ru.konditer_class.catalog.inject

interface ChangeKolForCategoryView: MvpView {
    fun onCategoriesLoaded(list: List<CategoryWrapper>)
    fun onSaveChanges()
}

@InjectViewState
class ChangeKolForCategoryPresenter: MvpPresenter<ChangeKolForCategoryView>() {

    private val db: KonditerDatabase by inject()

    @SuppressLint("CheckResult")
    fun loadCategories() {
        db.getDAO()
            .getCategoriesWithKolInRow()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onCategoriesLoaded(it)
            },{})
    }

    @SuppressLint("CheckResult")
    fun saveChanges(list: List<CategoryWrapper>) {
        val newList = list.map { KolFotoInRow(it.grCode, it.kolFotoInRow ?: 3) }
        Single.fromCallable {
            db.getDAO()
                .updateKolFotoInRow(newList)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewState.onSaveChanges()
            },{})
    }
}