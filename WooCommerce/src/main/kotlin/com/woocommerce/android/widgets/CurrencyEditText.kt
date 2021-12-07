package com.woocommerce.android.widgets

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.util.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.math.pow

class CurrencyEditText : AppCompatEditText {
    private var formatCurrency: (BigDecimal) -> String = { "" }
    private var isChangingSelection = false
    private var isChangingText = false
    private var decimals = 2

    @Suppress("SENSELESS_COMPARISON")
    private val isInitialized
        get() = formatCurrency != null

    private val _value = MutableLiveData<BigDecimal>()
    val value: LiveData<BigDecimal> = _value

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        this.inputType = InputType.TYPE_CLASS_NUMBER
    }

    fun initView(currency: String, decimals: Int, currencyFormatter: CurrencyFormatter) {
        this.formatCurrency = currencyFormatter.buildBigDecimalFormatter(currency)
        this.decimals = decimals

        value.value?.let {
            setValue(it)
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        if (isInitialized && !isChangingSelection) {
            isChangingSelection = true
            setSelection(this.length())
            isChangingSelection = false
        }
    }

    fun setValue(value: BigDecimal) {
        isChangingText = true
        setText(formatCurrency(value))
        _value.value = value
        isChangingText = false
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (isInitialized && !isChangingText) {
            isChangingText = true

            val cleanValue = clean(text = text, decimals = decimals)

            _value.value = cleanValue
            setText(formatCurrency(cleanValue))

            isChangingText = false
        }
    }

    companion object TextCleaner {
        /**
         * Cleans the [text] so that it only has numerical characters and has the correct number of fractional digits.
         */
        fun clean(text: CharSequence?, decimals: Int): BigDecimal {
            val regex = Regex("[^\\d]")
            var cleanValue = text.toString().replace(regex, "").toBigDecimalOrNull() ?: BigDecimal.ZERO

            if (decimals > 0) {
                cleanValue = cleanValue.divide(BigDecimal(10f.pow(decimals).toInt()), decimals, HALF_UP)
            }

            return cleanValue
        }
    }
}
