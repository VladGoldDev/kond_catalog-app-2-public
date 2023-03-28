package ru.konditer_class.catalog.screens.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.arellomobile.mvp.presenter.InjectPresenter
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.redmadrobot.gallery.entity.Media
import com.redmadrobot.gallery.entity.TovWrapper
import com.redmadrobot.gallery.ui.GalleryFragment
import com.redmadrobot.gallery.util.closeKeyboard
import com.redmadrobot.gallery.util.isVisible
import kotlinx.android.synthetic.main.activity_main_list.*
import kotlinx.android.synthetic.main.app_bar_main_list.*
import kotlinx.android.synthetic.main.dialog_search_tovs.*
import ru.konditer_class.catalog.*
import ru.konditer_class.catalog.data.Category
import ru.konditer_class.catalog.data.OrderTovItem
import ru.konditer_class.catalog.data.WholeTov
import ru.konditer_class.catalog.screens.BaseActivity
import ru.konditer_class.catalog.screens.CacheHelper
import ru.konditer_class.catalog.screens.change_kol_for_categories.ChangeKolForCategoryActivity
import ru.konditer_class.catalog.screens.current_order.CurrentOrderActivity
import ru.konditer_class.catalog.screens.LoginActivity
import ru.konditer_class.catalog.screens.previous_orders.PreviousOrdersActivity
import timber.log.Timber.d
import timber.log.Timber.e
import java.io.File

open class MainListActivity : BaseActivity(), MainListView {

    companion object {
        const val REQUEST_CODE_CHANGE_KOL = 123
    }

    @InjectPresenter
    lateinit var presenter: MainListPresenter

    override var comment = ""

    val categoriesAdapter = KonditerCategoriesAdapter()
    lateinit var konditerAdapter: KonditerAdapter

    lateinit var rvCat: RecyclerView
    var dialog: AlertDialog? = null

    var showPrices: Boolean = false

    lateinit var priceTypeMenuItem: MenuItem
    val priceTypeList = arrayListOf<String>()

    var pd: ProgressDialog? = null
    var itemsInRow = 3

    protected open val menuResId = R.menu.nav_drawer
    protected open val isZakup = true

    //workaround for strange behavior
    var firstTimeRemoveItem = true

    lateinit var interactionManager: UserInteractionManager

    open fun setupShowPrices() {
        showPrices = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("showPrices", false)
    }

