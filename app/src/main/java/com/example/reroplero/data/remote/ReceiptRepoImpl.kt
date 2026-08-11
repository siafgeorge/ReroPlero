package com.example.reroplero.data.remote

import android.util.Xml
import com.example.reroplero.domain.ReceiptInfo
import com.example.reroplero.domain.ReceiptLine
import com.example.reroplero.domain.ReceiptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import javax.inject.Inject
import kotlin.math.roundToLong

class ReceiptRepoImpl @Inject constructor(
    private val api: ReceiptApi
) : ReceiptRepository {

    override suspend fun fetch(qr: String): ReceiptInfo = withContext(Dispatchers.IO) {
        parse(api.fetchMyDataXml(detailedUrl(qr)))
    }

    /**
     * Builds the detailed myDATA link out of the QR.
     *
     * The QR is a viewer link such as
     *   https://<host>/fd/faa9b4feee2b4d052a8908ddcfadaaeb:129
     * whose hex blob is the document GUID with its dashes stripped.
     *
     * The *detailed* document is used rather than `getfile?fileType=3`: the plain
     * myDATA export aggregates lines by VAT rate (19 items collapse to 2), while
     * this one keeps every line with its own netValue and vatAmount.
     */
    private fun detailedUrl(qr: String): String {
        val host = HOST.find(qr)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("No host in QR")
        val hex = DOCUMENT_ID.find(qr)?.value
            ?: throw IllegalArgumentException("No document id in QR")
        val guid = "${hex.substring(0, 8)}-${hex.substring(8, 12)}-" +
                "${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
        return "https://$host/filedocument/GetInvoiceDocDetailed/$guid"
    }

    /**
     * The document uses a default namespace with no prefixes, so matching on bare
     * tag names is enough. Header fields keep only their first occurrence because
     * `vatNumber` appears for both the issuer and the counterpart.
     */
    private fun parse(xml: String): ReceiptInfo {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var uid = ""
        var gross = 0.0
        var date = ""
        var vat = ""
        val lines = mutableListOf<ReceiptLine>()

        var tag: String? = null
        var inDetail = false
        var lineNumber = 0
        var lineNet = 0.0
        var lineVat = 0.0

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name
                    if (tag == DETAIL) {
                        inDetail = true
                        lineNumber = 0
                        lineNet = 0.0
                        lineVat = 0.0
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == DETAIL) {
                        inDetail = false
                        lines += ReceiptLine(lineNumber, round2(lineNet + lineVat))
                    }
                    tag = null
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        if (inDetail) when (tag) {
                            "lineNumber" -> lineNumber = text.toIntOrNull() ?: lineNumber
                            "netValue" -> lineNet = text.toDoubleOrNull() ?: lineNet
                            "vatAmount" -> lineVat = text.toDoubleOrNull() ?: lineVat
                        } else when (tag) {
                            "uid" -> if (uid.isEmpty()) uid = text
                            "issueDate" -> if (date.isEmpty()) date = text
                            "vatNumber" -> if (vat.isEmpty()) vat = text
                            "totalGrossValue" -> gross = text.toDoubleOrNull() ?: gross
                        }
                    }
                }
            }
        }
        return ReceiptInfo(uid, gross, date, vat, lines)
    }

    /** Keeps line totals at cent precision so summing them can't drift off the receipt. */
    private fun round2(value: Double): Double = (value * 100).roundToLong() / 100.0

    companion object {
        private const val DETAIL = "invoiceDetails"
        private val HOST = Regex("https?://([^/]+)")
        private val DOCUMENT_ID = Regex("[0-9a-fA-F]{32}")
    }
}
