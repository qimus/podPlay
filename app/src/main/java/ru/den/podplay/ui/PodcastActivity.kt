package ru.den.podplay.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.work.*
import kotlinx.android.synthetic.main.activity_podcast.*
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem
import ru.den.podplay.R
import ru.den.podplay.db.PodplayDatabase
import ru.den.podplay.repository.ItunesRepo
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.service.FeedService
import ru.den.podplay.service.ItunesService
import ru.den.podplay.viewmodel.PodcastViewModel
import ru.den.podplay.viewmodel.SearchViewModel
import ru.den.podplay.worker.EpisodeUpdateWorker
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(),
    PodcastDetailsFragment.OnPodcastDetailsListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var navController: NavController

    companion object {
        const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        addBackStackListener()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        val menuItems = arrayOf(
            CbnMenuItem(
                R.drawable.ic_search,
                R.drawable.avd_search,
                R.id.searchFragment
            ),
            CbnMenuItem(
                R.drawable.ic_download,
                R.drawable.avd_download,
                R.id.downloadedPodcastFragment
            )
        )
        bottomNavigation.setMenuItems(menuItems)
        bottomNavigation.setupWithNavController(navController)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.itunesRepo = ItunesRepo(service)
        val db = PodplayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(FeedService.instance, podcastDao)
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showBottomNavBar()
            }
        }
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

    fun hideBottomNavBar() {
        bottomNavigation?.visibility = View.INVISIBLE
    }

    fun showBottomNavBar() {
        bottomNavigation?.visibility = View.VISIBLE
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        navController.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        navController.popBackStack()
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        val mediaInfo = EpisodePlayerFragment.MediaInfo(
            podcastViewModel.activePodcastViewData?.feedTitle,
            podcastViewModel.activeEpisodeViewData?.title,
            podcastViewModel.activeEpisodeViewData?.description,
            podcastViewModel.activeEpisodeViewData?.isVideo ?: false,
            podcastViewModel.activeEpisodeViewData?.mediaUrl,
            podcastViewModel.activePodcastViewData?.imageUrl,
            podcastViewModel.activeEpisodeViewData?.downloadInfo?.file
        )
        navController.navigate(
            PodcastDetailsFragmentDirections
                .actionPodcastDetailsFragmentToEpisodePlayerFragment(mediaInfo)
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

//    private fun handleIntent(intent: Intent) {
//        if (intent.action == Intent.ACTION_SEARCH) {
//            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
//            searchCount = 0
//            searchTerm = query
//            podcastListAdapter.setSearchData(listOf())
//            performSearch(query)
//        }

        //navigation by click on notification
//        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
//        if (podcastFeedUrl != null) {
//            podcastViewModel.setActivePodcast(podcastFeedUrl) {
//                it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }
//            }
//        }
//    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        //handleIntent(intent)
    }
}
