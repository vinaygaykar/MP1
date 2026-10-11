package io.github.vinaygaykar.mp1.data.network

import io.github.vinaygaykar.mp1.data.network.models.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Deezer API.
 * Used primarily for fetching artist artwork.
 */
interface DeezerApiService {

    /**
     * Search for an artist by name.
     * @param query Artist name to search for
     * @param limit Maximum number of results to return
     * @return Search response containing list of matching artists
     */
    @GET("search/artist")
    suspend fun searchArtist(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("order") order: String = "RANKING"
    ): DeezerSearchResponse
}
