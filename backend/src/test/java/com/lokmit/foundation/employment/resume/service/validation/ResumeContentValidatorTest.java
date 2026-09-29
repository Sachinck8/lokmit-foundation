package com.lokmit.foundation.employment.resume.service.validation;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.PayloadTooLargeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the A7.6.2 byte-level content validator: real minimal
 * PDF/DOC/DOCX fixtures built in-test, hostile variants rejected, MIME
 * cross-check, size bounds.
 */
class ResumeContentValidatorTest {

    private final ResumeContentValidator validator = new ResumeContentValidator();

    // ------------------------------------------------------------------
    // Fixture builders — real bytes, no fakes
    // ------------------------------------------------------------------

    private static byte[] pdf(String body) {
        // Header + body + xref trailer with %%EOF in the final KiB.
        String doc = "%PDF-1.4\n" + body + "\ntrailer\n<< /Root 1 0 R >>\nstartxref\n0\n%%EOF\n";
        return doc.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] docx(String contentTypesXml) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(contentTypesXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static final String WORD_CT =
            "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>";

    private static final String XLSX_CT =
            "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "</Types>";

    private static final String PPTX_CT =
            "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>"
                    + "</Types>";

    private static byte[] ole2() {
        return new byte[]{
                (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07
        };
    }

    private static byte[] zip(String entryName, String entryContent) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(entryContent.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    // ------------------------------------------------------------------
    // Content detection — accepted formats
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid PDF is detected and accepted")
    void validPdfAccepted() {
        byte[] pdf = pdf("1 0 obj << >> endobj");
        assertThat(validator.validate(pdf, "application/pdf"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.PDF);
    }

    @Test
    @DisplayName("valid DOC/OLE2 signature is detected and accepted")
    void validDocAccepted() {
        assertThat(validator.validate(ole2(), "application/msword"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.DOC);
    }

    @Test
    @DisplayName("valid DOCX is detected and accepted")
    void validDocxAccepted() throws IOException {
        byte[] docx = docx(WORD_CT);
        assertThat(validator.validate(docx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.DOCX);
    }

    // ------------------------------------------------------------------
    // Content detection — PDF edge cases
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PDF header without %%EOF is rejected (truncated)")
    void pdfWithoutEofRejected() {
        String doc = "%PDF-1.4\n1 0 obj << >> endobj\ntrailer\nstartxref\n0\n";
        assertThatThrownBy(() -> validator.validate(doc.getBytes(StandardCharsets.US_ASCII), "application/pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid PDF, DOC or DOCX");
    }

    @Test
    @DisplayName("PDF with %%EOF too far from the end is rejected")
    void pdfWithEofTooFarFromEndRejected() {
        // %%EOF right after the header, then >1KiB of filler — truncated body.
        String filler = "x".repeat(2048);
        String doc = "%PDF-1.4\n%%EOF\n" + filler;
        assertThatThrownBy(() -> validator.validate(doc.getBytes(StandardCharsets.US_ASCII), "application/pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid PDF, DOC or DOCX");
    }

    @Test
    @DisplayName("%%EOF only (renamed non-PDF fragment) is rejected")
    void eofOnlyFragmentRejected() {
        assertThatThrownBy(() -> validator.validate("%%EOF".getBytes(StandardCharsets.US_ASCII), "application/pdf"))
                .isInstanceOf(BadRequestException.class);
    }

    // ------------------------------------------------------------------
    // Content detection — hostile OOXML variants
    // ------------------------------------------------------------------

    @Test
    @DisplayName("XLSX disguised as DOCX is rejected by content-type declaration")
    void xlsxDisguisedAsDocxRejected() throws IOException {
        byte[] xlsx = docx(XLSX_CT);
        assertThatThrownBy(() -> validator.validate(xlsx, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a WordprocessingML");
    }

    @Test
    @DisplayName("PPTX disguised as DOCX is rejected by content-type declaration")
    void pptxDisguisedAsDocxRejected() throws IOException {
        byte[] pptx = docx(PPTX_CT);
        assertThatThrownBy(() -> validator.validate(pptx, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a WordprocessingML");
    }

    @Test
    @DisplayName("arbitrary ZIP (no content-types entry) is rejected")
    void arbitraryZipRejected() throws IOException {
        byte[] zip = zip("readme.txt", "just an archive");
        assertThatThrownBy(() -> validator.validate(zip, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("missing [Content_Types].xml");
    }

    @Test
    @DisplayName("ZIP with WordprocessingML declaration but no word/document.xml is rejected")
    void docxWithoutWordDocumentRejected() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(WORD_CT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("other/file.txt"));
            zip.write("x".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertThatThrownBy(() -> validator.validate(bytes.toByteArray(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("missing word/document.xml");
    }

    @Test
    @DisplayName("unknown content is rejected, never guessed")
    void unknownContentRejected() {
        assertThatThrownBy(() -> validator.validate("just some text".getBytes(StandardCharsets.UTF_8), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid PDF, DOC or DOCX");
    }

    @Test
    @DisplayName("empty content is rejected")
    void emptyContentRejected() {
        assertThatThrownBy(() -> validator.validate(new byte[0], null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("corrupted ZIP (truncated after signature) is rejected")
    void corruptedZipRejected() {
        assertThatThrownBy(() -> validator.validate(new byte[]{'P', 'K', 0x03, 0x04, 0x00, 0x01}, null))
                .isInstanceOf(BadRequestException.class);
    }

    // ------------------------------------------------------------------
    // MIME cross-check
    // ------------------------------------------------------------------

    @Test
    @DisplayName("matching declared MIME is accepted")
    void matchingMimeAccepted() {
        assertThat(validator.validate(pdf("body"), "application/pdf"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.PDF);
        assertThat(validator.validate(ole2(), "application/msword"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.DOC);
    }

    @Test
    @DisplayName("contradictory declared MIME is rejected (never overrides content)")
    void contradictoryMimeRejected() throws IOException {
        // PDF bytes, declared as a spreadsheet image type.
        assertThatThrownBy(() -> validator.validate(pdf("body"), "image/jpeg"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
        // DOCX bytes, declared as PDF.
        assertThatThrownBy(() -> validator.validate(docx(WORD_CT), "application/pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("application/octet-stream is accepted for every detected format")
    void neutralOctetStreamAccepted() throws IOException {
        assertThat(validator.validate(pdf("body"), "application/octet-stream"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.PDF);
        assertThat(validator.validate(ole2(), "application/octet-stream"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.DOC);
        assertThat(validator.validate(docx(WORD_CT), "application/octet-stream"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.DOCX);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("missing or blank declared MIME is treated as neutral")
    void blankMimeTreatedAsNeutral(String declared) {
        assertThat(validator.validate(pdf("body"), declared))
                .isEqualTo(ResumeContentValidator.ResumeFormat.PDF);
    }

    // ------------------------------------------------------------------
    // Size bounds
    // ------------------------------------------------------------------

    @Test
    @DisplayName("exactly 5 MiB valid PDF is accepted")
    void exactlyFiveMiBAccepted() {
        int target = ResumeContentValidator.MAX_FILE_SIZE_BYTES;
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        byte[] trailer = "\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        byte[] pdf = new byte[target];
        System.arraycopy(header, 0, pdf, 0, header.length);
        System.arraycopy(trailer, 0, pdf, target - trailer.length, trailer.length);
        // Fill the middle with filler bytes — still a structurally plausible PDF.
        for (int i = header.length; i < target - trailer.length; i++) {
            pdf[i] = 'x';
        }
        assertThat(pdf.length).isEqualTo(target);
        assertThat(validator.validate(pdf, "application/pdf"))
                .isEqualTo(ResumeContentValidator.ResumeFormat.PDF);
    }

    @Test
    @DisplayName("oversized upload produces the controlled PayloadTooLargeException")
    void oversizedRejected() {
        byte[] big = new byte[ResumeContentValidator.MAX_FILE_SIZE_BYTES + 1];
        big[0] = '%';
        big[1] = 'P';
        assertThatThrownBy(() -> validator.validate(big, "application/pdf"))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessageContaining("5 MiB");
    }
}
