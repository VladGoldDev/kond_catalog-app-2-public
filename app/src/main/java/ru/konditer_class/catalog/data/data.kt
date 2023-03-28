package ru.konditer_class.catalog.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import io.reactivex.subjects.PublishSubject
import timber.log.Timber.d
import java.util.*


@Entity(tableName = "OstTov")
data class OstTov(
    @field:Json(name = "Code") @PrimaryKey val code: String,
    @field:Json(name = "Name") val name: String,
    @field:Json(name = "Ost") val ost: Float,
    @field:Json(name = "GrCode") val grCode: String,
    @field:Json(name = "GrName") val grName: String,
    @field:Json(name = "KolFotoByGr") val kolFotoByGr: Int,
    @Ignore @field:Json(name = "itemsEI") var itemsEI: List<ItemsEI>?,
    var blokK: Int?,
    var upakK: Int?,
    var shtK: Int?
) {
    constructor(
        code: String,
        name: String,
        ost: Float,
        grCode: String,
        grName: String,
        kolFotoByGr: Int,
        blokK: Int?,
        upakK: Int?,
        shtK: Int?
    ) : this(
        code = code, name = name, ost = ost, grCode = grCode, grName = grName, kolFotoByGr = kolFotoByGr,
        itemsEI = null,
        blokK = blokK,
        upakK = upakK,
        shtK = shtK
    )
}

@Entity(tableName = "Price", primaryKeys = ["code", "typePice"])
data class Price(
    @field:Json(name = "TypePice") val typePice: String,
    @field:Json(name = "Code") val code: String,
    @field:Json(name = "Price") val price: Float
)

@Entity(tableName = "User")
data class User(
    @field:Json(name = "id") @PrimaryKey val id: String
)

@Entity(tableName = "KolFotoInRow")
data class KolFotoInRow(
    @PrimaryKey val grCode: String,
    @field:Json(name = "kolFotoInRow") val kolFotoInRow: Int
)

@Entity(tableName = "TovHidden", primaryKeys = ["code", "typePice"])
data class TovHidden(
    val code: String,
    val typePice: String
)

data class Category(
    @field:Json(name = "GrCode") @PrimaryKey val grCode: String,
    @field:Json(name = "GrName") val grName: String
)

data class CategoryWrapper(
    @field:Json(name = "GrCode") @PrimaryKey val grCode: String,
    @field:Json(name = "GrName") val grName: String,
    var kolFotoInRow: Int?
)

@Entity(tableName = "WholeTov")
data class WholeTov(
    @field:Json(name = "Code") @PrimaryKey val code: String,
    @field:Json(name = "Name") val name: String,
    @field:Json(name = "Ost") val ost: Int,
    @field:Json(name = "GrCode") val grCode: String,
    @field:Json(name = "GrName") val grName: String,
    @field:Json(name = "KolFotoByGr") val kolFotoByGr: Int,
    @field:Json(name = "TypePice") val typePice: String,
    @field:Json(name = "Price") val price: Float,
    @field:Json(name = "kolFotoInRow") var kolFotoInRow: Int?,
    var blokK: Int?,
    var upakK: Int?,
    var shtK: Int?
) {
    companion object {
        fun createDividerStub(grCode: String, grName: String) = WholeTov(
            code = "-1",
            name = "",
            ost = -1,
            grCode = grCode,
            grName = grName,
            kolFotoByGr = -1,
            typePice = "",
            price = -1f,
            kolFotoInRow = -1,
            blokK = null,
            upakK = null,
            shtK = null
        )
    }

    fun hasCode(text: String) = code.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))
    fun hasName(text: String) = name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))
    fun isDividerStub() = code == "-1"
}

data class ItemsEI(val EI: String, val K: Int)

@Entity(tableName = "OrderTovItem", primaryKeys = ["id", "tovar"])
data class OrderTovItem(
    var id: String,
    var name: String,
    @field:Json(name = "Tovar") val tovar: String,
    @field:Json(name = "Kol") var kol: Int,
    var kolSht: Int?,
    var kolUpak: Int?,
    var kolBlok: Int?,
    @field:Json(name = "Price") val price: Float
) {
    fun toMap() = hashMapOf(
        "TypeR" to "Item",
        "Tovar" to tovar,
        "Kol" to kol,
        "Price" to price,
        "Sum" to price * kol
    )

    fun copyWithNewId(id: String): OrderTovItem {
        return this.copy(id = id)
    }
}

@Entity(tableName = "OrderTovHeader")
data class OrderTovHeader(
    @PrimaryKey val id: String,
    @field:Json(name = "Data") val date: String,
    @field:Json(name = "IdUser") val idUser: String,
    @field:Json(name = "SumZ") val sumZ: Float,
    @field:Json(name = "comment") val comment: String
)

object ItemEITransformer {
    fun transform(ostTov: OstTov) {
        ostTov.itemsEI?.forEach { items ->
            when (items.EI) {
                "блок" -> {
                    d("items EI ${items.EI}")
                    d("ostTov blokK ${ostTov.blokK}")
                    d("items K ${items.K}")
                    ostTov.blokK = items.K
                }
                "упак" -> {
                    ostTov.upakK = items.K
                }
                "шт" -> {
                    ostTov.shtK = items.K
                }
                else -> {}
            }
        }
    }
}

val removeFromHiddenSubject = PublishSubject.create<String>()

const val INITIAL_ORDER_ID = "ru.konditer_class.catalog2.initialid0"