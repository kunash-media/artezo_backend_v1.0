package com.artezo.service.compression;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VideoCompressor {

    private static final long MAX_VIDEO_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long SKIP_THRESHOLD = 1 * 1024 * 1024; // 1MB - skip compression
    private static final String FFMPEG_PATH = "C:\\ffmpeg-master-latest-win64-gpl-shared\\bin\\ffmpeg.exe";

    // 🔴 TIMEOUT SETTINGS
    private static final int COMPRESSION_TIMEOUT_SECONDS = 60;
    private static final int PROCESS_WAIT_TIMEOUT_SECONDS = 60;

    public File compress(File inputFile, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID().toString() + ".mp4";
        File outputFile = new File(outputDir + fileName);

        // 🔴 SKIP COMPRESSION IF FILE IS ALREADY SMALL
        if (inputFile.length() <= SKIP_THRESHOLD) {
            System.out.println("🎬 File already small (" + (inputFile.length() / (1024 * 1024)) +
                    "MB), skipping compression");
            Files.copy(inputFile.toPath(), outputFile.toPath());
            return outputFile;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("🎬 Compressing video: " + inputFile.getName() +
                " | Size: " + (inputFile.length() / (1024 * 1024)) + "MB");
        System.out.println("🎬 Timeout set to: " + COMPRESSION_TIMEOUT_SECONDS + " seconds");

        // 🔴 FAST COMPRESSION SETTINGS
        String outputPath = outputDir + fileName;

        // Temporary file for fallback
        File tempFallback = new File(outputDir + "temp_" + fileName);

        try {
            // 🔴 USE FULL FFMPEG PATH
            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG_PATH,
                    "-i", inputFile.getAbsolutePath(),
                    "-vf", "scale=480:270",        // Even smaller resolution (faster)
                    "-b:v", "200k",                  // Lower bitrate (faster)
                    "-preset", "ultrafast",          // Fastest preset
                    "-threads", "2",                  // Use multiple threads
                    "-t", String.valueOf(COMPRESSION_TIMEOUT_SECONDS), // Max processing time
                    "-y",                             // Overwrite
                    outputPath
            );

            // 🔴 REDIRECT ERROR STREAM FOR DEBUGGING
            pb.redirectErrorStream(true);

            System.out.println("🎬 Starting FFmpeg process...");
            Process process = pb.start();

            // 🔴 READ OUTPUT IN SEPARATE THREAD TO AVOID BLOCKING
            StringBuilder ffmpegOutput = new StringBuilder();
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ffmpegOutput.append(line).append("\n");
                        // Print only important messages
                        if (line.contains("error") || line.contains("Error") || line.contains("frame=")) {
                            System.out.println("FFmpeg: " + line);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading FFmpeg output: " + e.getMessage());
                }
            });
            outputReader.start();

            // 🔴 WAIT FOR PROCESS WITH TIMEOUT
            System.out.println("🎬 Waiting for compression to complete (max " + PROCESS_WAIT_TIMEOUT_SECONDS + " seconds)...");
            boolean finished = process.waitFor(PROCESS_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                // 🔴 TIMEOUT OCCURRED
                System.err.println("❌ FFmpeg process timed out after " + PROCESS_WAIT_TIMEOUT_SECONDS + " seconds");
                process.destroyForcibly();

                // Fallback - copy original file
                System.out.println("🎬 Using fallback - copying original file");
                Files.copy(inputFile.toPath(), outputFile.toPath());

                long endTime = System.currentTimeMillis();
                System.out.println("⚠️ Fallback used - original size: " + (inputFile.length() / (1024 * 1024)) + "MB");
                System.out.println("⏱️ Total time: " + (endTime - startTime) + "ms");

                return outputFile;
            }

            // Wait for output reader to finish
            outputReader.join(2000);

            int exitCode = process.exitValue();
            System.out.println("🎬 FFmpeg exit code: " + exitCode);

            if (exitCode == 0) {
                File compressedFile = new File(outputPath);

                // Verify file exists and has content
                if (!compressedFile.exists() || compressedFile.length() == 0) {
                    throw new IOException("Compressed file is empty or doesn't exist");
                }

                long endTime = System.currentTimeMillis();
                double compressedSizeMB = compressedFile.length() / (1024.0 * 1024.0);
                double originalSizeMB = inputFile.length() / (1024.0 * 1024.0);
                double savingsPercent = ((originalSizeMB - compressedSizeMB) / originalSizeMB) * 100;

                System.out.println("✅ Compression successful!");
                System.out.println("   Original: " + String.format("%.2f", originalSizeMB) + "MB");
                System.out.println("   Compressed: " + String.format("%.2f", compressedSizeMB) + "MB");
                System.out.println("   Saved: " + String.format("%.1f", savingsPercent) + "%");
                System.out.println("   Time: " + (endTime - startTime) + "ms");

                // If still >2MB, log warning
                if (compressedFile.length() > MAX_VIDEO_SIZE) {
                    System.out.println("⚠️ Warning: Compressed file still >2MB (" +
                            String.format("%.2f", compressedSizeMB) + "MB)");
                }

                return compressedFile;

            } else {
                System.err.println("❌ FFmpeg failed with exit code: " + exitCode);
                System.err.println("FFmpeg output: " + ffmpegOutput.toString());

                // Fallback - copy original
                System.out.println("🎬 Using fallback - copying original file");
                Files.copy(inputFile.toPath(), outputFile.toPath());

                return outputFile;
            }

        } catch (InterruptedException e) {
            System.err.println("❌ Compression interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();

            // Fallback - copy original
            Files.copy(inputFile.toPath(), outputFile.toPath());
            return outputFile;

        } catch (Exception e) {
            System.err.println("❌ FFmpeg error: " + e.getMessage());
            e.printStackTrace();

            // Fallback - copy original
            System.out.println("🎬 Using fallback - copying original file");
            Files.copy(inputFile.toPath(), outputFile.toPath());
            return outputFile;
        }
    }

    /**
     * Helper method to check if FFmpeg is available
     */
    public boolean isFFmpegAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(FFMPEG_PATH + " -version");
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
