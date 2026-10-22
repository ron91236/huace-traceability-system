package com.huace.trace.service;

import com.huace.trace.common.BusinessException;
import lombok.Setter;
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
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

    /** 上传目录内的相对 URL 前缀，也是唯一允许读取的入口 */
    private static final String UPLOAD_PREFIX = "/uploads/";

    @Setter
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Setter
    @Value("${file.url-prefix}")
    private String urlPrefix;

    /**
     * 将上传目录内的 PDF 转换为图片URL列表
     * @param pdfUrl PDF 文件的 /uploads/ 相对访问URL
     * @return 图片URL列表（每页一张图）；读不到或解析不了时为空，由调用方决定如何兜底
     */
    public List<String> convertPdfToImages(String pdfUrl) {
        return convertPdfToImages(pdfUrl, 0);
    }

    /**
     * 将上传目录内的 PDF 转换为图片URL列表
     * @param pdfUrl PDF 文件的 /uploads/ 相对访问URL
     * @param maxPages 页数上限，0 表示不限制；超限抛业务异常而不是静默返回空
     */
    public List<String> convertPdfToImages(String pdfUrl, int maxPages) {
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

            // 缓存里的 jpg 清单就是页数：readCachedPages 只在 done 标记存在时才返回，结果不会残缺。
            // 页数判定因此可以放在 loadPDF 之前——免登录的扫码路径重复访问同一份报告时，
            // 不该为了"有几页"把整个文档再解析一遍。
            List<String> cached = readCachedPages(cacheDir, key);
            if (!cached.isEmpty()) {
                if (maxPages > 0 && cached.size() > maxPages) {
                    throw new BusinessException("PDF 共 " + cached.size() + " 页，超过 " + maxPages + " 页上限，请拆分后上传");
                }
                log.info("PDF转图片命中缓存: {} 页, 源: {}", cached.size(), pdfUrl);
                return cached;
            }

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                int totalPages = document.getNumberOfPages();
                if (maxPages > 0 && totalPages > maxPages) {
                    throw new BusinessException("PDF 共 " + totalPages + " 页，超过 " + maxPages + " 页上限，请拆分后上传");
                }
                // 目录留到页数放行之后再建，被拒绝的 PDF 不该在缓存目录里留下空壳
                Files.createDirectories(cacheDir);

                PDFRenderer renderer = new PDFRenderer(document);
                for (int i = 0; i < totalPages; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
                    String fileName = key + "_p" + (i + 1) + ".jpg";
                    writeJpeg(image, cacheDir.resolve(fileName).toFile());
                    imageUrls.add(urlPrefix + "/pdf-images/" + key + "/" + fileName);
                }
                Files.writeString(cacheDir.resolve("done"), String.valueOf(totalPages));
                log.info("PDF转图片完成: {} 页, 源: {}", totalPages, pdfUrl);
            }
        } catch (BusinessException e) {
            throw e;
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

    /**
     * 只允许读取本系统上传目录内的文件。这个方法挂在免登录的 /api/trace/pdf-images 上，
     * 保留 http 抓取或原始磁盘路径，等于把内网地址和整台机器的文件交给外部输入。
     */
    private byte[] loadPdfBytes(String pdfUrl) throws IOException {
        Path path = confineToUploads(pdfUrl);
        if (path == null) return null;
        return Files.readAllBytes(path);
    }

    /** /uploads/ 相对 URL 还原成真实磁盘路径，越出上传目录、不存在或不是普通文件时返回 null */
    private Path confineToUploads(String url) throws IOException {
        if (url == null || !url.startsWith(UPLOAD_PREFIX)) return null;
        Path root = Path.of(uploadDir).toAbsolutePath().normalize().toRealPath();
        Path lexical = root.resolve(url.substring(UPLOAD_PREFIX.length())).normalize();
        if (!lexical.startsWith(root)) return null;
        try {
            Path real = lexical.toRealPath();
            return real.startsWith(root) && Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS) ? real : null;
        } catch (IOException e) {
            // 文件不存在是常态（报告被删），按既有的"空结果"契约处理，不抛给调用方
            return null;
        }
    }
}
