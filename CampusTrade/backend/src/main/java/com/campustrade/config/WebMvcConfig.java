package com.campustrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC配置类
 * 配置静态资源映射和文件上传路径
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:${user.home}/campus-trade/uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的访问路径（兼容 Windows 和 Linux）
        String location = new File(uploadPath).toURI().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
