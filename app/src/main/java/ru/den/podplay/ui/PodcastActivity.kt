package ru.den.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import kotlinx.android.synthetic.main.activity_podcast.*
import ru.den.podplay.R
import ru.den.podplay.adapter.PodcastListAdapter
import ru.den.podplay.db.PodplayDatabase
import ru.den.podplay.repository.ItunesRepo
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.service.FeedService
import ru.den.podplay.service.ItunesService
import ru.den.podplay.viewmodel.PodcastViewModel
import ru.den.podplay.viewmodel.SearchViewModel
import ru.den.podplay.worker.EpisodeUpdateWorker
import java.util.concurrent.TimeUnit

interface BottomBarHolder {
    fun showBottomNavBar()
    fun hideBottomNavBar()
}

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListApapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener, BottomBarHolder {
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private var searchCount = 0
    private var searchTerm = ""
    private var isLoading = false

    companion object {
        const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        const val TAG_DOWNLOADS_FRAGMENT = "DownloadsFragment"
        const val TAG_EPISODE_UPDATE_JOB = "ru.den.podplay.episodes"
        const val TAG_PLAYER_FRAGMENT = "TAG_PLAYER_FRAGMENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        addBackStackListener()
        initScrollListener()
        setupBottomNavigation()
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    private fun createDownloadsPodcastFragment(): DownloadedPodcastFragment {
        var downloadPodcastFragment = supportFragmentManager
            .findFragmentByTag(TAG_DOWNLOADS_FRAGMENT) as? DownloadedPodcastFragment

        if (downloadPodcastFragment == null) {
            downloadPodcastFragment = DownloadedPodcastFragment.newInstance()
        }

        return downloadPodcastFragment
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })

        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (supportFragmentManager.backStackEntryCount > 0) {
            rv_podcast.visibility = View.INVISIBLE
        }

        if (rv_podcast.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }

        return true
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_podcasts -> {
                    rv_podcast.visibility = View.VISIBLE
                    searchMenuItem.isVisible = true
                    showSubscribedPodcasts()
                    removeFragment()
                    true
                }
                R.id.menu_downloads -> {
                    showDownloadsFragment()
                    toolbar.title = getString(R.string.downloads)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.podcastDetailsContainer)
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .remove(fragment)
                .commit()
        }
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.itunesRepo = ItunesRepo(service)
        val db = PodplayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(FeedService.instance, podcastDao)
    }

    private fun updateControls() {
        rv_podcast.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        rv_podcast.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            rv_podcast.context, layoutManager.orientation
        )
        rv_podcast.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this)
        rv_podcast.adapter = podcastListAdapter
    }

    private fun initScrollListener() {
        rv_podcast.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findLastCompletelyVisibleItemPosition() == searchCount - 1) {
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
        performSearch(searchTerm)
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                rv_podcast.visibility = View.VISIBLE
                showBottomNavBar()
            }
        }
    }

    private fun showProgressBar() {
        progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progress_bar.visibility = View.GONE
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUri ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed: $feedUrl")
            }
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun scheduleJobs() {
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_DETAILS_FRAGMENT,
            ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment")
            .commit()

        rv_podcast.visibility = View.INVISIBLE
        hideBottomNavBar()
        searchMenuItem.isVisible = false
    }

    private fun showDownloadsFragment() {
        val fragment = createDownloadsPodcastFragment()
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.podcastDetailsContainer, fragment, TAG_DOWNLOADS_FRAGMENT)
            .addToBackStack(TAG_DOWNLOADS_FRAGMENT)
            .commit()

        rv_podcast.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    override fun hideBottomNavBar() {
        bottomNavigation.visibility = View.INVISIBLE
    }

    override fun showBottomNavBar() {
        bottomNavigation.visibility = View.VISIBLE
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {
        return EpisodePlayerFragment.newInstance(
            EpisodePlayerFragment.MediaInfo(
                podcastViewModel.activePodcastViewData?.feedTitle,
                podcastViewModel.activeEpisodeViewData?.title,
                podcastViewModel.activeEpisodeViewData?.description,
                podcastViewModel.activeEpisodeViewData?.isVideo ?: false,
                podcastViewModel.activeEpisodeViewData?.mediaUrl,
                podcastViewModel.activePodcastViewData?.imageUrl,
                podcastViewModel.activeEpisodeViewData?.downloadInfo?.file
            )
        )
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()

        supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer, episodePlayerFragment, TAG_PLAYER_FRAGMENT)
            .addToBackStack("PlayerFragment").commit()
        rv_podcast.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun performSearch(term: String, limit: Int = 30) {
        showProgressBar()

        searchViewModel.searchPodcasts(term, limit, searchCount) { results ->
            hideProgressBar()

            if (isLoading) {
                podcastListAdapter.hideLoader()
                isLoading = false
            }

            searchCount += results.size
            toolbar.title = term
            podcastListAdapter.addItems(results)
        }
    }

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            searchCount = 0
            searchTerm = query
            podcastListAdapter.setSearchData(listOf())
            performSearch(query)
        }

        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}
