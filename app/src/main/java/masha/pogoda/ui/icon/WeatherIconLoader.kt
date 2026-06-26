package masha.pogoda.ui.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.ImageView
import android.util.LruCache
import com.caverock.androidsvg.SVG
import masha.pogoda.data.mapper.weatherCodeToIcon
import masha.pogoda.domain.model.WeatherCode

class WeatherIconLoader(private val context: Context) {
    private val cache = object : LruCache<String, SVG>(32) {}

    fun load(imageView: ImageView, iconCode: String) {
        imageView.setImageBitmap(renderBitmap(iconCode, DEFAULT_ICON_SIZE_PX))
    }

    fun renderBitmap(iconCode: String, sizePx: Int): Bitmap {
        val svg = loadSvg(iconCode) ?: loadSvg(weatherCodeToIcon(WeatherCode.CLOUDY, isDay = true))
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        svg?.setDocumentWidth(sizePx.toFloat())
        svg?.setDocumentHeight(sizePx.toFloat())
        svg?.renderToCanvas(canvas)
        return bitmap
    }

    private fun loadSvg(iconCode: String): SVG? {
        cache.get(iconCode)?.let { return it }
        return runCatching {
            SVG.getFromAsset(context.assets, "weather/$iconCode.svg")
        }.getOrNull()?.also { cache.put(iconCode, it) }
    }

    private companion object {
        const val DEFAULT_ICON_SIZE_PX = 192
    }
}