    open fun setupShowWith0Ost() {
        presenter.showWith0Ost = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("showWith0Ost", false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_list)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(toolbar)
//        e("onCreate  ${"Кофе".contains("кофе")} ${"asdasf".contains("e")} ${"12345".contains("4")}")

        setupShowPrices()
        setupShowWith0Ost()
        konditerAdapter = KonditerAdapter(showPrices, isZakup)
        interactionManager = UserInteractionManager(this)
        setupDrawer()

        setupKonditerAdapterListeners()
        recycler.apply {
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            setupLayoutManager()
            adapter = konditerAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, p1: Int) {
                    interactionManager.ping()
                    closeKeyboard()
                }
            })
        }
        categoriesAdapter.listener = { it, pos ->
            categoriesAdapter.selectedPos = pos
            drawer_layout.closeDrawer(GravityCompat.START)
            recycler.post {
                if (eearchEditText.text?.toString()?.isNotEmpty() == true) {
                    eearchEditText.text = null
                    recycler.post {
                        (recycler.layoutManager as GridLayoutManager).scrollToPositionWithOffset(
                            presenter.getCurrentList()
                                .indexOf(presenter.getCurrentList().find { tov -> tov.grCode == it.grCode }), 0
                        )
                    }
                } else {
                    (recycler.layoutManager as GridLayoutManager).scrollToPositionWithOffset(
                        presenter.getCurrentList().indexOf(presenter.getCurrentList().find { tov -> tov.grCode == it.grCode }), 0
                    )
                }
            }
        }
        rvCat = nav_view.getHeaderView(0).findViewById(R.id.rvCategories)
        rvCat.apply {
            adapter = categoriesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, p1: Int) {
                    interactionManager.ping()
                }
            })
        }

        eearchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = p0?.toString() ?: ""
                konditerAdapter.selectedText = text
                onSearch(text)
                searchClear.isVisible = text.isNotEmpty()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {}

        })

        searchClear.setOnClickListener {
            eearchEditText.text = null
        }
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(p0: View) {
                drawer_layout.closeKeyboard()
            }

            override fun onDrawerSlide(p0: View, p1: Float) {}
            override fun onDrawerClosed(p0: View) {}
            override fun onDrawerStateChanged(p0: Int) {}
        })

    }


    fun changeItemsInRow() {
        itemsInRow = when (itemsInRow) {
            3 -> 2
            2 -> 1
            else -> 3
        }
        setupLayoutManager()
    }

    fun setupLayoutManager() {
        recycler.layoutManager = GridLayoutManager(this@MainListActivity, 6).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (konditerAdapter.getItemForPos(position).isDividerStub()) {
                        return 6
                    }
                    return 6 / (konditerAdapter.getItemForPos(position).kolFotoInRow ?: itemsInRow)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interactionManager.destroy()
    }

    open fun setupKonditerAdapterListeners() {
        konditerAdapter.apply {
            listener = { tov ->
                GalleryFragment.create(
                    arrayListOf(
                        TovWrapper(
                            code = tov.code,
                            name = tov.name,
                            image = Media(
                                File("${getExternalFilesDir(null)}${File.separator}${tov.code}"),
                                com.redmadrobot.gallery.entity.MediaType.IMAGE,
                                ""
                            ),
                            price = tov.price,
                            ost = if (isZakup) tov.ost else null,
                            showPrice = showPrices
                        )
                    ), 0, false
                ).show(supportFragmentManager, "fragment_tag_gallery")
            }
            refreshListener = { code, pos ->
                Toast.makeText(this@MainListActivity, "Фото обновляется", Toast.LENGTH_SHORT).show()
                presenter.updatePhoto(code, pos)
            }
        }
        setupAddTovToOrderListener()
    }

    open fun setupAddTovToOrderListener() {
        konditerAdapter.addTovToOrderListener = { item ->
            presenter.loadTovForOrderForDialog(item)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onOpenOrderDialog(tov: WholeTov, orderTovItem: OrderTovItem?) {
        showDialogToAddTovToCurrentOrder(tov, orderTovItem)
    }

    override fun saveTovItemForCurrentOrder(orderTovItem: OrderTovItem) {
        presenter.saveTovItemForCurrentOrder(orderTovItem)
    }

//    protected fun showDialogToSearch(tovs: List<WholeTov>, recycler: RecyclerView) {
//        interactionManager.ping()
//        val dialog = AlertDialog.Builder(this)
//            .setView(R.layout.dialog_search_tovs)
//            .create()
//        dialog.apply {
//            show()
//            val etSearch = findViewById<EditText>(R.id.etSearch)!!
//            val btFind = findViewById<Button>(R.id.btFind)!!
//            val btCancel = findViewById<Button>(R.id.btCancel)!!
//
//            btFind.setOnClickListener {
//                val index = tovs.indexOf(tovs.find { it.code == etSearch.text.toString() })
//                if (index < 0)
//                    Snackbar.make(rootDialog, "В списке такой товар не найден", Snackbar.LENGTH_LONG).show()
//                else {
//                    dismiss()
//                    scrollToFoundTov(index, recycler)
//                }
//            }
//            btCancel.setOnClickListener {
//                dismiss()
//            }
//        }
//    }

    private fun scrollToFoundTov(index: Int, recycler: RecyclerView) {
        recycler.post {
            recycler.scrollToPosition(index)
            recycler.postDelayed({
                recycler.scrollToPosition(index)
            }, 500)
        }
    }

    override fun onTovItemForCurrentOrderSaved() {
        Snackbar.make(root, "Товар сохранен в корзине", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onDataLoaded(
        data: MutableList<WholeTov>,
        fromLogin: Boolean,
        errorAfterLoadingPhotos: Boolean,
        listOfPhotosLeft: List<String>?
    ) {

        e("onDataLoaded data ${data.size} fromLogin $fromLogin errorAfterLoadingPhotos $errorAfterLoadingPhotos listOfPhotosLeft ${listOfPhotosLeft?.size}")
        if (errorAfterLoadingPhotos) {
            AlertDialog.Builder(this)
                .setTitle("Ошибка при загрузке фото")
                .setMessage("Кажется, не все фото загрузились. Повторить загрузку?")
                .setCancelable(false)
                .setPositiveButton("Да") { dialog, which ->
                    dialog.dismiss()
                    if (listOfPhotosLeft == null)
                        presenter.loadPhotos(data)
                    else {
                        presenter.downloadedCount = 0
                        prepareProgressDialog(listOfPhotosLeft.size)
                        presenter.loadPhotosThemselves(listOfPhotosLeft, listOfPhotosLeft.size, data)
                    }
                }
                .setNegativeButton("Нет") { dialog, which ->
                    dialog.dismiss()
                    onDataLoaded(data)
                }
                .show()
        } else if (!fromLogin) {
            val dataWithDiv = data.withDividers()
            content_loading.hide()
            appbar.visibility = View.VISIBLE
            eearchEditText.visibility = View.VISIBLE
            if (presenter.showWith0Ost) {
                presenter.actualList = dataWithDiv
            } else {
                presenter.actualListWithoutZeroOst = dataWithDiv
            }
            firstTimeRemoveItem = true
            konditerAdapter.submitList(dataWithDiv)
            presenter.loadCategories()
        } else {
            presenter.loadPhotos(data)
        }
    }

    var lastListSubmited: MutableList<WholeTov> = mutableListOf()
    fun onSearch(text: String) {
        val currentList: MutableList<WholeTov> = presenter.getCurrentList()

        var selectedItems: MutableList<WholeTov> = currentList

        if (text.isNotEmpty()) {
            selectedItems = currentList.toList().filter { it.hasCode(text) || it.hasName(text) }.toMutableList()
        }
        d("onSearch lastListSubmited     text '$text' currentList ${currentList.size} selectedItems ${selectedItems.size}")
        submitNewList(selectedItems.withDividers())
        konditerAdapter.notifyDataSetChanged()
        recycler.post {
            recycler.scrollToPosition(0);
        }
    }


    override fun prepareProgressDialog(size: Int) {
        if (pd == null)
            pd = ProgressDialog(this).apply {
                max = size
                progress = 0
                setMessage("Загружено 0 из $size фото")
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                show()
            }
        else
            pd!!.apply {
                max = size
                progress = 0
                setMessage("Загружено 0 из $size фото")
                show()
            }
    }

    override fun toggleLoading(on: Boolean) {
        if (on)
            content_loading.show()
        else
            content_loading.hide()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CHANGE_KOL && resultCode == Activity.RESULT_OK)
            presenter.loadTovsList(intent.getStringExtra("code"))
    }

    override fun onCategoriesLoaded(it: List<Category>?) {
        categoriesAdapter.submitList(it)
    }

    override fun submitNewList(it: MutableList<WholeTov>) {
        firstTimeRemoveItem = true
        konditerAdapter.submitList(it)
    }

    override fun submitListWithNewPriceType(it: MutableList<WholeTov>) {
        firstTimeRemoveItem = true
        konditerAdapter.submitList(it)
        presenter.loadCategories()
    }

    override fun updatePricesInMenu(it: List<String>) {
        runOnUiThread {
            priceTypeList.clear()
            priceTypeList.addAll(it)
            if (priceTypeList.isNotEmpty()) {
                priceTypeMenuItem.apply {
                    title = priceTypeList[0]
                    setOnMenuItemClickListener {
                        BottomSheetBuilder(this@MainListActivity)
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
    }

    override fun onError(it: Throwable) {
        e(it, "onError")
        tvError.text = it.localizedMessage
    }

    override fun onOstsUpdated() {
        Toast.makeText(this, "Остатки обновлены", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        interactionManager.ping()
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            AlertDialog.Builder(this)
                .setMessage("Хотите выйти?")
                .setPositiveButton("Да") { dialog, which ->
                    super.onBackPressed()
                }
                .setNegativeButton("Нет") { dialog, which ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        d("getOstTov onCreateOptionsMenu ")
        menuInflater.inflate(menuResId, menu)
        setupShowPricesFromMenu(menu)
        setup0OstFromMenu(menu)
        priceTypeMenuItem = menu.findItem(R.id.menu_price_type)
        finalActionOnInflatingMenu()
        return true
    }

    open fun setup0OstFromMenu(menu: Menu) {
        menu.findItem(R.id.menu_with_0_ost).isChecked = presenter.showWith0Ost
    }

    open fun setupShowPricesFromMenu(menu: Menu) {
        menu.findItem(R.id.menu_show_prices).isChecked = showPrices
    }

    open fun finalActionOnInflatingMenu() {
        d("getOstTov finalActionOnInflatingMenu ")
        presenter.loadTovsList(intent.getStringExtra("code"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        interactionManager.ping()
        eearchEditText.closeKeyboard()
        return when (item.itemId) {
            R.id.menu_show_row_items -> {
                changeItemsInRow()
                true
            }
            R.id.menu_show_prices -> {
                if (item.isChecked) {
                    item.isChecked = false
                    showPrices = false
                    konditerAdapter.apply {
                        showPrices = false
                        notifyDataSetChanged()
                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean("showPrices", false)
                        .apply()
                } else {
                    item.isChecked = true
                    showPrices = true
                    konditerAdapter.apply {
                        showPrices = true
                        notifyDataSetChanged()
                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean("showPrices", true)
                        .apply()
                }
                true
            }
            R.id.menu_with_0_ost -> {
                if (item.isChecked) {
                    item.isChecked = false
                    presenter.showWith0Ost = false
                    presenter.loadCategories()
                    if (presenter.actualListWithoutZeroOst.isEmpty()) {
                        presenter.loadFromDb()
                    } else {
                        submitNewList(presenter.actualListWithoutZeroOst)
                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean("showWith0Ost", false)
                        .apply()
                } else {
                    item.isChecked = true
                    presenter.showWith0Ost = true
                    presenter.loadCategories()
                    if (presenter.actualList.isEmpty()) {
                        presenter.loadFromDb()
                    } else {
                        submitNewList(presenter.actualList)
                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean("showWith0Ost", true)
                        .apply()
                }
                true
            }
            R.id.menu_kol_foto_in_row -> {
                startActivityForResult(Intent(this, ChangeKolForCategoryActivity::class.java), REQUEST_CODE_CHANGE_KOL)
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            R.id.menu_update_data -> {
                d("getOstTov onOptionsItemSelected menu clicked ")
                presenter.updateTovsList(intent.getStringExtra("code"))
                true
            }
            R.id.menu_upload_rest_of_the_photos -> {
                presenter.loadRestOfPhotos()
                true
            }
            R.id.menu_view_current_order -> {
                startActivity(Intent(this, CurrentOrderActivity::class.java))
                true
            }
            R.id.menu_view_past_orders -> {
                startActivity(Intent(this, PreviousOrdersActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    open fun logout() {
        AlertDialog.Builder(this)
            .setMessage("Удалить заказы из БД?")
            .setPositiveButton("Да") { dialog, which ->
                dialog.dismiss()
                presenter.clearOrdersOnLogout()
            }
            .setNegativeButton("Нет") { dialog, which ->
                dialog.dismiss()
                logoutWithoutDialog(true)
            }
            .create()
            .show()
    }

    override fun logoutWithoutDialog(loadData: Boolean) {
        e("getOstTov logoutWithoutDialog 1 loadData $loadData current ${CacheHelper.current} ")
        CacheHelper.saveClearCacheFlag(loadData)
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .remove("code")
            .apply()

        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    override fun showHideDialog(show: Boolean, message: String) {
        if (show) {
            dialog = AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .create()
            dialog!!.show()
        } else {
            dialog?.dismiss()
        }
    }

    override fun onUpdateSinglePhoto(pos: Int) {
        konditerAdapter.notifyItemChanged(pos)
    }

    override fun updatePhotoProgress(downloadedCount: Int, initialSize: Int) {
        if (downloadedCount == -1 && initialSize == -1)
            pd?.dismiss()
        else
            pd?.apply {
                setMessage("Загружено $downloadedCount из $initialSize фото")
                progress = downloadedCount
                max = initialSize
            }
    }


}
