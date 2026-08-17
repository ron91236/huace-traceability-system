package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Poster;
import com.huace.trace.mapper.PosterMapper;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosterService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private final PosterMapper posterMapper;

    public PageResult<Poster> list(int page, int size, String keyword) {
        LambdaQueryWrapper<Poster> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            w.like(Poster::getTitle, keyword);
        }
        w.orderByDesc(Poster::getId);
        Page<Poster> r = posterMapper.selectPage(new Page<>(page, size), w);
        r.getRecords().forEach(this::fillUrl);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public Poster create(MultipartFile file, String title) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("请上传HTML文件");
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".html")) {
            throw new BusinessException("仅支持上传HTML文件");
        }
        // 生成唯一slug
        String slug = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        // 存储目录：posters/
        Path posterDir = Paths.get(uploadDir, "posters");
        Files.createDirectories(posterDir);
        // 保存文件：slug.html
        String savedFileName = slug + ".html";
        Path targetPath = posterDir.resolve(savedFileName);
        file.transferTo(targetPath.toFile());

        Poster poster = new Poster();
        poster.setTitle(title != null && !title.isEmpty() ? title : originalName);
        poster.setSlug(slug);
        poster.setFileName(originalName);
        poster.setFilePath("posters/" + savedFileName);
        poster.setStatus(1);
        posterMapper.insert(poster);
        fillUrl(poster);
        return poster;
    }

    public void update(Long id, String title, Integer status) {
        Poster poster = posterMapper.selectById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        if (title != null && !title.isEmpty()) poster.setTitle(title);
        if (status != null) poster.setStatus(status);
        posterMapper.updateById(poster);
    }

    public void delete(Long id) {
        Poster poster = posterMapper.selectById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        // 删除文件
        try {
            Path filePath = Paths.get(uploadDir, poster.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}
        posterMapper.deleteById(id);
    }

    public String getQrCode(Long id) {
        Poster poster = posterMapper.selectById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        String url = baseUrl + "/poster/" + poster.getSlug();
        return QrCodeUtil.generateBase64(url, 400, 400);
    }

    public String getPosterContent(Long slug) {
        // 此方法由Controller直接读取文件返回
        return null;
    }

    /** 读取海报HTML内容（后台编辑用） */
    public String getHtml(Long id) {
        Poster poster = posterMapper.selectById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        try {
            return Files.readString(Paths.get(uploadDir, poster.getFilePath()));
        } catch (Exception e) {
            throw new BusinessException("读取海报内容失败");
        }
    }

    /** 覆写海报HTML内容：slug与文件路径不变，访问链接与二维码保持不变 */
    public void updateHtml(Long id, String content) {
        Poster poster = posterMapper.selectById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        if (content == null || content.isBlank()) throw new BusinessException("HTML内容不能为空");
        try {
            Path filePath = Paths.get(uploadDir, poster.getFilePath());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
        } catch (IOException e) {
            throw new BusinessException("保存海报内容失败");
        }
        poster.setUpdatedAt(java.time.LocalDateTime.now());
        posterMapper.updateById(poster);
    }

    public Poster getById(Long id) {
        Poster poster = posterMapper.selectById(id);
        if (poster != null) fillUrl(poster);
        return poster;
    }

    public Poster getBySlug(String slug) {
        return posterMapper.selectOne(
                new LambdaQueryWrapper<Poster>()
                        .eq(Poster::getSlug, slug)
                        .eq(Poster::getStatus, 1));
    }

    public String readPosterHtml(String slug) {
        Poster poster = getBySlug(slug);
        if (poster == null) return null;
        try {
            Path filePath = Paths.get(uploadDir, poster.getFilePath());
            return Files.readString(filePath);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillUrl(Poster poster) {
        if (poster != null && poster.getSlug() != null) {
            poster.setPosterUrl(baseUrl + "/poster/" + poster.getSlug());
        }
    }
}
