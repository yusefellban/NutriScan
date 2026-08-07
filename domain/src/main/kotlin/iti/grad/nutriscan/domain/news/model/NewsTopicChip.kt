package iti.grad.nutriscan.domain.news.model

data class NewsTopicChip(
    val id: String,
    val label: String,
    /** Null for the "All" chip — it has no search keyword, it switches to top-headlines. */
    val searchKeyword: String?,
) {
    companion object {
        const val ALL_CHIP_ID = "all"
    }
}
