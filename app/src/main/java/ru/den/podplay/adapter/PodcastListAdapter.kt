package ru.den.podplay.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.search_item.view.*
import ru.den.podplay.R
import ru.den.podplay.ext.inflate
import ru.den.podplay.viewmodel.SearchViewModel
import java.lang.ClassCastException

class PodcastListAdapter(
    private val podcastListAdapterListener: PodcastListApapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var podcastSummaryViewList: MutableList<SearchViewModel.PodcastSummaryViewData?>
            = mutableListOf()

    companion object {
        const val ITEM_PODCAST = 0
        const val ITEM_LOADING = 1
    }

    interface PodcastListApapterListener {
        fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ITEM_PODCAST -> {
                PodcastViewHolder(
                    parent.context.inflate(R.layout.search_item, parent, false),
                    podcastListAdapterListener
                )
            }
            ITEM_LOADING -> {
                LoaderViewHolder(
                    parent.context.inflate(R.layout.loading_item, parent, false)
                )
            }
            else -> throw ClassCastException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (podcastSummaryViewList?.get(position) == null) ITEM_LOADING else ITEM_PODCAST
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val searchViewList = podcastSummaryViewList ?: return

        when (holder) {
            is PodcastViewHolder -> holder.bind(searchViewList[position]!!)
        }
    }

    fun setSearchData(podcastSummaryViewData: List<SearchViewModel.PodcastSummaryViewData>) {
        podcastSummaryViewList.clear()
        podcastSummaryViewList.addAll(podcastSummaryViewData)
        notifyDataSetChanged()
    }

    fun showLoader() {
        podcastSummaryViewList.add(null)
    }

    fun hideLoader() {
        val listSize = podcastSummaryViewList.size
        if (listSize > 0) {
            podcastSummaryViewList.removeAt(listSize - 1)
        }
    }

    fun addItems(items: List<SearchViewModel.PodcastSummaryViewData>) {
        podcastSummaryViewList.addAll(items)
        notifyDataSetChanged()
    }

    inner class PodcastViewHolder(v: View,
                                  private val podcastListAdapterListener: PodcastListApapterListener
    ) :
        RecyclerView.ViewHolder(v) {
        var podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData? = null
        val nameTextView: TextView = v.tv_podcast_name
        val lastUpdatedTextView: TextView = v.tv_podcast_last_updated
        val podcastImageView: ImageView = v.iv_podcast_logo

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

    class LoaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {

    }
}
