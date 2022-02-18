package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType

enum class ProductType(@StringRes val stringResource: Int = 0, val coreProductType: String = "") {
    SIMPLE(R.string.product_type_simple, CoreProductType.SIMPLE.value),
    VIRTUAL(R.string.product_type_virtual, CoreProductType.SIMPLE.value),
    GROUPED(R.string.product_type_grouped, CoreProductType.GROUPED.value),
    EXTERNAL(R.string.product_type_external, CoreProductType.EXTERNAL.value),
    VARIABLE(R.string.product_type_variable, CoreProductType.VARIABLE.value),
    OTHER;

    companion object {
        fun fromCoreProductType(coreProductType: String, isVirtual: Boolean): ProductType {
            if (isVirtual && coreProductType.lowercase() != "simple") {
                WooLog.w(T.PRODUCTS, "Unknown productType state: isVirtual == true but type is $coreProductType")
                // Virtual non-simple products are not known to the app => OTHER is returned
                return OTHER
            }
            return when (coreProductType.lowercase()) {
                "grouped" -> GROUPED
                "external" -> EXTERNAL
                "variable" -> VARIABLE
                else -> when {
                    coreProductType.lowercase() == "simple" && isVirtual -> VIRTUAL
                    coreProductType.lowercase() == "simple" -> SIMPLE
                    else -> OTHER
                }
            }
        }
    }
}
