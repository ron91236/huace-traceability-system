package com.huace.trace.service;

import com.huace.trace.common.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDF 转图片服务回归测试：用 PDFBox 现造 PDF，测真实转换而不是 mock 协作方。
 * 1. 页数上限在解析后立刻拒绝，异常要能穿出兜底 catch
 * 2. 上限为 0 表示不限制
 * 3. 命中缓存同样受上限约束，且返回的确实是缓存里的页序
 * 4. 只读上传目录：外部 URL、越界路径、原始磁盘路径一律空结果
 */
class PdfConvertServiceTest {

    @TempDir Path tmp;

    private PdfConvertService service;

    @BeforeEach
    void setUp() {
        service = new PdfConvertService();
        service.setUploadDir(tmp.toString());
        service.setUrlPrefix("/uploads");
    }

    @Test
    void 超过页数上限的PDF被拒绝() throws Exception {
        String url = writeUploadPdf("2026/limit.pdf", 2);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdfToImages(url, 1));

        assertTrue(e.getMessage().contains("2 页"), e.getMessage());
        assertTrue(e.getMessage().contains("1 页上限"), e.getMessage());
    }

    @Test
    void 上限为0不限制() throws Exception {
        byte[] bytes = pdfBytes(2);
        String url = writeUploadPdf("2026/nolimit.pdf", bytes);

        List<String> pages = service.convertPdfToImages(url, 0);

        String key = contentKey(bytes);
        assertEquals(List.of(
                "/uploads/pdf-images/" + key + "/" + key + "_p1.jpg",
                "/uploads/pdf-images/" + key + "/" + key + "_p2.jpg"), pages);
    }

    @Test
    void 命中缓存也要拒绝超限() throws Exception {
        // 缓存里 3 页、源文件只有 2 页：两处断言都不可能被"重新渲染一遍"满足，
        // 于是它们测的就是缓存短路本身，而不是解析出的页数。
        byte[] bytes = pdfBytes(2);
        String url = writeUploadPdf("2026/cached.pdf", bytes);
        String key = contentKey(bytes);
        Path cacheDir = tmp.resolve("pdf-images").resolve(key);
        Files.createDirectories(cacheDir);
        for (int i = 1; i <= 3; i++) {
            Files.write(cacheDir.resolve(key + "_p" + i + ".jpg"), new byte[]{1});
        }
        Files.writeString(cacheDir.resolve("done"), "3");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdfToImages(url, 2));
        assertTrue(e.getMessage().contains("3 页"), e.getMessage());
        assertTrue(e.getMessage().contains("2 页上限"), e.getMessage());

        assertEquals(List.of(
                "/uploads/pdf-images/" + key + "/" + key + "_p1.jpg",
                "/uploads/pdf-images/" + key + "/" + key + "_p2.jpg",
                "/uploads/pdf-images/" + key + "/" + key + "_p3.jpg"),
                service.convertPdfToImages(url, 5));
    }

    @Test
    void 外部URL与越界路径不被读取() {
        assertEquals(List.of(), service.convertPdfToImages("http://127.0.0.1:9/x.pdf"));
        assertEquals(List.of(), service.convertPdfToImages("/etc/hosts"));
        assertEquals(List.of(), service.convertPdfToImages("/uploads/../../etc/passwd"));
    }

    /** 写一份指定页数的 PDF 到上传目录，返回它的 /uploads/ 相对 URL */
    private String writeUploadPdf(String relative, int pages) throws Exception {
        return writeUploadPdf(relative, pdfBytes(pages));
    }

    private String writeUploadPdf(String relative, byte[] bytes) throws IOException {
        Path file = tmp.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        return "/uploads/" + relative;
    }

    /** 现造一个 N 页空白 PDF；与生产代码同为 PDFBox 内容哈希寻址，键值必须一致 */
    private static byte[] pdfBytes(int pages) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** PdfConvertService 的缓存键：文件字节 SHA-256 前 16 位十六进制 */
    private static String contentKey(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest(bytes)) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.substring(0, 16);
    }
}
