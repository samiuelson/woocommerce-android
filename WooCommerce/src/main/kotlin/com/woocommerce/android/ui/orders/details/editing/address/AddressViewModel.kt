package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

@HiltViewModel
class AddressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val dataStore: WCDataStore,
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    val states: List<Location>
        get() = dataStore.getStates(viewState.countryLocation.code).map { it.toAppModel() }

    val countryLocation: Location
        get() = viewState.countryLocation

    val stateLocation: Location
        get() = viewState.stateLocation

    fun start(storedAddress: Address, addressDraft: Address) {
        loadCountriesAndStates(storedAddress, addressDraft)
        viewState.applyCountryStateChangesSafely(storedAddress, addressDraft)
    }

    private fun loadCountriesAndStates(storedAddress: Address, addressDraft: Address) {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState.copy(
                    isLoading = false
                ).applyCountryStateChangesSafely(storedAddress, addressDraft)
            }
        }
    }

    fun hasCountries() = countries.isNotEmpty()

    fun hasStates() = states.isNotEmpty()

    private fun getCountryNameFromCode(countryCode: String): String {
        return countries.find { it.code == countryCode }?.name ?: countryCode
    }

    private fun getCountryLocationFromCode(countryCode: String): Location {
        return Location(countryCode, getCountryNameFromCode(countryCode))
    }

    private fun getStateNameFromCode(stateCode: String): String {
        return states.find { it.code == stateCode }?.name ?: stateCode
    }

    private fun getStateLocationFromCode(stateCode: String): Location {
        return Location(stateCode, getStateNameFromCode(stateCode))
    }

    fun onCountrySelected(countryCode: String) {
        if (countryCode != viewState.countryLocation.code) {
            viewState = viewState.copy(
                countryLocation = getCountryLocationFromCode(countryCode),
                stateLocation = Location("", ""),
                isStateSelectionEnabled = true
            )
        }
    }

    fun onStateSelected(stateCode: String) {
        if (stateCode != viewState.stateLocation.code) {
            viewState = viewState.copy(
                stateLocation = getStateLocationFromCode(stateCode)
            )
        }
    }

    /**
     * State data acquisition depends on the Country configuration, so when updating the ViewState
     * we need to make sure that we updated the Country code before applying everything else to avoid
     * looking into a outdated state information. If country is currently being edited, we should use it's value.
     * If user doesn't change country while editing the address data, we should try to use a previously selected
     * country (if there is one).
     *
     * @param storedAddress previously selected address.
     * @param addressDraft address being edited. Country and state are not empty if they were just selected.
     */
    private fun ViewState.applyCountryStateChangesSafely(storedAddress: Address, addressDraft: Address) {
        val countryCode = if (addressDraft.country.isEmpty()) storedAddress.country else addressDraft.country
        val stateCode = if (addressDraft.state.isEmpty()) storedAddress.state else addressDraft.state
        val countryLocation = getCountryLocationFromCode(countryCode)

        viewState = viewState.copy(
            countryLocation = countryLocation
        )

        viewState = this.copy(
            countryLocation = countryLocation,
            stateLocation = getStateLocationFromCode(stateCode),
            isStateSelectionEnabled = countryCode.isNotEmpty()
        )
    }

    @Parcelize
    data class ViewState(
        val countryLocation: Location = Location("", ""),
        val stateLocation: Location = Location("", ""),
        val isLoading: Boolean = false,
        val isStateSelectionEnabled: Boolean = false
    ) : Parcelable
}
