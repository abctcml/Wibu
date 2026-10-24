package com.animapper.app.data.api.models

import com.squareup.moshi.Json

data class SearchResponse(
    val success: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val hasNextPage: Boolean = false,
)

data class SearchResult(
    val id: Int,
    val mediaType: String = "ANIME",
    val titles: Map<String, String> = emptyMap(),
    val images: Images = Images(),
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: String? = null,
    val format: String? = null,
)

data class Images(
    val coverXl: String? = null,
    val coverLg: String? = null,
    val coverMd: String? = null,
    val coverColor: String? = null,
    val bannerUrl: String? = null,
)

data class MetadataResponse(val success: Boolean = false, val result: MediaDetail? = null)

data class MediaDetail(
    val id: Int,
    val mediaType: String = "ANIME",
    val format: String? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val totalUnits: Int? = null,
    val unitDurationMin: Int? = null,
    val countryOfOrigin: String? = null,
    val titles: Map<String, String> = emptyMap(),
    val descriptions: Map<String, String> = emptyMap(),
    val images: Images = Images(),
    val trailer: Trailer? = null,
    val genres: List<Genre> = emptyList(),
    val studios: List<Studio> = emptyList(),
    val relations: List<Relation> = emptyList(),
    val streamingProviders: List<Provider> = emptyList(),
)

data class Trailer(val site: String? = null, @Json(name="trailerId") val trailerId: String? = null, val thumbnail: String? = null)
data class Genre(val id: Int, val name: String = "")
data class Studio(val id: Int, val name: String = "")
data class Relation(val id: Int, val title: String = "", val relationType: String = "", val mediaType: String = "ANIME")
data class Provider(val name: String = "", val providerMediaId: String? = null)

data class EpisodesResponse(
    val success: Boolean = false,
    val provider: String = "",
    val total: Int = 0,
    val hasNextPage: Boolean = false,
    val episodes: List<Episode> = emptyList(),
)

data class Episode(val episodeNumber: Int = 0, val episodeId: String = "", val server: String? = null)
data class ServersResponse(val success: Boolean = false, val provider: String = "", val servers: List<String> = emptyList())
data class SourceResponse(val success: Boolean = false, val result: StreamSource? = null)
data class StreamSource(
    val type: String = "HLS",
    val url: String? = null,
    @Json(name="corsProxyRequired") val corsProxyRequired: Boolean = false,
)
data class AutocompleteResponse(val success: Boolean = false, val query: String = "", val suggestions: List<Suggestion> = emptyList())
data class Suggestion(val id: Int, val title: String = "", val mediaType: String = "ANIME")
