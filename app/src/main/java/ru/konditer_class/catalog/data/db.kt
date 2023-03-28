package ru.konditer_class.catalog.data

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Database(
    entities = [
        User::class,
        OstTov::class,
        Price::class,
        KolFotoInRow::class,
        TovHidden::class,
        OrderTovItem::class,
        OrderTovHeader::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KonditerDatabase : RoomDatabase() {
    abstract fun getDAO(): DAO
}

@Dao
interface DAO {

    @Query("SELECT * FROM User")
    fun getUserList(): Single<List<User>>

    @Query("SELECT OstTov.*, Price.*, KolFotoInRow.kolFotoInRow FROM OstTov INNER JOIN Price ON OstTov.code = Price.code LEFT JOIN KolFotoInRow ON OstTov.grCode = KolFotoInRow.grCode WHERE Price.typePice == :priceType ORDER BY OstTov.grName, OstTov.name")
    fun getWholeTovsForPriceType(priceType: String): Single<MutableList<WholeTov>>

    @Query("SELECT OstTov.*, Price.*, KolFotoInRow.kolFotoInRow FROM OstTov INNER JOIN Price ON OstTov.code = Price.code LEFT JOIN KolFotoInRow ON OstTov.grCode = KolFotoInRow.grCode WHERE Price.typePice == :priceType AND OstTov.code NOT IN (SELECT TovHidden.code from TovHidden WHERE TovHidden.typePice = :priceType) ORDER BY OstTov.grName, OstTov.name")
    fun getWholeTovsForPriceTypeExcludeHidden(priceType: String): Single<MutableList<WholeTov>>

    @Query("SELECT OstTov.*, Price.*, KolFotoInRow.kolFotoInRow FROM OstTov INNER JOIN Price ON OstTov.code = Price.code LEFT JOIN KolFotoInRow ON OstTov.grCode = KolFotoInRow.grCode WHERE Price.typePice == :priceType AND OstTov.code IN (SELECT TovHidden.code from TovHidden WHERE TovHidden.typePice = :priceType) ORDER BY OstTov.grName, OstTov.name")
    fun getWholeTovsForPriceTypeHidden(priceType: String): Single<MutableList<WholeTov>>

    @Query("SELECT OstTov.*, Price.*, KolFotoInRow.kolFotoInRow FROM OstTov INNER JOIN Price ON OstTov.code = Price.code LEFT JOIN KolFotoInRow ON OstTov.grCode = KolFotoInRow.grCode WHERE Price.typePice == :priceType AND OstTov.ost>0 ORDER BY OstTov.grName, OstTov.name")
    fun getWholeTovsForPriceTypeWithout0Ost(priceType: String): Single<MutableList<WholeTov>>

    @Query("SELECT DISTINCT GrCode, GrName FROM OstTov INNER JOIN Price ON OstTov.code = Price.code WHERE Price.typePice == :priceType ORDER BY OstTov.grName")
    fun getCategoriesForPriceType(priceType: String): Flowable<List<Category>>

    @Query("SELECT DISTINCT GrCode, GrName FROM OstTov INNER JOIN Price ON OstTov.code = Price.code WHERE Price.typePice == :priceType AND OstTov.ost>0 ORDER BY OstTov.grName")
    fun getCategoriesForPriceTypeWithout0Ost(priceType: String): Flowable<List<Category>>

    @Query("SELECT DISTINCT GrCode, GrName FROM OstTov INNER JOIN Price ON OstTov.code = Price.code WHERE Price.typePice == :priceType AND OstTov.code NOT IN (SELECT TovHidden.code from TovHidden WHERE TovHidden.typePice = :priceType) ORDER BY OstTov.grName")
    fun getCategoriesForPriceTypeExcludeHidden(priceType: String): Flowable<List<Category>>

    @Query("SELECT DISTINCT GrCode, GrName FROM OstTov INNER JOIN Price ON OstTov.code = Price.code WHERE Price.typePice == :priceType AND OstTov.code IN (SELECT TovHidden.code from TovHidden WHERE TovHidden.typePice = :priceType) ORDER BY OstTov.grName")
    fun getCategoriesForPriceTypeHidden(priceType: String): Flowable<List<Category>>

    @Query("SELECT DISTINCT TypePice FROM Price")
    fun getPriceTypes(): Single<List<String>>

    @Query("SELECT Code FROM OstTov")
    fun getPhotoNames(): Single<MutableList<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addOstTovs(ostTovs: List<OstTov>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPrices(prices: List<Price>)

    @Query("DELETE FROM OstTov")
    fun clearTovs()

    @Query("DELETE FROM Price")
    fun clearPrices()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateKolFotoInRow(kolFotoInRow: List<KolFotoInRow>)

    @Query("SELECT DISTINCT OstTov.GrCode, OstTov.GrName, KolFotoInRow.kolFotoInRow FROM OstTov LEFT JOIN KolFotoInRow ON OstTov.grCode = KolFotoInRow.grCode")
    fun getCategoriesWithKolInRow(): Flowable<List<CategoryWrapper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun moveTovToHidden(hidden: TovHidden)

    @Query("DELETE FROM TovHidden WHERE TovHidden.Code = :code AND TovHidden.typePice = :typePice")
    fun removeTovFromHidden(code: String, typePice: String)

    @Query("SELECT * FROM OrderTovItem WHERE OrderTovItem.tovar = :code AND OrderTovItem.id = :orderId")
    fun getOrderTovItemForCurrentOrder(code: String, orderId: String): Maybe<OrderTovItem>

    @Query("SELECT * FROM OrderTovItem WHERE OrderTovItem.id = :orderId")
    fun getOrderTovItemsForCurrentOrder(orderId: String): Flowable<List<OrderTovItem>>

    @Query("SELECT OstTov.*, Price.* FROM OstTov INNER JOIN Price ON OstTov.code = Price.code WHERE Price.price == :price AND OstTov.code = :code")
    fun getWholeTovToAddToOrder(code: String, price: Float): Maybe<WholeTov>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTovItemForCurrentOrder(orderTovItem: OrderTovItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTovHeaderForPreviousOrder(orderTovHeader: OrderTovHeader)

    @Query("DELETE FROM OrderTovItem WHERE OrderTovItem.tovar = :code AND OrderTovItem.id = :orderId")
    fun removeOrderItemTovFromCurrentOrder(code: String, orderId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTovItemsToPreviousOrder(orderTovItemList: List<OrderTovItem>)

    @Delete
    fun removeTovItemsFromCurrentOrder(orderTovItemList: List<OrderTovItem>)

    @Query("SELECT * FROM OrderTovHeader ORDER BY OrderTovHeader.id DESC")
    fun getOrderHeadersForPreviousOrders(): Single<List<OrderTovHeader>>

    @Query("DELETE FROM OrderTovItem")
    fun clearOrderItems()

    @Query("DELETE FROM OrderTovHeader")
    fun clearOrderHeaders()

}