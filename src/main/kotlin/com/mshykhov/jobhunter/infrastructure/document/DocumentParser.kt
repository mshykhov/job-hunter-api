package com.mshykhov.jobhunter.infrastructure.document

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class DocumentParser {
    companion object {
        private const val MIME_PDF = "application/pdf"
        private const val MIME_DOC = "application/msword"
        private const val MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        val SUPPORTED_TYPES = setOf(MIME_PDF, MIME_DOC, MIME_DOCX)
    }

    fun extractText(
        inputStream: InputStream,
        contentType: String,
    ): String =
        when (contentType) {
            MIME_PDF -> extractPdfText(inputStream)
            MIME_DOC -> extractDocText(inputStream)
            MIME_DOCX -> extractDocxText(inputStream)
            else -> throw IllegalArgumentException(
                "Unsupported file type: $contentType. Supported types: PDF, DOC, DOCX",
            )
        }

    private fun extractPdfText(inputStream: InputStream): String {
        val bytes = inputStream.readAllBytes()
        return Loader.loadPDF(bytes).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    private fun extractDocText(inputStream: InputStream): String =
        HWPFDocument(inputStream).use { document ->
            WordExtractor(document).use { extractor ->
                extractor.text
            }
        }

    private fun extractDocxText(inputStream: InputStream): String =
        XWPFDocument(inputStream).use { document ->
            XWPFWordExtractor(document).use { extractor ->
                extractor.text
            }
        }
}
