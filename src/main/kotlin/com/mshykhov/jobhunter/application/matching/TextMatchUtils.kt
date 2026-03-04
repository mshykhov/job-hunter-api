package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.job.JobEntity
import java.util.concurrent.ConcurrentHashMap

internal object TextMatchUtils {
    private val wordBoundaryCache = ConcurrentHashMap<String, Regex>()

    fun buildSearchText(job: JobEntity): String =
        listOfNotNull(job.title, job.company, job.description, job.location, job.salary)
            .joinToString(" ")
            .lowercase()

    fun containsWord(
        text: String,
        word: String,
    ): Boolean {
        val key = word.lowercase()
        val pattern =
            wordBoundaryCache.getOrPut(key) {
                val escaped = Regex.escape(key)
                Regex("(?<![\\w-])$escaped(?![\\w-])")
            }
        return pattern.containsMatchIn(text)
    }

    fun containsSubstring(
        text: String,
        keyword: String,
    ): Boolean = text.contains(keyword.lowercase())
}
