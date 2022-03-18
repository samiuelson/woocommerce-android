package com.woocommerce.android.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.WooLog.T
import javax.inject.Inject

class PrintHtmlHelper @Inject constructor() {
    // Hold an instance of the WebView object so it isn't garbage collected before the print job is created
    private var webViewInstance: WebView? = null
    private var printJob: PrintJob? = null
    private val content = """<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Print Receipt</title><style>body {margin: 0;padding: 0;border: 0;}.align-left {text-align: left;}.align-right {text-align: right;}.align-top {vertical-align: top;}.receipt {min-width: 130px;max-width: 300px;margin: 0 auto;text-align: center;font-family: SF Pro Text, sans-serif;font-size: 10px;}.receipt-table {width: 100%;border-collapse: separate;border-spacing: 0 2px;font-size: 10px;}.receipt__header .title {font-size: 14px;line-height: 17px;margin-bottom: 12px;margin-top: 12px;font-weight: 700;}.receipt__header .store {padding: 0 12px;}.receipt__header .store__address {margin-top: 12px;line-height: 2px;}.receipt__header .store__contact {margin-top: 4px;}.receipt__header .order__title {font-weight: 800;}.receipt__transaction {line-height: 2px;}.branding-logo {max-width: 250px;margin: 20px auto;}#powered_by {font-size: 7px;padding-top: 5px;}</style></head><body><div class="receipt"><div class="receipt__header"><h1 class="title">AAAAMy testing woo site</h1><hr /><div class="store"><p class="store__contact"> </p></div><div class="order"><p class="order__title">Order 837</p></div></div><hr /><div class="receipt__products"><table class="receipt-table"></table></div><hr /><div class="receipt__subtotal"><table class="receipt-table"><tr><td class="align-left"><b>SUBTOTAL</b></td><td class="align-right"><b><span class="woocommerce-Price-amount amount"><span class="woocommerce-Price-currencySymbol">&#036;</span>0.00</span></b></td></tr><tr><td class="align-left"><div>Tax</div><div>8.63%</div></td><td class="align-right align-top"><span class="woocommerce-Price-amount amount"><span class="woocommerce-Price-currencySymbol">&#036;</span>0.94</span></td></tr><tr><td colspan="2" class="align-left"></td></tr><tr><td class="align-left"><b>TOTAL</b></td><td class="align-right"><b><span class="woocommerce-Price-amount amount"><span class="woocommerce-Price-currencySymbol">&#036;</span>11.80</span></b></td></tr></table></div><hr /><div class="receipt__amount-paid"><table class="receipt-table"><tr><td class="align-left"><b>AMOUNT PAID</b>:</td><td class="align-right"><b><span class="woocommerce-Price-amount amount"><span class="woocommerce-Price-currencySymbol">&#036;</span>11.80</span></b></td></tr><tr><td colspan="2" class="align-left">Visa - 4242</td></tr></table></div><hr /><div class="receipt__transaction"><p id="application-preferred-name">Application name: </p><p id="dedicated-file-name">AID: </p><p id="account_type">Account Type: Credit</p><p id="powered_by">Powered by WooCommerce</p></div></div></body></html>"""

    fun printReceipt(activity: Activity, receiptUrl: String, documentName: String) {
        webViewInstance?.let {
            WooLog.e(
                T.UTILS,
                "Initiating print job before the previous job has finished. " +
                    "The previous job might fail since its WebView might get garbage collected."
            )
        }
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                enqueuePrintJob(activity, view, documentName)
                webViewInstance = null
            }
        }

        webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null)
        webViewInstance = webView
    }

    fun getAndClearPrintJobResult(): PrintJobResult? {
        return printJob?.let {
            when {
                it.isCancelled -> CANCELLED
                it.isFailed -> FAILED
                else -> PrintJobResult.STARTED
            }.also { printJob = null }
        }
    }

    private fun enqueuePrintJob(activity: Activity, webView: WebView, documentName: String) {
        (activity.getSystemService(Context.PRINT_SERVICE) as PrintManager).print(
            documentName,
            webView.createPrintDocumentAdapter(documentName),
            PrintAttributes.Builder().build()
        ).also { printJob = it }
    }

    enum class PrintJobResult {
        CANCELLED, STARTED, FAILED
    }
}
