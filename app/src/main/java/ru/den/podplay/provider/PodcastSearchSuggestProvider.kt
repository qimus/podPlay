package ru.den.podplay.provider

import android.content.SearchRecentSuggestionsProvider

class PodcastSearchSuggestProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "ru.den.podplay.PodcastSearchSuggestProvider"
        const val MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES
    }
}
