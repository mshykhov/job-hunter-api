package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ColdFilterChainTest {
    private val chain = ColdFilterChain()

    @Nested
    inner class SourceFilter {
        @Test
        fun `should pass when no disabled sources`() {
            val result = chain.evaluate(TestFixtures.jobEntity(source = JobSource.DOU), TestFixtures.userPreferenceEntity())
            assertIs<FilterResult.Passed>(result)
        }

        @ParameterizedTest
        @EnumSource(JobSource::class)
        fun `should reject when job source is disabled`(source: JobSource) {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(source = source),
                    TestFixtures.userPreferenceEntity(disabledSources = listOf(source)),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("source", result.filter)
        }

        @Test
        fun `should pass when job source is not in disabled list`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(source = JobSource.DOU),
                    TestFixtures.userPreferenceEntity(disabledSources = listOf(JobSource.DJINNI)),
                )
            assertIs<FilterResult.Passed>(result)
        }
    }

    @Nested
    inner class RemoteFilter {
        @ParameterizedTest(name = "remote={0}, remoteOnly={1} -> passed={2}")
        @CsvSource(
            "false, false, true",
            "true, false, true",
            "true, true, true",
            ", true, true",
            "false, true, false",
        )
        fun `should filter by remote status`(
            remote: Boolean?,
            remoteOnly: Boolean,
            shouldPass: Boolean,
        ) {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(remote = remote),
                    TestFixtures.userPreferenceEntity(remoteOnly = remoteOnly),
                )
            if (shouldPass) {
                assertIs<FilterResult.Passed>(result)
            } else {
                assertIs<FilterResult.Rejected>(result)
                assertEquals("remote", result.filter)
            }
        }
    }

    @Nested
    inner class ExcludedKeywordsFilter {
        @Test
        fun `should pass when no excluded keywords`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(description = "Kotlin Spring Boot"),
                    TestFixtures.userPreferenceEntity(),
                )
            assertIs<FilterResult.Passed>(result)
        }

        @ParameterizedTest(name = "keyword={0} in description={1}")
        @CsvSource(
            "php, Looking for a PHP developer",
            "java, Senior JAVA Engineer needed",
            "c++, We need a C++ expert",
        )
        fun `should reject when description contains excluded keyword case-insensitively`(
            keyword: String,
            description: String,
        ) {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(description = description),
                    TestFixtures.userPreferenceEntity(excludedKeywords = listOf(keyword)),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("excludedKeyword", result.filter)
        }

        @Test
        fun `should pass when excluded keyword is not found`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(description = "Senior Kotlin developer"),
                    TestFixtures.userPreferenceEntity(excludedKeywords = listOf("php", "ruby")),
                )
            assertIs<FilterResult.Passed>(result)
        }
    }

    @Nested
    inner class ExcludedTitleKeywordsFilter {
        @Test
        fun `should reject when title contains excluded keyword`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(title = "Junior PHP Developer"),
                    TestFixtures.userPreferenceEntity(excludedTitleKeywords = listOf("junior")),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("excludedTitleKeyword", result.filter)
        }

        @Test
        fun `should pass when title does not contain excluded keyword`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(title = "Senior Kotlin Developer"),
                    TestFixtures.userPreferenceEntity(excludedTitleKeywords = listOf("junior", "intern")),
                )
            assertIs<FilterResult.Passed>(result)
        }
    }

    @Nested
    inner class ExcludedCompaniesFilter {
        @ParameterizedTest(name = "company={0}, excluded={1} -> rejected={2}")
        @CsvSource(
            "BadCompany Inc, badcompany, true",
            "GoodCorp, badcompany, false",
        )
        fun `should filter by company name`(
            company: String,
            excluded: String,
            shouldReject: Boolean,
        ) {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(company = company),
                    TestFixtures.userPreferenceEntity(excludedCompanies = listOf(excluded)),
                )
            if (shouldReject) {
                assertIs<FilterResult.Rejected>(result)
                assertEquals("excludedCompany", result.filter)
            } else {
                assertIs<FilterResult.Passed>(result)
            }
        }

        @Test
        fun `should pass when company is null`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(company = null),
                    TestFixtures.userPreferenceEntity(excludedCompanies = listOf("badcompany")),
                )
            assertIs<FilterResult.Passed>(result)
        }
    }

    @Nested
    inner class CategoriesFilter {
        @Test
        fun `should pass when no categories specified`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(description = "Random job"),
                    TestFixtures.userPreferenceEntity(),
                )
            assertIs<FilterResult.Passed>(result)
        }

        @Test
        fun `should pass when job text contains a category word`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(description = "We need a Kotlin backend developer"),
                    TestFixtures.userPreferenceEntity(categories = listOf("Kotlin", "Java")),
                )
            assertIs<FilterResult.Passed>(result)
        }

        @Test
        fun `should reject when no category matches`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(title = "Recruiter", description = "HR position for recruiting"),
                    TestFixtures.userPreferenceEntity(categories = listOf("Kotlin", "Java", "Python")),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("category", result.filter)
        }
    }

    @Nested
    inner class FilterPriority {
        @Test
        fun `should reject by source before checking other filters`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(source = JobSource.DJINNI, remote = false, description = "PHP developer"),
                    TestFixtures.userPreferenceEntity(
                        disabledSources = listOf(JobSource.DJINNI),
                        remoteOnly = true,
                        excludedKeywords = listOf("php"),
                    ),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("source", result.filter)
        }

        @Test
        fun `should reject by remote before checking keywords`() {
            val result =
                chain.evaluate(
                    TestFixtures.jobEntity(remote = false, description = "PHP developer"),
                    TestFixtures.userPreferenceEntity(
                        remoteOnly = true,
                        excludedKeywords = listOf("php"),
                    ),
                )
            assertIs<FilterResult.Rejected>(result)
            assertEquals("remote", result.filter)
        }
    }
}
