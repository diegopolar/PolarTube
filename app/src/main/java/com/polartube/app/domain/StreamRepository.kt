package com.polartube.app.domain

interface StreamRepository {
    suspend fun search(query: String): List<SearchItem>
    suspend fun streamDetails(url: String): StreamDetails
}

class SearchUseCase(private val repo: StreamRepository) { suspend operator fun invoke(q: String) = repo.search(q) }
class GetStreamUseCase(private val repo: StreamRepository) { suspend operator fun invoke(u: String) = repo.streamDetails(u) }
