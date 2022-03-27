package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.CouponSummaryViewModel.CouponSummaryState
import com.woocommerce.android.ui.coupons.CouponSummaryViewModel.CouponUi
import java.lang.StringBuilder

@Composable
fun CouponSummaryScreen(viewModel: CouponSummaryViewModel) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponSummaryState())

    CouponSummaryScreen(state = couponSummaryState)
}

@Composable
fun CouponSummaryScreen(state: CouponSummaryState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        state.coupon?.let { coupon ->
            CouponSummaryHeading(
                code = coupon.code,
                isActive = true
            )

            CouponSummary(coupon, state.currencyCode)
        }
    }
}

@Composable
fun CouponSummaryHeading(
    code: String?,
    isActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        CouponSummaryExpirationLabel(isActive)
    }
}

@Composable
fun CouponSummaryExpirationLabel(isActive: Boolean) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp))
            .padding(vertical = 4.dp)
    ) {
        val status = if (isActive) {
            stringResource(id = R.string.coupon_list_item_label_active)
        } else {
            stringResource(id = R.string.coupon_list_item_label_expired)
        }

        val color = if (isActive) colorResource(id = R.color.woo_celadon_5) else colorResource(id = R.color.woo_gray_5)

        Text(
            text = status,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary,
            modifier = Modifier
                .background(color = color)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CouponSummary(coupon: CouponUi, currencyCode: String?) {
    Card(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.coupon_summary_heading),
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CouponListItemInfo(
                amount = coupon.amount,
                discountType = coupon.discountType,
                currencyCode = currencyCode,
                includedProductsCount = coupon.includedProductsCount,
                excludedProductsCount = coupon.excludedProductsCount,
                includedCategoryCount = coupon.includedCategoryCount,
                fontSize = 20,
                color = MaterialTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            CouponListSpendingInfo(
                coupon.minimumAmount,
                coupon.maximumAmount,
                currencyCode
            )

            /* Hardcoded for design work purposes */
            Text(
                text = "Expires August 4, 2022",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun CouponListSpendingInfo(
    minimumAmount: String? = null,
    maximumAmount: String? = null,
    currencyCode: String?
) {
    val sb = StringBuilder()

    minimumAmount?.let { amount ->
        currencyCode?.let { code ->
            val value = " $code $amount \n\n"
            sb.append(stringResource(id = R.string.coupon_summary_minimum_spend, value))
        }
    }

    maximumAmount?.let { amount ->
        currencyCode?.let { code ->
            val value = " $code $amount \n"
            sb.append(stringResource(id = R.string.coupon_summary_maximum_spend, value))
        }
    }

    if (sb.isNotEmpty()) {
        Text(
            style = MaterialTheme.typography.body1,
            text = sb.toString(),
            fontSize = 20.sp
        )
    }
}
