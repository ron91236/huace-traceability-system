package com.huace.trace.controller;

import com.huace.trace.service.PosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PosterPageController {

    private final PosterService posterService;

    /**
     * 公开访问海报HTML页面
     * 用户扫描二维码后访问此端点
     */
    @GetMapping(value = "/poster/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewPoster(@PathVariable String slug) {
        String html = posterService.readPosterHtml(slug);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
