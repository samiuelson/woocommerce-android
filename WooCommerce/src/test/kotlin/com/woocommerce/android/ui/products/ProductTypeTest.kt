package com.woocommerce.android.ui.products

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductTypeTest : BaseUnitTest() {
    @Test
    fun `given simple core product, when converting from core product string, then ProductType SIMPLE`() {
        val coreProductType = "simple"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = false)

        assertThat(result).isEqualTo(ProductType.SIMPLE)
    }

    @Test
    fun `given variable core product, when converting from core product string, then ProductType VARIABLE`() {
        val coreProductType = "variable"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = false)

        assertThat(result).isEqualTo(ProductType.VARIABLE)
    }

    @Test
    fun `given grouped core product, when converting from core product string, then ProductType GROUPED`() {
        val coreProductType = "grouped"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = false)

        assertThat(result).isEqualTo(ProductType.GROUPED)
    }

    @Test
    fun `given external core product, when converting from core product string, then ProductType EXTERNAL`() {
        val coreProductType = "external"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = false)

        assertThat(result).isEqualTo(ProductType.EXTERNAL)
    }

    @Test
    fun `given unknown core product, when converting from core product string, then ProductType OTHER`() {
        val coreProductType = "abcdef"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = false)

        assertThat(result).isEqualTo(ProductType.OTHER)
    }

    @Test
    fun `given isVirtual and simple core product, when converting from core product string, then ProductType VIRTUAL`() {
        val coreProductType = "simple"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = true)

        assertThat(result).isEqualTo(ProductType.VIRTUAL)
    }

    @Test
    fun `given isVirtual and variable core product, when converting from core product string, then ProductType OTHER`() {
        val coreProductType = "variable"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = true)

        assertThat(result).isEqualTo(ProductType.OTHER)
    }

    @Test
    fun `given isVirtual and grouped core product, when converting from core product string, then ProductType OTHER`() {
        val coreProductType = "grouped"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = true)

        assertThat(result).isEqualTo(ProductType.OTHER)
    }

    @Test
    fun `given isVirtual and external core product, when converting from core product string, then ProductType OTHER`() {
        val coreProductType = "external"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = true)

        assertThat(result).isEqualTo(ProductType.OTHER)
    }

    @Test
    fun `given isVirtual and unknown core product, when converting from core product string, then ProductType OTHER`() {
        val coreProductType = "abcdef"

        val result = ProductType.fromCoreProductType(coreProductType, isVirtual = true)

        assertThat(result).isEqualTo(ProductType.OTHER)
    }
}
