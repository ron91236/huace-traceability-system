package com.huace.trace.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PDF转图片服务 - 将PDF每页渲染为PNG图片，用于手机端滑动查看
 */
@Slf4j
@Service
public class PdfConvertService {

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
            // 获取PDF文件（本地文件或远程URL）
            byte[] pdfBytes = loadPdfBytes(pdfUrl);
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("PDF文件为空或无法读取: {}", pdfUrl);
                return imageUrls;
            }

            // 创建输出目录
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String outDir = uploadDir + "/" + datePath + "/pdf-images";
            Files.createDirectories(Path.of(outDir));

            // 使用PDFBox渲染每页为图片
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer renderer = new PDFRenderer(document);
                String batchId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                int totalPages = document.getNumberOfPages();

                for (int i = 0; i < totalPages; i++) {
                    // 渲染为150 DPI的PNG（手机端足够清晰，文件大小适中）
                    BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB);
                    String fileName = batchId + "_p" + (i + 1) + ".png";
                    File outputFile = new File(outDir + "/" + fileName);
                    javax.imageio.ImageIO.write(image, "PNG", outputFile);

                    String imageUrl = urlPrefix + "/" + datePath + "/pdf-images/" + fileName;
                    imageUrls.add(imageUrl);
                }
                log.info("PDF转图片完成: {} 页, 源: {}", totalPages, pdfUrl);
            }
        } catch (Exception e) {
            log.error("PDF转图片失败: {}", pdfUrl, e);
        }
        return imageUrls;
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
