package ru.den.podplay.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.download_podcast_item.view.*
import kotlinx.android.synthetic.main.search_item.view.iv_podcast_logo
import ru.den.podplay.R
import ru.den.podplay.ext.inflate
import ru.den.podplay.model.Download

class DownloadPodcastAdapter(private val onSelect: (Download) -> Unit) :
    RecyclerView.Adapter<DownloadPodcastAdapter.DownloadPodcastViewHolder>() {

    private var items: MutableList<Download> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadPodcastViewHolder {
        val view = parent.context.inflate(R.layout.download_podcast_item, parent, false)
        return DownloadPodcastViewHolder(view, onSelect)
    }

    override fun onBindViewHolder(holder: DownloadPodcastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(items: MutableList<Download>) {
        this.items = items
        notifyDataSetChanged()
    }

    class DownloadPodcastViewHolder(v: View, onSelect: (Download) -> Unit) : RecyclerView.ViewHolder(v) {
        private val podcastLogoImageView = v.iv_podcast_logo
        private val titleTextView = v.tv_title
        private val descriptionTextView = v.tv_description
        private val duration = v.tv_duration

        private var downloadItem: Download? = null

        init {
            v.setOnClickListener {
                downloadItem?.let {
                    onSelect(it)
                }
            }
        }

        fun bind(item: Download) {
            downloadItem = item
            titleTextView.text = item.podcastTitle
            descriptionTextView.text = item.episodeTitle
            duration.text = item.duration
            Glide.with(itemView)
                .load(item.imageUrl)
                .into(podcastLogoImageView)
        }
    }
}
