package io.github.vinaygaykar.mp1.data.repo

import android.util.Log
import android.util.LruCache
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.network.DeezerApiService
import io.github.vinaygaykar.mp1.data.network.models.DeezerArtist
import io.github.vinaygaykar.mp1.utils.NetworkRetryUtils
import io.github.vinaygaykar.mp1.utils.isRetryableNetworkError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching and caching artist images from Deezer API.
 * Uses both in-memory LRU cache and Room database for persistent storage.
 */
@Singleton
class ArtistImageRepository @Inject constructor(
    private val deezerApiService: DeezerApiService,
    private val artistDao: ArtistDao
) {
    companion object {
        private const val TAG = "ArtistImageRepository"
        private const val CACHE_SIZE = 100 // Number of artist images to cache in memory
        private const val PREFETCH_CONCURRENCY = 3 // Limit parallel API calls
        private val deezerSizeRegex = Regex("/\\d{2,4}x\\d{2,4}([\\-.])")
        private const val NETWORK_RETRY_ATTEMPTS = 3
        private const val NETWORK_RETRY_INITIAL_DELAY_MS = 500L
    }

    // In-memory LRU cache for quick access
    private val memoryCache = LruCache<String, String>(CACHE_SIZE)

    // Mutex to prevent duplicate API calls for the same artist
    private val fetchMutex = Mutex()
    private val pendingFetches = mutableSetOf<String>()

    // Semaphore to limit concurrent API calls during prefetch
    private val prefetchSemaphore = Semaphore(PREFETCH_CONCURRENCY)

    // Set to track artists for whom image fetching failed (e.g. not found), to avoid retrying in the same session
    private val failedFetches = mutableSetOf<String>()

    /**
     * Get artist image URL, fetching from Deezer if not cached.
     * @param artistName Name of the artist
     * @param artistId Room database ID of the artist
     * @return Image URL or null if not found
     */
    suspend fun getArtistImageUrl(
        artistName: String,
        artistId: String,
        ignoreFailedCache: Boolean = false
    ): String? {
        if (artistName.isBlank()) return null

        val normalizedName = artistName.trim().lowercase()

        // 1. Check memory cache first
        memoryCache.get(normalizedName)?.let { cachedUrl ->
            return cachedUrl
        }

        // 2. Check if previously failed in this session
        if (!ignoreFailedCache && failedFetches.contains(normalizedName)) {
            return null
        }

        // 3. Check Room database cache
        val dbCachedUrl = withContext(Dispatchers.IO) {
            artistDao.getArtistImageUrl(artistId)
                ?: artistDao.getArtistImageUrlByNormalizedName(artistName)
        }
        if (!dbCachedUrl.isNullOrEmpty()) {
            val upgradedDbUrl = upgradeToHighResDeezerUrl(dbCachedUrl)
            memoryCache.put(normalizedName, upgradedDbUrl)
            if (upgradedDbUrl != dbCachedUrl) {
                withContext(Dispatchers.IO) {
                    artistDao.updateArtistImageUrl(artistId, upgradedDbUrl)
                }
            }
            return upgradedDbUrl
        }

        // 4. Fetch from Deezer API
        return fetchAndCacheArtistImage(artistName, artistId, normalizedName)
    }

    /**
     * Prefetch artist images for a list of artists in background.
     * Useful for batch loading when displaying artist lists.
     * @param artists List of Pair(artistId, artistName)
     */
    suspend fun prefetchArtistImages(artists: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        try {
            // Process in small chunks to avoid creating hundreds of coroutines simultaneously.
            artists.chunked(PREFETCH_CONCURRENCY * 4).forEach { chunk ->
                chunk.map { (artistId, artistName) ->
                    async {
                        try {
                            val normalizedName = artistName.trim().lowercase()
                            if (memoryCache.get(normalizedName) == null && !failedFetches.contains(normalizedName)) {
                                prefetchSemaphore.withPermit {
                                    getArtistImageUrl(artistName, artistId)
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to prefetch image for $artistName: ${e.message}")
                        }
                    }
                }.awaitAll()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during artist image prefetch batch: ${e.message}")
        }
    }

    private suspend fun fetchAndCacheArtistImage(
        artistName: String,
        artistId: String,
        normalizedName: String
    ): String? {
        // Prevent duplicate fetches for the same artist
        fetchMutex.withLock {
            if (pendingFetches.contains(normalizedName)) {
                return null // Already fetching
            }
            pendingFetches.add(normalizedName)
        }

        return try {
            withContext(Dispatchers.IO) {
                val response = withNetworkRetry("deezer_search:$artistName") {
                    deezerApiService.searchArtist(artistName, limit = 1)
                }
                val deezerArtist = response.data.firstOrNull()

                if (deezerArtist != null) {
                    val imageUrl = (
                        deezerArtist.pictureXl
                            ?: deezerArtist.pictureBig
                            ?: deezerArtist.pictureMedium
                            ?: deezerArtist.picture
                        )?.let(::upgradeToHighResDeezerUrl)

                    if (!imageUrl.isNullOrEmpty()) {
                        // Cache in memory
                        memoryCache.put(normalizedName, imageUrl)

                        // Cache in database
                        artistDao.updateArtistImageUrl(artistId, imageUrl)

                        failedFetches.remove(normalizedName)

                        Log.d(TAG, "Fetched and cached image for $artistName: $imageUrl")
                        imageUrl
                    } else {
                        failedFetches.add(normalizedName)
                        null
                    }
                } else {
                    Log.d(TAG, "No Deezer artist found for: $artistName")
                    failedFetches.add(normalizedName) // Mark as explicitly missing on Deezer
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching artist image for $artistName: ${e.message}")
            // Do not add to failedFetches on transient network errors or rate limits,
            // allowing retries when the item is scrolled into view again.
            null
        } finally {
            fetchMutex.withLock {
                pendingFetches.remove(normalizedName)
            }
        }
    }

    private suspend fun <T> withNetworkRetry(
        operationName: String,
        maxAttempts: Int = NETWORK_RETRY_ATTEMPTS,
        initialDelayMs: Long = NETWORK_RETRY_INITIAL_DELAY_MS,
        block: suspend () -> T
    ): T {
        return NetworkRetryUtils.withNetworkRetry(
            operationName = operationName,
            maxAttempts = maxAttempts,
            initialDelayMs = initialDelayMs,
            shouldRetry = { throwable -> throwable.isRetryableNetworkError() },
            onRetry = { attempt, attempts, throwable ->
                Log.d(TAG, "Retrying $operationName after failure ($attempt/$attempts): ${throwable.message}")
            },
            block = block
        )
    }

    /**
     * Search Deezer API for artist candidate image options, ordered and sorted by popularity/fan count.
     */
    suspend fun searchArtistCandidates(query: String, limit: Int = 15): List<DeezerArtist> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val response = withNetworkRetry("deezer_search_candidates:$query") {
                    deezerApiService.searchArtist(query, limit = limit, order = "RANKING")
                }
                response.data
                    .filter { candidate ->
                        !candidate.pictureXl.isNullOrEmpty() ||
                        !candidate.pictureBig.isNullOrEmpty() ||
                        !candidate.pictureMedium.isNullOrEmpty() ||
                        !candidate.picture.isNullOrEmpty()
                    }
                    .sortedByDescending { candidate -> candidate.fanCount }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching artist candidates for $query: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Save a user-selected artist image URL directly to memory cache and Room database.
     */
    suspend fun updateArtistImageUrl(artistId: String, artistName: String, imageUrl: String) {
        val normalizedName = artistName.trim().lowercase()
        val upgradedUrl = upgradeToHighResDeezerUrl(imageUrl)
        memoryCache.put(normalizedName, upgradedUrl)
        failedFetches.remove(normalizedName)
        withContext(Dispatchers.IO) {
            artistDao.updateArtistImageUrl(artistId, upgradedUrl)
        }
    }

    /**
     * Clear all in-memory cached images and failed fetches memory.
     */
    fun clearCache() {
        memoryCache.evictAll()
        failedFetches.clear()
    }

    private fun upgradeToHighResDeezerUrl(url: String): String {
        if (!url.contains("dzcdn.net/images/artist")) return url
        return deezerSizeRegex.replace(url, "/1000x1000$1")
    }
}
