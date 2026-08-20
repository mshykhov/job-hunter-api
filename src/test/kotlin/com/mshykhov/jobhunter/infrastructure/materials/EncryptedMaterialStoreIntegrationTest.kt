package com.mshykhov.jobhunter.infrastructure.materials

import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncryptedMaterialStoreIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var store: EncryptedMaterialStore

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `stores encrypted bytes and returns them only to their owner`() {
        val owner = userRepository.save(TestFixtures.userEntity())
        val content = "%PDF synthetic private CV".toByteArray()

        val stored = store.store(owner.id, MaterialKind.CV_PDF, "application/pdf", content)

        assertEquals(EncryptedMaterialStore.sha256(content), stored.plaintextSha256)
        assertContentEquals(content, store.read(owner.id, stored.id))
        assertFailsWith<NotFoundException> { store.read(UUID.randomUUID(), stored.id) }
    }

    @Test
    fun `deduplicates equal owner artifact content`() {
        val owner = userRepository.save(TestFixtures.userEntity())
        val content = "short direct cover letter".toByteArray()

        val first = store.store(owner.id, MaterialKind.COVER_LETTER, "text/plain", content)
        val second = store.store(owner.id, MaterialKind.COVER_LETTER, "text/plain", content)

        assertEquals(first.id, second.id)
    }

    @Test
    fun `rejects mismatched hashes and media types`() {
        val owner = userRepository.save(TestFixtures.userEntity())

        assertFailsWith<ValidationException> {
            store.store(owner.id, MaterialKind.CV_PDF, "application/pdf", "pdf".toByteArray(), "0".repeat(64))
        }
        assertFailsWith<ValidationException> {
            store.store(owner.id, MaterialKind.CV_PDF, "text/html", "pdf".toByteArray())
        }
    }
}
