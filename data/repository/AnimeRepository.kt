package com.animapper.app.data.repository

import com.animapper.app.data.api.AniMapperApi
import com.animapper.app.data.api.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(private val api: AniMapperApi) {
    suspend fun trending() = api.search(sortBy = "POPULARITY", sortOrder = "DESC", limit = 20).results
    suspend fun seasonal(season: String, year: Int) =
        api.search(season = season, seasonYear = year, sortBy = "POPULARITY", sortOrder = "DESC", limit = 20).results
    suspend fun topRated() =
        api.search(status = "FINISHED", sortBy = "POPULARITY", sortOrder = "DESC", limit = 20).results
    suspend fun byCountry(country: String) = api.search(countryOfOrigin = country, limit = 20).results
    suspend fun search(q: String) = api.search(title = q, limit = 30).results
    suspend fun filter(genreIds: String? = null, format: String? = null, seasonYear: Int? = null, countryOfOrigin: String? = null) =
        api.search(genreIds = genreIds, format = format, seasonYear = seasonYear,
            countryOfOrigin = countryOfOrigin, sortBy = "POPULARITY", sortOrder = "DESC", limit = 30).results
    suspend fun detail(id: Int) = api.metadata(id).result
    suspend fun autocomplete(q: String) = api.autocomplete(q).suggestions
    suspend fun episodes(id: Int, provider: String) = api.episodes(id, provider).episodes
    suspend fun source(episodeId: String, provider: String, server: String?) =
        api.source(episodeId, provider, server).result
}
