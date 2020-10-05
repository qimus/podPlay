package ru.den.podplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.episode_item.view.*
import ru.den.podplay.R
import ru.den.podplay.util.DateUtils
import ru.den.podplay.util.HtmlUtils
import ru.den.podplay.viewmodel.PodcastViewModel

class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private val episodeListAdapterListener: EpisodeListAdapterListener
) : RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    interface EpisodeListAdapterListener {
        fun onSelectEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.episode_item, parent, false),
            episodeListAdapterListener
        )
    }

    override fun getItemCount() = episodeViewList?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        holder.bind(episodeViewList[position])
    }

    class ViewHolder(v: View, private val episodeListAdapterListener: EpisodeListAdapterListener) :
        RecyclerView.ViewHolder(v) {

        var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        val titleTextView: TextView = v.titleView
        val descTextView: TextView = v.descView
        val durationTextView: TextView = v.durationView
        val releaseDateTextView: TextView = v.releaseDateView

        init {
            itemView.setOnClickListener {
                episodeViewData?.let {
                    episodeListAdapterListener.onSelectEpisode(it)
                }
            }
        }

        fun bind(episodeView: PodcastViewModel.EpisodeViewData) {
            this.episodeViewData = episodeView
            titleTextView.text = episodeView.title
            descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
            durationTextView.text = episodeView.duration
            releaseDateTextView.text = episodeView.releaseDate?.let {
                DateUtils.dateToShortDate(it)
            }
        }
    }
}
