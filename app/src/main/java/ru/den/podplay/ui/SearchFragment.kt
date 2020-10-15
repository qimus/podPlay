package ru.den.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_search.*
import ru.den.podplay.R
import ru.den.podplay.adapter.PodcastListAdapter
import ru.den.podplay.provider.PodcastSearchSuggestProvider
import ru.den.podplay.viewmodel.PodcastViewModel
import ru.den.podplay.viewmodel.SearchViewModel
import timber.log.Timber

class SearchFragment : Fragment(), PodcastListAdapter.PodcastListApapterListener {
    private lateinit var podcastListAdapter: PodcastListAdapter
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by activityViewModels()
    private lateinit var searchMenuItem: MenuItem
    private var searchOffset = 0
    private var isLoading = false

    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.d("onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScrollListener()
        setupAdapter()
        setupObservable()
        setupPodcastListView()
    }

    private fun setupObservable() {
        if (searchViewModel.podcasts.value!!.isNotEmpty()) {
            podcastListAdapter.addItems(searchViewModel.podcasts.value!!)
        }
        searchViewModel.podcasts.observe(viewLifecycleOwner, Observer { podcasts ->
            if (isLoading) {
                podcastListAdapter.hideLoader()
                isLoading = false
            }

            podcastListAdapter.addItems(podcasts)
        })

        searchViewModel.isLoading.observe(viewLifecycleOwner, Observer { value ->
            if (value) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        })

        searchViewModel.searchTerm.observe(viewLifecycleOwner, Observer { term ->
            activity?.title = term
        })
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value ?: return

        if (podcasts.isNotEmpty()) {
            activity?.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        } else {
            activity?.title = getString(R.string.app_name)
        }
    }

    private fun setupAdapter() {
        val layoutManager = LinearLayoutManager(requireActivity())
        val dividerItemDecoration = DividerItemDecoration(
            rv_podcast.context, layoutManager.orientation
        )
        rv_podcast.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(this)

        rv_podcast.layoutManager = layoutManager
        rv_podcast.adapter = podcastListAdapter
        rv_podcast.setHasFixedSize(true)
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUri ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToPodcastDetailsFragment())
            } else {
                showError("Error loading feed: $feedUrl")
            }
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun setupScrollListener() {
        rv_podcast.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findLastCompletelyVisibleItemPosition() == searchOffset - 1) {
                    if (!isLoading) {
                        isLoading = true
                        loadMore()
                    }
                }
            }
        })
    }

    private fun loadMore() {
        podcastListAdapter.showLoader()
        performSearch(searchViewModel.searchTerm.value)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        setupSearchView()
    }

    private fun setupSearchView() {
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                return true
            }
        })

        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val text = cursor.getString(
                    cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                )
                podcastListAdapter.setSearchData(listOf())
                performSearch(text)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                podcastListAdapter.setSearchData(listOf())
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Timber.d(newText)
                return true
            }
        })

        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.isQueryRefinementEnabled = true

        searchMenuItem.isVisible = true
    }

    private fun showProgressBar() {
        progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progress_bar.visibility = View.GONE
    }

    private fun saveRecentQueries(term: String) {
        SearchRecentSuggestions(
            requireContext(),
            PodcastSearchSuggestProvider.AUTHORITY,
            PodcastSearchSuggestProvider.MODE
        ).saveRecentQuery(term, null)
    }

    private fun performSearch(term: String?, limit: Int = 30) {
        if (term == null) return
        showProgressBar()
        saveRecentQueries(term)
        (searchMenuItem.actionView as SearchView).setQuery(term, false)
        searchViewModel.searchPodcasts(term, limit, searchOffset)
    }
}
