package ru.konditer_class.catalog.screens.change_kol_for_categories

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.presenter.InjectPresenter
import kotlinx.android.synthetic.main.activity_change_kol_for_category.*
import ru.konditer_class.catalog.R
import ru.konditer_class.catalog.data.CategoryWrapper

class ChangeKolForCategoryActivity : MvpAppCompatActivity(), ChangeKolForCategoryView {

    @InjectPresenter
    lateinit var presenter: ChangeKolForCategoryPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_kol_for_category)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter.loadCategories()
    }

    override fun onCategoriesLoaded(list: List<CategoryWrapper>) {
        rvCategories.adapter = ChangeKolForCategoryAdapter().apply {
            submitList(list)
        }
    }

    override fun onSaveChanges() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.kol_in_row, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_apply -> {
                (rvCategories.adapter as? ChangeKolForCategoryAdapter)?.data?.let {
                    presenter.saveChanges(it)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
