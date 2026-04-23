package dean.ai.nps.common

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T : Any, R : Any> of(page: Page<T>, transform: (T) -> R): PageResponse<R> = PageResponse(
            content = page.content.map(transform),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
