package masha.pogoda.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import masha.pogoda.databinding.ItemHourlyBinding
import masha.pogoda.domain.model.HourlyWeather
import masha.pogoda.ui.icon.WeatherIconLoader

class HourlyAdapter(
    private val iconLoader: WeatherIconLoader
) : ListAdapter<HourlyWeather, HourlyAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHourlyBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HourlyWeather) {
            binding.timeText.text = item.time.toHourLabel()
            binding.tempText.text = "${item.temperature}°"
            binding.precipText.text = if (item.precipProb > 20) "☂ ${item.precipProb}%" else ""
            binding.iconView.contentDescription = item.code.toString()
            iconLoader.load(binding.iconView, item.iconCode)
        }
    }

    private object Diff : DiffUtil.ItemCallback<HourlyWeather>() {
        override fun areItemsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean =
            oldItem.time == newItem.time

        override fun areContentsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean =
            oldItem == newItem
    }
}

private fun String.toHourLabel(): String =
    substringAfter("T", this).take(5)

