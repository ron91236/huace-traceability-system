package com.huace.trace.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.huace.trace.entity.SysFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Component
public class FileUploadUtil {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.url-prefix}")
    private String urlPrefix;

    public SysFile upload(MultipartFile file, Long uploaderId) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName);
        String storedName = IdUtil.fastSimpleUUID() + "." + ext;

        // 按日期分子目录
        String datePath = cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyy/MM/dd");
        String dirPath = uploadDir + "/" + datePath;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = datePath + "/" + storedName;
        File dest = new File(dirPath + "/" + storedName);
        file.transferTo(dest);

        // 判断文件类型
        String fileType = "other";
        if (ext != null) {
            String lowerExt = ext.toLowerCase();
            if (lowerExt.matches("jpg|jpeg|png|gif|bmp|webp")) {
                fileType = "image";
            } else if (lowerExt.matches("mp4|avi|mov|wmv|flv")) {
                fileType = "video";
            } else if (lowerExt.matches("pdf|doc|docx|xls|xlsx|ppt|pptx")) {
                fileType = "document";
            }
        }

        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(originalName);
        sysFile.setStoredName(storedName);
        sysFile.setFilePath(filePath);
        sysFile.setFileSize(file.getSize());
        sysFile.setFileType(fileType);
        sysFile.setUploaderId(uploaderId);
        return sysFile;
    }

    public String getAccessUrl(String filePath) {
        if (filePath == null) return null;
        return urlPrefix + "/" + filePath;
    }
}
