package ru.den.podplay.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.search_item.view.*
import ru.den.podplay.R
import ru.den.podplay.viewmodel.SearchViewModel
import timber.log.Timber

class PodcastListAdapter(
    private var podcastSummaryViewList: List<SearchViewModel.PodcastSummaryViewData>?,
    private val podcastListAdapterListener: PodcastListApapterListener
) : RecyclerView.Adapter<PodcastListAdapter.ViewHolder>() {

    interface PodcastListApapterListener {
        fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false), podcastListAdapterListener)
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchViewList = podcastSummaryViewList ?: return
        holder.bind(searchViewList[position])
    }

    fun setSearchData(podcastSummaryViewData: List<SearchViewModel.PodcastSummaryViewData>) {
        Timber.i("${podcastSummaryViewData.size}")
        podcastSummaryViewList = podcastSummaryViewData
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View,
        private val podcastListAdapterListener: PodcastListApapterListener
    ) :
        RecyclerView.ViewHolder(v) {
        var podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData? = null
        val nameTextView: TextView = v.tv_podcast_name
        val lastUpdatedTextView: TextView = v.tv_podcast_last_updated
        val podcastImageView: ImageView = v.iv_podcast

        init {
            v.setOnClickListener {
                podcastSummaryViewData?.let {
                    podcastListAdapterListener.onShowDetails(it)
                }
            }
        }

        fun bind(item: SearchViewModel.PodcastSummaryViewData) {
            podcastSummaryViewData = item
            nameTextView.text = item.name
            lastUpdatedTextView.text = item.lastUpdated
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .into(podcastImageView)
        }
    }
}
