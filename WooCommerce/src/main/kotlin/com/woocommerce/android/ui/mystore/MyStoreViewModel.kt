package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersSuccess
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val wooCommerceStore: WooCommerceStore,
    private val getStats: GetStats,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedState) {
    private companion object {
        const val NUM_TOP_PERFORMERS = 5
        const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        const val ACTIVE_STATS_GRANULARITY_KEY = "active_stats_granularity_key"
    }

    private var activeStatsGranularity: StatsGranularity =
        savedState.get<StatsGranularity>(ACTIVE_STATS_GRANULARITY_KEY) ?: StatsGranularity.DAYS

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val refreshStoreStats = BooleanArray(StatsGranularity.values().size)
    private val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size)

    init {
        ConnectionChangeReceiver.getEventBus().register(this)
        refreshAll()
        showJetpackBenefitsIfNeeded()
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            if (refreshStoreStats.any { it } || refreshTopPerformerStats.any { it }) {
                refreshAll()
            }
        }
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        activeStatsGranularity = granularity
        savedState[ACTIVE_STATS_GRANULARITY_KEY] = granularity
        loadStoreStats()
        loadTopPerformersStats()
    }

    fun onSwipeToRefresh() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.DASHBOARD_PULLED_TO_REFRESH)
        refreshAll()
    }

    fun getSelectedSiteName(): String =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        } ?: ""

    private fun refreshAll() {
        resetForceRefresh()
        loadStoreStats()
        loadTopPerformersStats()
    }

    private fun showJetpackBenefitsIfNeeded() {
        val showBanner = if (selectedSite.getIfExists()?.isJetpackCPConnected == true) {
            val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
            )
            daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        } else false

        if (showBanner) {
            AnalyticsTracker.track(
                stat = AnalyticsTracker.Stat.FEATURE_JETPACK_BENEFITS_BANNER,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "shown")
            )
        }
        _uiState.update {
            it.copy(
                jetpackBenefitsBanner = JetpackBenefitsBannerUiModel(
                    showBanner,
                    ::onJetpackBannerDismiss
                )
            )
        }
    }

    private fun onJetpackBannerDismiss() {
        _uiState.update { it.copy(jetpackBenefitsBanner = JetpackBenefitsBannerUiModel(show = false)) }
        appPrefsWrapper.recordJetpackBenefitsDismissal()
        AnalyticsTracker.track(
            stat = AnalyticsTracker.Stat.FEATURE_JETPACK_BENEFITS_BANNER,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
        )
    }

    private fun loadStoreStats() {
        if (!networkStatus.isConnected()) {
            refreshStoreStats[activeStatsGranularity.ordinal] = true
            _uiState.update { it.copy(revenueStats = null) }
            _uiState.update { it.copy(visitorsStats = emptyMap()) }
            return
        }

        _uiState.update { it.copy(isLoadingRevenue = true) }
        val forceRefresh = refreshStoreStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshStoreStats[activeStatsGranularity.ordinal] = false
        }
        launch {
            getStats(forceRefresh, activeStatsGranularity).collect { result ->
                when (result) {
                    is RevenueStatsSuccess -> {
                        _uiState.update { it.copy(revenueStats = result.stats?.toStoreStatsUiModel()) }
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
                            mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                        )
                    }
                    is RevenueStatsError -> _uiState.update { it.copy(revenueError = true) }
                    PluginNotActive -> _uiState.update { it.copy(jetPackPluginNotActive = true) }
                    is VisitorsStatsSuccess -> _uiState.update { it.copy(visitorsStats = result.stats) }
                    is VisitorsStatsError -> _uiState.update { it.copy(visitorsError = true) }
                    IsJetPackCPEnabled -> _uiState.update { it.copy(jetpackCpEnabled = true) }
                    is HasOrders -> _uiState.update { it.copy(hasOrders = result.hasOrder) }
                }
            }
        }
    }

    private fun loadTopPerformersStats() {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = true
            _uiState.update { it.copy(topPerformers = emptyList()) }
            return
        }

        val forceRefresh = refreshTopPerformerStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = false
        }

        _uiState.update { it.copy(isLoadingTopPerformers = true) }
        launch {
            getTopPerformers(forceRefresh, activeStatsGranularity, NUM_TOP_PERFORMERS).collect { result ->
                when (result) {
                    is TopPerformersSuccess -> {
                        _uiState.update { it.copy(topPerformers = result.topPerformers.toTopPerformersUiList()) }
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                            mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                        )
                    }
                    TopPerformersError -> _uiState.update { it.copy(topPerformersError = true) }
                }
            }
        }
    }

    private fun resetForceRefresh() {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        refreshStoreStats.forEachIndexed { index, _ ->
            refreshStoreStats[index] = true
        }
    }

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        AnalyticsTracker.track(AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED)
    }

    data class UiState(
        val isLoadingRevenue: Boolean = false,
        val revenueError: Boolean = false,
        val revenueStats: RevenueStatsUiModel? = null,
        val jetPackPluginNotActive: Boolean = false,

        val visitorsError: Boolean = false,
        val jetpackCpEnabled: Boolean = false,
        val visitorsStats: Map<String, Int> = emptyMap(),

        val isLoadingTopPerformers: Boolean = false,
        val topPerformersError: Boolean = false,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),

        val hasOrders: Boolean = false,
        val jetpackBenefitsBanner: JetpackBenefitsBannerUiModel = JetpackBenefitsBannerUiModel(show = false)
    )

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()
    }

    data class JetpackBenefitsBannerUiModel(
        val show: Boolean = false,
        val onDismiss: () -> Unit = {}
    )

    private fun WCRevenueStatsModel.toStoreStatsUiModel(): RevenueStatsUiModel {
        val totals = parseTotal()
        return RevenueStatsUiModel(
            intervalList = getIntervalList().toStatsIntervalUiModelList(),
            totalOrdersCount = totals?.ordersCount,
            totalSales = totals?.totalSales,
            currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        )
    }

    private fun List<WCRevenueStatsModel.Interval>.toStatsIntervalUiModelList() =
        map {
            StatsIntervalUiModel(
                it.interval,
                it.subtotals?.ordersCount,
                it.subtotals?.totalSales
            )
        }

    private fun List<WCTopPerformerProductModel>.toTopPerformersUiList() = map { it.toTopPerformersUiModel() }

    private fun WCTopPerformerProductModel.toTopPerformersUiModel() =
        TopPerformerProductUiModel(
            productId = product.remoteProductId,
            name = StringEscapeUtils.unescapeHtml4(product.name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            totalSpend = currencyFormatter.formatCurrencyRounded(
                total,
                wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
            ),
            imageUrl = product.getFirstImageUrl()?.toImageUrl(),
            onClick = ::onTopPerformerSelected
        )

    private fun String.toImageUrl() =
        PhotonUtils.getPhotonImageUrl(
            this,
            resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100),
            0
        )
}
