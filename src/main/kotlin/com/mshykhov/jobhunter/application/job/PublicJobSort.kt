package com.mshykhov.jobhunter.application.job

import org.springframework.data.domain.Sort

enum class PublicJobSort(
    val sort: Sort,
) {
    PUBLISHED(Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id"))),
    SCRAPED(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))),
}
