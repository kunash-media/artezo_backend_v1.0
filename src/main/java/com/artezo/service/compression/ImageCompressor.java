package com.artezo.service.compression;

import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class ImageCompressor {

    private static final long MAX_IMAGE_SIZE = 1 * 1024 * 1024; // 1MB
    private static final long SKIP_THRESHOLD = 500 * 1024; // 500KB - skip compression

    public File compress(File inputFile, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID().toString() + ".jpg";
        File outputFile = new File(outputDir + fileName);

        // 🔴 SKIP COMPRESSION IF FILE IS ALREADY SMALL
        if (inputFile.length() <= SKIP_THRESHOLD) {
            System.out.println("📸 File already small (" + (inputFile.length() / 1024) +
                    "KB), skipping compression");
            Files.copy(inputFile.toPath(), outputFile.toPath());
            return outputFile;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("📸 Compressing image: " + inputFile.getName() +
                " | Size: " + (inputFile.length() / 1024) + "KB");

        // 🔴 FASTER COMPRESSION SETTINGS
        if (inputFile.length() > MAX_IMAGE_SIZE) {
            // Large file - aggressive compression
            Thumbnails.of(inputFile)
                    .size(800, 800)           // Smaller size
                    .outputQuality(0.6)        // Lower quality (faster)
                    .toFile(outputFile);
        } else {
            // Medium file - moderate compression
            Thumbnails.of(inputFile)
                    .size(1000, 1000)          // Medium size
                    .outputQuality(0.7)         // Medium quality
                    .toFile(outputFile);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ Compressed to: " + (outputFile.length() / 1024) +
                "KB in " + (endTime - startTime) + "ms");

        return outputFile;
    }
}