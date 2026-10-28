package com.polartube.app.ui.search

data class SearchUiState(val query: String = "", val loading: Boolean = false,
                         val results: List<SearchItem> = emptyList(), val error: String? = null)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val search = SearchUseCase((app as PolarTubeApp).repository)
    private val _ui = MutableStateFlow(SearchUiState())
    val ui = _ui.asStateFlow()

    fun onQueryChange(q: String) = _ui.update { it.copy(query = q) }

    fun submit() {
        val q = _ui.value.query.trim().ifEmpty { return }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { search(q) }
                .onSuccess { r -> _ui.update { it.copy(loading = false, results = r) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message) } }
        }
    }
}
