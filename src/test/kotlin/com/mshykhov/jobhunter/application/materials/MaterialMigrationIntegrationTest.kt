package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class MaterialMigrationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `creates versioned encrypted material storage`() {
        val expected =
            setOf(
                "fact_catalog_versions",
                "candidate_profile_versions",
                "writing_style_versions",
                "job_description_versions",
                "application_material_packages",
                "application_material_requests",
                "application_material_revisions",
                "application_material_artifacts",
                "application_material_revision_artifacts",
                "material_claim_usages",
                "material_generation_attempts",
                "material_validation_results",
            )
        val actual =
            jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ANY (?)
                """.trimIndent(),
                String::class.java,
                jdbcTemplate.dataSource!!.connection.use { connection ->
                    connection.createArrayOf("text", expected.toTypedArray())
                },
            ).toSet()

        assertEquals(expected, actual)
    }

    @Test
    fun `stores private bodies only in encrypted binary columns`() {
        val columns =
            jdbcTemplate.queryForList(
                """
                SELECT table_name || '.' || column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'fact_catalog_versions',
                    'candidate_profile_versions',
                    'writing_style_versions',
                    'job_description_versions',
                    'application_material_artifacts'
                  )
                  AND data_type = 'bytea'
                """.trimIndent(),
                String::class.java,
            ).toSet()

        assertEquals(
            setOf(
                "fact_catalog_versions.encrypted_content",
                "candidate_profile_versions.encrypted_content",
                "writing_style_versions.encrypted_content",
                "job_description_versions.encrypted_raw_content",
                "job_description_versions.encrypted_normalized_content",
                "application_material_artifacts.encrypted_content",
            ),
            columns,
        )
    }

    @Test
    fun `removes legacy outreach storage`() {
        val remaining =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND ((table_name = 'user_jobs' AND column_name IN ('cover_letter', 'recruiter_message'))
                    OR table_name = 'outreach_settings')
                """.trimIndent(),
                Long::class.java,
            )

        assertEquals(0, remaining)
    }
}
