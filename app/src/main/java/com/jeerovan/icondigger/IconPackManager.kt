package com.jeerovan.icondigger
import android.content.Context
import android.content.res.Resources
import org.xmlpull.v1.XmlPullParser

object IconPackManager {
    private var iconPackPackage: String? = null
    private var iconPackRes: Resources? = null
    val appFilterList = mutableListOf<String>()
    val drawableList = mutableListOf<String>()

    // Call this when the user selects an icon pack
    fun loadIconPack(context: Context, packageName: String) {
        iconPackPackage = packageName
        appFilterList.clear()
        drawableList.clear()
        try {
            val pm = context.packageManager
            iconPackRes = pm.getResourcesForApplication(packageName)
            val appFilterId = iconPackRes?.getIdentifier("appfilter", "xml", packageName) ?: 0

            if (appFilterId != 0) {
                val xpp = iconPackRes?.getXml(appFilterId)
                if (xpp != null) {
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && xpp.name == "item") {
                            val component = xpp.getAttributeValue(null, "component")
                            if (component != null) {
                                appFilterList.add(component)
                            }
                            val drawable = xpp.getAttributeValue(null, "drawable")
                            if (!drawableList.contains(drawable)){
                                drawableList.add(drawable)
                            }
                        }
                        eventType = xpp.next()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
