package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditVariationAttributesBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesViewModel.ViewState
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class EditVariationAttributesFragment :
    BaseFragment(R.layout.fragment_edit_variation_attributes) {
    companion object {
        const val TAG: String = "EditVariationAttributesFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditVariationAttributesViewModel by activityViewModels { viewModelFactory }

    private val navArgs: EditVariationAttributesFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditVariationAttributesBinding

    private val adapter
        get() = binding.variationList.adapter as? VariationAttributesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditVariationAttributesBinding.bind(view)
        setupObservers()
        setupViews()
        viewModel.start(navArgs.remoteProductId, navArgs.remoteVariationId)
    }

    override fun getFragmentTitle() = getString(R.string.product_attributes)

    private fun setupObservers() = viewModel.apply {
        viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
        editableVariationAttributeList.observe(viewLifecycleOwner, Observer(::handleVariationAttributeListChanges))
    }

    private fun setupViews() = binding.apply {
        variationList.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            itemAnimator = null
        }
    }

    private fun handleViewStateChanges(old: ViewState?, new: ViewState?) {
        new?.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) {
            // change skeleton visibility
        }

        new?.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
            // update refresh layout
        }

        new?.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) {
            // update empty view visibility
        }
    }

    private fun handleVariationAttributeListChanges(selectableOptions: List<VariationAttributeSelectionGroup>) {
        showAttributeSelectableOptions(selectableOptions.toMutableList())
        requireActivity().invalidateOptionsMenu()
    }

    private fun showAttributeSelectableOptions(
        selectableOptions: MutableList<VariationAttributeSelectionGroup>
    ) {
        adapter
            ?.refreshSourceData(selectableOptions)
            ?: binding.variationList.apply {
                adapter = VariationAttributesAdapter(
                    selectableOptions,
                    ::displaySelectionDialog
                )
            }
    }

    private fun displaySelectionDialog(item: VariationAttributeSelectionGroup) {
        AttributeOptionSelectorDialog.newInstance(
            attributeGroup = item,
            onAttributeOptionSelected = { modifiedGroup ->
                adapter?.refreshSingleAttributeSelectionGroup(modifiedGroup)
            }
        ).also { it.show(parentFragmentManager, AttributeOptionSelectorDialog.TAG) }
    }
}
