package masha.pogoda.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import masha.pogoda.databinding.ItemDailyBinding
import masha.pogoda.domain.model.DailyDateLabel
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.ui.icon.WeatherIconLoader

class DailyAdapter(
    private val iconLoader: WeatherIconLoader
) : ListAdapter<DailyWeather, DailyAdapter.ViewHolder>(Diff) {

    /** «Сегодня» в зоне локации; задаётся перед submitList. */
    var today: LocalDate = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDailyBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DailyWeather) {
            binding.dateText.text = DailyDateLabel.format(item.date, today)
            binding.tempText.text = "${item.tempMin}…${item.tempMax}°"
            binding.precipText.text = "☂ ${item.precipProb}%"
            binding.iconView.contentDescription = item.code.toString()
            iconLoader.load(binding.iconView, item.iconCode)
        }
    }

    private object Diff : DiffUtil.ItemCallback<DailyWeather>() {
        override fun areItemsTheSame(oldItem: DailyWeather, newItem: DailyWeather): Boolean =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: DailyWeather, newItem: DailyWeather): Boolean =
            oldItem == newItem
    }
}

