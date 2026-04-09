package com.neo.browser.logic.search

class NeoSearchEngineProvider(
    private var selectedEngine: NeoSearchEngine = NeoSearchEngine.GOOGLE,
    private var customSearchTemplate: String? = null,
) {
    fun selected(): NeoSearchEngine = selectedEngine

    fun updateSelected(engine: NeoSearchEngine) {
        selectedEngine = engine
    }

    fun updateCustomSearchTemplate(template: String?) {
        customSearchTemplate = template
    }

    fun createSearchUrl(query: String): String {
        return selectedEngine.createSearchUrl(query, customSearchTemplate)
    }
}
