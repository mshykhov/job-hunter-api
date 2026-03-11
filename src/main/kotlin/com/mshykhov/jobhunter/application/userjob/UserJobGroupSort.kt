package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.domain.Sort

enum class UserJobGroupSort(val sort: Sort) {
    SCORE(Sort.by(Sort.Direction.DESC, "aiRelevanceScore").and(Sort.by(Sort.Direction.DESC, "id"))),
    MATCHED(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))),
}
