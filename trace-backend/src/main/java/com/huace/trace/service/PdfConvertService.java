package com.huace.trace.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PDF转图片服务 - 将PDF每页渲染为JPEG图片，用于手机端滑动查看。
 * 转换结果按PDF内容哈希缓存，重复扫码直接返回已有图片。
 */
@Slf4j
@Service
public class PdfConvertService {

    private static final int RENDER_DPI = 130;
    private static final float JPEG_QUALITY = 0.82f;
    private static final Pattern PAGE_FILE = Pattern.compile("^([0-9a-f]+)_p(\\d+)\\.jpg$");

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.url-prefix}")
    private String urlPrefix;

    /**
     * 将PDF文件转换为图片URL列表
     * @param pdfUrl PDF文件的访问URL（可以是http URL或本地文件路径）
     * @return 图片URL列表（每页一张图）
     */
    public List<String> convertPdfToImages(String pdfUrl) {
        List<String> imageUrls = new ArrayList<>();
        if (pdfUrl == null || pdfUrl.isEmpty()) return imageUrls;

        try {
            byte[] pdfBytes = loadPdfBytes(pdfUrl);
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("PDF文件为空或无法读取: {}", pdfUrl);
                return imageUrls;
            }

            String key = sha256Hex(pdfBytes).substring(0, 16);
            Path cacheDir = Path.of(uploadDir, "pdf-images", key);
            Files.createDirectories(cacheDir);

            List<String> cached = readCachedPages(cacheDir, key);
            if (!cached.isEmpty()) {
                log.info("PDF转图片命中缓存: {} 页, 源: {}", cached.size(), pdfUrl);
                return cached;
            }

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer renderer = new PDFRenderer(document);
                int totalPages = document.getNumberOfPages();
                for (int i = 0; i < totalPages; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
                    String fileName = key + "_p" + (i + 1) + ".jpg";
                    writeJpeg(image, cacheDir.resolve(fileName).toFile());
                    imageUrls.add(urlPrefix + "/pdf-images/" + key + "/" + fileName);
                }
                Files.writeString(cacheDir.resolve("done"), String.valueOf(totalPages));
                log.info("PDF转图片完成: {} 页, 源: {}", totalPages, pdfUrl);
            }
        } catch (Exception e) {
            log.error("PDF转图片失败: {}", pdfUrl, e);
        }
        return imageUrls;
    }

    /** 缓存目录中已存在完整转换结果时，按页序返回URL */
    private List<String> readCachedPages(Path cacheDir, String key) {
        Path done = cacheDir.resolve("done");
        if (!Files.exists(done)) return List.of();
        try (Stream<Path> stream = Files.list(cacheDir)) {
            return stream.map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith(key + "_p") && name.endsWith(".jpg"))
                    .sorted(Comparator.comparingInt(name -> {
                        Matcher m = PAGE_FILE.matcher(name);
                        return m.matches() ? Integer.parseInt(m.group(2)) : Integer.MAX_VALUE;
                    }))
                    .map(name -> urlPrefix + "/pdf-images/" + key + "/" + name)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void writeJpeg(BufferedImage image, File out) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest(bytes)) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private byte[] loadPdfBytes(String pdfUrl) throws IOException {
        if (pdfUrl.startsWith("http://") || pdfUrl.startsWith("https://")) {
            // 远程URL - 下载
            try (InputStream is = URI.create(pdfUrl).toURL().openStream()) {
                return is.readAllBytes();
            }
        } else if (pdfUrl.startsWith("/uploads/")) {
            // 本地上传文件路径
            String localPath = pdfUrl.replace("/uploads/", uploadDir + "/");
            return Files.readAllBytes(Path.of(localPath));
        } else {
            // 尝试作为本地路径
            File f = new File(pdfUrl);
            if (f.exists()) return Files.readAllBytes(f.toPath());
            // 尝试加上uploadDir前缀
            File f2 = new File(uploadDir + "/" + pdfUrl);
            if (f2.exists()) return Files.readAllBytes(f2.toPath());
            return null;
        }
    }
}
