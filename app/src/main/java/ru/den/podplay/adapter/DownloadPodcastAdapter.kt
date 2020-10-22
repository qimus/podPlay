package ru.den.podplay.adapter

import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.download_podcast_item.view.*
import kotlinx.android.synthetic.main.search_item.view.iv_podcast_logo
import ru.den.podplay.R
import ru.den.podplay.ext.inflate
import ru.den.podplay.model.Download
import ru.den.podplay.model.DownloadStatus
import ru.den.podplay.util.DateUtils

class DownloadPodcastAdapter(private val listener: DownloadPodcastAdapterListener) :
    RecyclerView.Adapter<DownloadPodcastAdapter.DownloadPodcastViewHolder>(), SwipeToDeleteCallback.Callback {

    private var items: MutableList<Download> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadPodcastViewHolder {
        val view = parent.context.inflate(R.layout.download_podcast_item, parent, false)
        return DownloadPodcastViewHolder(view, listener::onSelect)
    }

    override fun onBindViewHolder(holder: DownloadPodcastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(items: MutableList<Download>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun deleteItem(position: Int) {
        listener.onDeleted(items[position])
        items.removeAt(position)
        notifyDataSetChanged()
    }

    class DownloadPodcastViewHolder(v: View, onSelect: (Download) -> Unit) : RecyclerView.ViewHolder(v) {
        private val podcastLogoImageView = v.iv_podcast_logo
        private val titleTextView = v.tv_title
        private val descriptionTextView = v.tv_description
        private val duration = v.tv_duration
        private val downloadImageView = v.iv_download

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
            item.duration?.let {
                duration.text = DateUtils.formatDuration(it)
            }
            if (item.status != DownloadStatus.DOWNLOADED) {
                itemView.alpha = 0.5f
                itemView.isEnabled = false
                downloadImageView.visibility = View.VISIBLE
                (downloadImageView.drawable as AnimationDrawable).start()
            } else {
                itemView.alpha = 1f
                itemView.isEnabled = true
                downloadImageView.visibility = View.INVISIBLE
            }
            Glide.with(itemView)
                .load(item.imageUrl)
                .into(podcastLogoImageView)
        }
    }

    interface DownloadPodcastAdapterListener {
        fun onSelect(download: Download)
        fun onDeleted(download: Download)
    }
}
