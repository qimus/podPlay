package ru.den.podplay.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.den.podplay.R
import ru.den.podplay.adapter.PodcastListAdapter

class SearchFragment : Fragment() {
    private lateinit var podcastListAdapter: PodcastListAdapter

    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        podcastListAdapter = PodcastListAdapter(
            mutableListOf(),
            activity as PodcastListAdapter.PodcastListApapterListener
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }


}