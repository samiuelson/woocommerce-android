package com.woocommerce.android.widgets

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.woocommerce.android.databinding.WcActionableEmptyLabelBinding
import com.woocommerce.android.extensions.selectAll

/**
 * Simple ViewGroup which contains two views:
 * 1. emptyLabel - button-styled TextView which appears when the text is empty, used as a call to action
 * 2. notEmptyLabel - standard TextView which appears when the text is NOT empty
 */
class WCActionableEmptyLabel @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {
    private val binding = WcActionableEmptyLabelBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.notEmptyLabel.setSelectAllOnFocus(true)
        binding.notEmptyLabel.setOnClickListener {
            (binding.root.parent as View).performClick()
        }
        binding.root.setOnLongClickListener {
            binding.notEmptyLabel.selectAll()
            true
        }
    }

    /**
     * @param text - the text to appear if it's not empty
     * @param emptyTextId - string id of the text to appear if text is empty
     * @param maxLines - optional max number of lines to display
     */
    fun setText(text: String, @StringRes emptyTextId: Int, maxLines: Int = 0) {
        if (text.isEmpty()) {
            binding.emptyLabel.setText(emptyTextId)
            binding.emptyLabel.visibility = View.VISIBLE
            binding.notEmptyLabel.visibility = View.GONE
        } else {
            binding.notEmptyLabel.text = text
            binding.notEmptyLabel.visibility = View.VISIBLE
            binding.emptyLabel.visibility = View.GONE
            if (maxLines > 0) {
                binding.notEmptyLabel.maxLines = maxLines
                binding.notEmptyLabel.ellipsize = TextUtils.TruncateAt.END
            }
        }
    }

    /**
     * When the view is read-only, we make the text selectable and hide the pencil icon
     */
    fun setIsReadOnly(readOnly: Boolean) {
        binding.notEmptyLabel.setTextIsSelectable(readOnly)
        if (readOnly) {
            binding.notEmptyLabel.setCompoundDrawables(null, null, null, null)
        }
    }
}
