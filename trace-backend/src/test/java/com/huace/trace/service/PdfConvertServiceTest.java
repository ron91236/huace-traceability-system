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
 * 2. 免登录的门按兜底上限限量，超限返回空让前端回落，而不是抛给扫码的用户
 * 3. 命中缓存同样受上限约束，且返回的确实是缓存里的页序
 * 4. 只读上传目录：外部 URL、越界路径、目录、符号链接、非 PDF 文件一律空结果
 */
class PdfConvertServiceTest {

    /** PdfConvertService 免登录路径的兜底页数上限；与生产代码同为私有常量，改动要同步 */
    private static final int HARD_PAGES = 100;

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
        byte[] bytes = pdfBytes(2);
        String url = writeUploadPdf("2026/limit.pdf", bytes);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdfToImages(url, 1));

        assertTrue(e.getMessage().contains("2 页"), e.getMessage());
        assertTrue(e.getMessage().contains("1 页上限"), e.getMessage());
        // 被拒绝的 PDF 不该在缓存目录里留下空壳，否则下次扫码还会撞上这个半成品目录
        assertFalse(Files.exists(tmp.resolve("pdf-images").resolve(contentKey(bytes))),
                "拒绝转换后不应留下缓存目录");
    }

    @Test
    void 上限为0时按兜底上限放行() throws Exception {
        byte[] bytes = pdfBytes(2);
        String url = writeUploadPdf("2026/nolimit.pdf", bytes);

        List<String> pages = service.convertPdfToImages(url, 0);

        String key = contentKey(bytes);
        assertEquals(List.of(
                "/uploads/pdf-images/" + key + "/" + key + "_p1.jpg",
                "/uploads/pdf-images/" + key + "/" + key + "_p2.jpg"), pages);
    }

    @Test
    void 免登录的门超过兜底上限时返回空且不落盘() throws Exception {
        // 101 页只为越过硬上限而造，永远不该被渲染：一旦上限失效这个用例就是红的
        byte[] bytes = pdfBytes(HARD_PAGES + 1);
        String url = writeUploadPdf("2026/toolong.pdf", bytes);

        assertEquals(List.of(), service.convertPdfToImages(url));

        assertFalse(Files.exists(tmp.resolve("pdf-images").resolve(contentKey(bytes))),
                "超过兜底上限的 PDF 不该建立缓存目录");
    }

    @Test
    void 命中缓存也要拒绝超限() throws Exception {
        // 真实序列：免登录的门先把 3 页渲染成缓存，企业端随后带着更小的上限来要同一份报告。
        // 缓存清单与 done 记录一致，页数判定才允许放在 loadPDF 之前。
        byte[] bytes = pdfBytes(3);
        String url = writeUploadPdf("2026/cached.pdf", bytes);
        String key = contentKey(bytes);
        assertEquals(3, service.convertPdfToImages(url).size());

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
    void 缓存清单与done记录不一致时按真实页数重转() throws Exception {
        // 缓存里 1 张图、done 写着 5 页：清单不再等于页数，就必须整份作废重转，
        // 既不能凭 1 页放行，也不能把 p1 之外的 URL 发给前端换成 404。
        byte[] bytes = pdfBytes(2);
        String url = writeUploadPdf("2026/rotten.pdf", bytes);
        String key = contentKey(bytes);
        Path cacheDir = tmp.resolve("pdf-images").resolve(key);
        Files.createDirectories(cacheDir);
        Files.write(cacheDir.resolve(key + "_p1.jpg"), new byte[]{1});
        Files.writeString(cacheDir.resolve("done"), "5");

        List<String> pages = service.convertPdfToImages(url, 5);

        assertEquals(List.of(
                "/uploads/pdf-images/" + key + "/" + key + "_p1.jpg",
                "/uploads/pdf-images/" + key + "/" + key + "_p2.jpg"), pages);
        assertEquals("2", Files.readString(cacheDir.resolve("done")));
    }

    @Test
    void 外部URL与越界路径不被读取() {
        assertEquals(List.of(), service.convertPdfToImages("http://127.0.0.1:9/x.pdf"));
        assertEquals(List.of(), service.convertPdfToImages("/etc/hosts"));
        assertEquals(List.of(), service.convertPdfToImages("/uploads/../../etc/passwd"));
    }

    @Test
    void 上传目录里的符号链接不被读取() throws Exception {
        // toRealPath 之后仍在上传目录内才放行：指向 /etc/hosts 的链接就是 /uploads/ 前缀
        // 唯一能读走目录外文件的方式。
        Files.createDirectories(tmp.resolve("2026"));
        Files.createSymbolicLink(tmp.resolve("2026/evil.pdf"), Path.of("/etc/hosts"));

        assertEquals(List.of(), service.convertPdfToImages("/uploads/2026/evil.pdf"));
    }

    @Test
    void 上传目录里的非PDF文件不被解析() throws Exception {
        // 改名为 .pdf 的视频/图片不该被整份读进堆再交给 PDFBox 放弃，
        // 更不该在缓存目录里留下一个永远填不满的哈希壳。
        Files.createDirectories(tmp.resolve("2026"));
        byte[] junk = "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(tmp.resolve("2026/clip.pdf"), junk);

        assertEquals(List.of(), service.convertPdfToImages("/uploads/2026/clip.pdf"));

        assertFalse(Files.exists(tmp.resolve("pdf-images").resolve(contentKey(junk))),
                "非 PDF 文件不该建立缓存目录");
    }

    /** 写一份指定内容的 PDF 到上传目录，返回它的 /uploads/ 相对 URL */
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
