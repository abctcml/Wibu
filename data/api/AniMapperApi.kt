package com.animapper.app.data.api

import com.animapper.app.data.api.models.*
import retrofit2.http.GET
import retrofit2.http.Query

interface AniMapperApi {
    @GET("search")
    suspend fun search(
        @Query("title") title: String? = null,
        @Query("mediaType") mediaType: String? = "ANIME",
        @Query("season") season: String? = null,
        @Query("seasonYear") seasonYear: Int? = null,
        @Query("genreIds") genreIds: String? = null,
        @Query("format") format: String? = null,
        @Query("status") status: String? = null,
        @Query("countryOfOrigin") countryOfOrigin: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): SearchResponse

    @GET("metadata")
    suspend fun metadata(@Query("id") id: Int): MetadataResponse

    @GET("autocomplete")
    suspend fun autocomplete(
        @Query("q") q: String,
        @Query("mediaType") mediaType: String? = "ANIME",
        @Query("limit") limit: Int = 5,
    ): AutocompleteResponse

    @GET("stream/episodes")
    suspend fun episodes(
        @Query("id") id: Int,
        @Query("provider") provider: String,
        @Query("server") server: String? = null,
    ): EpisodesResponse

    @GET("stream/source")
    suspend fun source(
        @Query("episodeData") episodeData: String,
        @Query("provider") provider: String,
        @Query("server") server: String? = null,
    ): SourceResponse
}
