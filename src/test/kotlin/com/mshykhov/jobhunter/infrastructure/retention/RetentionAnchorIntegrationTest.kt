package com.mshykhov.jobhunter.infrastructure.retention

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import kotlin.test.assertEquals

class RetentionAnchorIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var retentionAnchor: RetentionAnchor

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should anchor on the installed_on of the last-seen-at migration`() {
        val installedOn =
            jdbcTemplate.queryForObject(
                "SELECT installed_on FROM flyway_schema_history WHERE version = '25'",
                Timestamp::class.java,
            )

        assertEquals(installedOn?.toInstant(), retentionAnchor.instant())
    }
}
