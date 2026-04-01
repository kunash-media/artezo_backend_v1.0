package com.artezo.service;

import com.artezo.service.compression.ImageCompressor;
import com.artezo.service.compression.VideoCompressor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Service
public class MediaCompressorService {

    @Value("${file.upload-dir:uploads}")  // 🔴 DEFAULT VALUE
    private String uploadDir;

    private final ImageCompressor imageCompressor;
    private final VideoCompressor videoCompressor;
    private String absoluteUploadPath;

    public MediaCompressorService() {
        this.imageCompressor = new ImageCompressor();
        this.videoCompressor = new VideoCompressor();
        System.out.println("✅ MediaCompressorService initialized");

        // 🔴 DEBUG: Ye print karo
        System.out.println("🔍 Constructor - uploadDir: '" + uploadDir + "'");
    }

    @PostConstruct
    public void init() {
        // 🔴 DEBUG: Properties value check
        System.out.println("🔍 @PostConstruct - uploadDir raw value: '" + uploadDir + "'");

        // 🔴 Current directory
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        System.out.println("📁 Current working directory: " + currentPath);
        System.out.println("📁 Upload directory from properties: '" + uploadDir + "'");

        // 🔴 AGAR NULL hai to default use karo
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            System.err.println("⚠️ WARNING: uploadDir is null or empty! Using default 'uploads'");
            uploadDir = "uploads";
        }

        absoluteUploadPath = currentPath.resolve(uploadDir).toString();
        System.out.println("📁 Absolute upload path: " + absoluteUploadPath);

        // Create directories
        File imagesDir = new File(absoluteUploadPath, "images");
        File videosDir = new File(absoluteUploadPath, "videos");

        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
            System.out.println("✅ Created images directory: " + imagesDir.getAbsolutePath());
        } else {
            System.out.println("✅ Images directory exists: " + imagesDir.getAbsolutePath());
        }

        if (!videosDir.exists()) {
            videosDir.mkdirs();
            System.out.println("✅ Created videos directory: " + videosDir.getAbsolutePath());
        } else {
            System.out.println("✅ Videos directory exists: " + videosDir.getAbsolutePath());
        }

        System.out.println("⚡ Async mode enabled - Faster processing");
    }

    @Async
    public CompletableFuture<File> compressImageAsync(MultipartFile file, Long userId) {
        long startTime = System.currentTimeMillis();

        System.out.println("\n📸 Processing image for user: " + userId);
        System.out.println("📸 File: " + file.getOriginalFilename() + " | Size: " + (file.getSize() / 1024) + "KB");

        try {
            File tempFile = File.createTempFile("img_", ".tmp");
            file.transferTo(tempFile);

            String imageDir = absoluteUploadPath + File.separator + "images" + File.separator;
            File compressedFile = imageCompressor.compress(tempFile, imageDir);

            tempFile.delete();

            long endTime = System.currentTimeMillis();
            System.out.println("✅ Image compressed in " + (endTime - startTime) + "ms: " +
                    (compressedFile.length() / 1024) + "KB");
            System.out.println("📁 Saved to: " + compressedFile.getAbsolutePath());

            return CompletableFuture.completedFuture(compressedFile);

        } catch (Exception e) {
            System.err.println("❌ Image compression failed: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<File> compressVideoAsync(MultipartFile file, Long userId) {
        long startTime = System.currentTimeMillis();

        System.out.println("\n🎬 Processing video for user: " + userId);
        System.out.println("🎬 File: " + file.getOriginalFilename() + " | Size: " +
                (file.getSize() / (1024 * 1024)) + "MB");

        try {
            File tempFile = File.createTempFile("vid_", ".tmp");
            file.transferTo(tempFile);

            String videoDir = absoluteUploadPath + File.separator + "videos" + File.separator;
            File compressedFile = videoCompressor.compress(tempFile, videoDir);

            tempFile.delete();

            long endTime = System.currentTimeMillis();
            System.out.println("✅ Video compressed in " + (endTime - startTime) + "ms: " +
                    (compressedFile.length() / (1024 * 1024)) + "MB");
            System.out.println("📁 Saved to: " + compressedFile.getAbsolutePath());

            return CompletableFuture.completedFuture(compressedFile);

        } catch (Exception e) {
            System.err.println("❌ Video compression failed: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}