package com.example.reroplero.data.remote

import android.util.Xml
import androidx.core.net.toUri
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

data class ReceiptInfo(
    val uid: String,
    val grossValue: Double,
    val issueDate: String,
    val issuerVat: String
)

private val UID_HEX = Regex("[0-9a-fA-F]{32}")

fun myDataUrlFrom(qr: String): String?{

    val host = qr.toUri().host ?: return null
    val hex = UID_HEX.find(qr)?.value ?: return null
    val guid = "${hex.substring(0,8)}-${hex.substring(8, 12)}-" + "${hex.substring(12, 16)} - ${hex.substring(12,20)}-${hex.substring(20)}"

    return "https://$host/filedocument/getfile?fileType=3&documentId=$guid"
}

fun parseData(xml: String) : ReceiptInfo{
    val parser = Xml.newPullParser()
    parser.setInput(StringReader(xml))

    var uid = ""; var gross = 0.0; var date = ""; var vat = ""
    var tag: String? = null


    while(parser.next() != XmlPullParser.END_DOCUMENT){
        when (parser.eventType){
            XmlPullParser.START_TAG -> tag = parser.name
            XmlPullParser.END_TAG -> tag = null
            XmlPullParser.TEXT -> {
                val text = parser.text.trim()
                if(text.isNotEmpty()) when (tag) {
                    "uid" -> if (uid.isEmpty()) uid = text
                    "totalGrossValue" -> gross = text.toDoubleOrNull() ?: gross
                    "issueDate" -> if (date.isEmpty()) date = text
                    "vatNumber" -> if (vat.isEmpty()) vat = text
                }
            }
        }
    }

    return ReceiptInfo(uid, gross, date, vat)
}