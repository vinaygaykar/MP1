package io.github.vinaygaykar.mp1.data.network

import io.github.vinaygaykar.mp1.data.network.models.LrcLibResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibService {
    @GET("get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String?,
        @Query("duration") duration: Int?
    ): Response<LrcLibResponse>

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LrcLibResponse>>
}
