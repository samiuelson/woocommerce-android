package com.woocommerce.android.ui.refunds

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundByItemsViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IssueRefundViewModelTest : BaseUnitTest() {
    private val orderStore: WCOrderStore = mock()
    private val wooStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val gatewayStore: WCGatewayStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag = mock()
    private val resourceProvider: ResourceProvider = mock {
        on(it.getString(R.string.multiple_shipping)).thenAnswer { "Multiple shipping lines" }
        on(it.getString(R.string.and)).thenAnswer { "and" }
        on(it.getString(any(), any())).thenAnswer { i ->
            "You can refund " + i.arguments[1].toString()
        }
    }
    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        }
    )

    private val paymentChargeRepository: PaymentChargeRepository = mock()

    private val savedState = IssueRefundFragmentArgs(0).initSavedStateHandle()

    private lateinit var viewModel: IssueRefundViewModel

    @Before
    fun setup() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)
    }

    private fun initViewModel() {
        whenever(selectedSite.get()).thenReturn(SiteModel())
        whenever(currencyFormatter.buildBigDecimalFormatter(any())).thenReturn { "" }

        viewModel = IssueRefundViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            currencyFormatter,
            orderStore,
            wooStore,
            selectedSite,
            networkStatus,
            resourceProvider,
            orderDetailRepository,
            gatewayStore,
            refundStore,
            paymentChargeRepository,
            orderMapper,
            inPersonPaymentsCanadaFeatureFlag,
        )
    }

    @Test
    fun `when order has no shipping, then refund notice is not visible`() {
        testBlocking {
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(OrderTestUtils.generateOrder())

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has one shipping, then the notice not visible`() {
        testBlocking {
            val orderWithShipping = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has multiple shipping, multiple shipping are mentioned in the notice`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertTrue(viewState!!.isRefundNoticeVisible)
            assertEquals("You can refund multiple shipping lines", viewState!!.refundNotice)
        }
    }

    @Test
    fun `given non cash order, when successfully charge data loaded, then card info is visible`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card (Visa **** 1234)")
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.first()).isInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then snack bar is not triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.any()).isNotInstanceOf(
                MultiLiveEvent.Event.ShowSnackbar::class.java
            )
        }
    }

    @Test
    fun `given non interac refund, when refund confirmed, then snack bar is triggered with refund message`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat((events.first() as MultiLiveEvent.Event.ShowSnackbar).message).isEqualTo(
                R.string.order_refunds_amount_refund_progress_message
            )
        }
    }

    @Test
    fun `given interac refund, when initiating refund, then trigger updating backend snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.first()).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when initiating refund, then don't trigger updating backend snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.any()).isNotEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund
                )
            )
        }
    }

    @Test
    fun `given interac refund, when initiating refund fails, then trigger updating backend failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events[1]).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when initiating refund fails, then don't trigger update failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.any()).isNotEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when refund() fails, then trigger failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.first()).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.order_refunds_amount_refund_error
                )
            )
        }
    }

    @Test
    fun `given IPP canada feature flag is disabled, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.first()).isNotInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given non-interac refund, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.first()).isNotInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given charges call fails, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.first()).isNotInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given non cash order, when charge data loaded with error, then card info is not visible`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card")
        }
    }

    @Test
    fun `given non cash order, when charge data loaded, then button enabled`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.isSubmitButtonEnabled).isTrue()
        }
    }

    @Test
    fun `given non cash order and text summary to long, when charge data loaded, then button not enabled`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )

            initViewModel()

            viewModel.onRefundSummaryTextChanged(10, 100)

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.isSubmitButtonEnabled).isFalse()
        }
    }

    @Test
    fun `given non cash order and non charge id in order, when charge data loading, then card info is not visible`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card")
        }
    }
}
