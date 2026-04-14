package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ReviewRequestDto;
import com.artezo.dto.response.ReviewResponseDto;
import com.artezo.entity.ReviewEntity;
import com.artezo.repository.ReviewRepository;
import com.artezo.exceptions.ReviewNotFoundException;
import com.artezo.exceptions.MediaUploadException;
import com.artezo.service.ReviewService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 5 * 1024 * 1024;
    private static final long TARGET_VIDEO_SIZE = 2 * 1024 * 1024;

    // 🔴 UPDATED: Now just "ffmpeg" since it's in system PATH
    private static final String FFMPEG_PATH = "C:\\ffmpeg-2026-04-09-git-d3d0b7a5ee-essentials_build\\bin\\ffmpeg.exe";

    @Override
    public ReviewResponseDto createReview(ReviewRequestDto requestDto) {
        if (reviewRepository.existsByProductIdAndUserId(requestDto.getProductId(), requestDto.getUserId())) {
            throw new RuntimeException("User has already reviewed this product");
        }

        ReviewEntity review = new ReviewEntity();
        review.setProductId(requestDto.getProductId());
        review.setUserId(requestDto.getUserId());
        review.setRating(requestDto.getRating());
        review.setComment(requestDto.getComment());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        // 🔴 NEW: Set default approval values
        review.setApproved(false);      // Not approved by default
        review.setStatus("pending");     // Status is pending
        review.setFlagged(false);        // Not flagged by default
        review.setReplies(null);         // No replies yet

        if (requestDto.getImages() != null && !requestDto.getImages().isEmpty()) {
            handleImageWithCompression(review, requestDto.getImages().get(0));
        }

        if (requestDto.getVideos() != null && !requestDto.getVideos().isEmpty()) {
            handleVideoWithCompression(review, requestDto.getVideos().get(0));
        }

        if (requestDto.getImageBase64() != null && !requestDto.getImageBase64().isEmpty()) {
            handleBase64Image(review, requestDto.getImageBase64(), requestDto.getImageContentType());
        }

        ReviewEntity savedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(savedReview);
    }

    private void handleImageWithCompression(ReviewEntity review, MultipartFile image) {
        try {
            long originalSize = image.getSize();
            System.out.println("📸 Original image size: " + (originalSize / 1024) + "KB (" + String.format("%.2f", originalSize / (1024.0 * 1024.0)) + " MB)");

            if (originalSize > MAX_IMAGE_SIZE) {
                throw new MediaUploadException("Image size exceeds 5MB limit");
            }

            byte[] compressedBytes = compressImageInMemory(image.getBytes());
            long compressedSize = compressedBytes.length;

            double originalMB = originalSize / (1024.0 * 1024.0);
            double compressedMB = compressedSize / (1024.0 * 1024.0);
            double savedPercent = ((originalMB - compressedMB) / originalMB) * 100;

            System.out.println("✅ Image compressed: " + String.format("%.2f", originalMB) + "MB → " +
                    String.format("%.2f", compressedMB) + "MB (Saved " + String.format("%.1f", savedPercent) + "%)");

            review.setImageOriginalSize(originalSize);
            review.setImageCompressedSize(compressedSize);
            review.setImageSize(compressedSize);
            review.setImageData(compressedBytes);
            review.setImageContentType("image/jpeg");
            review.setImageName(image.getOriginalFilename());

        } catch (Exception e) {
            throw new MediaUploadException("Failed to compress image: " + e.getMessage(), e);
        }
    }

    /**
     * 🔴 VIDEO COMPRESSION WITH FFMPEG - NOW WORKING!
     */
    private void handleVideoWithCompression(ReviewEntity review, MultipartFile video) {
        try {
            long originalSize = video.getSize();
            double originalMB = originalSize / (1024.0 * 1024.0);

            System.out.println("🎬 Original video size: " + String.format("%.2f", originalMB) + "MB");

            if (originalSize > MAX_VIDEO_SIZE) {
                throw new MediaUploadException("Video size exceeds 5MB limit. Your video: " + String.format("%.2f", originalMB) + "MB");
            }

            byte[] videoBytes;
            long compressedSize;

            if (originalSize > TARGET_VIDEO_SIZE) {
                System.out.println("🎬 Compressing video (target: <2MB)...");
                videoBytes = compressVideoUsingFFmpeg(video);
                compressedSize = videoBytes.length;

                double compressedMB = compressedSize / (1024.0 * 1024.0);
                double savedPercent = ((originalMB - compressedMB) / originalMB) * 100;

                System.out.println("✅ Video compressed: " + String.format("%.2f", originalMB) + "MB → " +
                        String.format("%.2f", compressedMB) + "MB (Saved " + String.format("%.1f", savedPercent) + "%)");
            } else {
                videoBytes = video.getBytes();
                compressedSize = originalSize;
                System.out.println("🎬 Video already under 2MB, skipping compression");
            }

            review.setVideoOriginalSize(originalSize);
            review.setVideoCompressedSize(compressedSize);
            review.setVideoSize(compressedSize);
            review.setVideoData(videoBytes);
            review.setVideoContentType("video/mp4");
            review.setVideoName(video.getOriginalFilename());

        } catch (Exception e) {
            System.err.println("⚠️ Video compression failed, storing original: " + e.getMessage());
            try {
                review.setVideoOriginalSize(video.getSize());
                review.setVideoCompressedSize(video.getSize());
                review.setVideoSize(video.getSize());
                review.setVideoData(video.getBytes());
                review.setVideoContentType(video.getContentType());
                review.setVideoName(video.getOriginalFilename());
            } catch (IOException ex) {
                throw new MediaUploadException("Failed to process video: " + ex.getMessage(), ex);
            }
        }
    }

    private byte[] compressVideoUsingFFmpeg(MultipartFile video) throws Exception {
        File tempInputFile = File.createTempFile("video_input_", ".mp4");
        File tempOutputFile = File.createTempFile("video_output_", ".mp4");

        try {
            video.transferTo(tempInputFile);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    FFMPEG_PATH,
                    "-i", tempInputFile.getAbsolutePath(),
                    "-vf", "scale=640:360",
                    "-b:v", "800k",
                    "-b:a", "64k",
                    "-r", "24",
                    "-preset", "fast",
                    "-fs", String.valueOf(TARGET_VIDEO_SIZE - 100 * 1024),
                    "-y",
                    tempOutputFile.getAbsolutePath()
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output to avoid blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("error") || line.contains("Error")) {
                        System.err.println("FFmpeg: " + line);
                    }
                }
            }

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                throw new Exception("FFmpeg compression failed with exit code: " + process.exitValue());
            }

            byte[] compressedBytes = Files.readAllBytes(tempOutputFile.toPath());

            // If still > 2MB, try aggressive compression
            if (compressedBytes.length > TARGET_VIDEO_SIZE) {
                System.out.println("⚠️ Still >2MB, trying aggressive compression...");
                return aggressiveCompression(tempInputFile);
            }

            return compressedBytes;

        } finally {
            tempInputFile.delete();
            tempOutputFile.delete();
        }
    }

    private byte[] aggressiveCompression(File inputFile) throws Exception {
        File tempOutputFile = File.createTempFile("video_aggressive_", ".mp4");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    FFMPEG_PATH,
                    "-i", inputFile.getAbsolutePath(),
                    "-vf", "scale=480:270",
                    "-b:v", "400k",
                    "-b:a", "32k",
                    "-r", "20",
                    "-preset", "ultrafast",
                    "-fs", String.valueOf(TARGET_VIDEO_SIZE - 100 * 1024),
                    "-y",
                    tempOutputFile.getAbsolutePath()
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            process.waitFor(60, TimeUnit.SECONDS);

            return Files.readAllBytes(tempOutputFile.toPath());

        } finally {
            tempOutputFile.delete();
        }
    }

    private byte[] compressImageInMemory(byte[] imageBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(inputStream)
                .size(800, 800)
                .outputQuality(0.7)
                .outputFormat("jpg")
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    private void handleBase64Image(ReviewEntity review, String base64Image, String contentType) {
        try {
            String[] parts = base64Image.split(",");
            byte[] imageBytes = Base64.getDecoder().decode(parts.length > 1 ? parts[1] : parts[0]);

            long originalSize = imageBytes.length;

            if (originalSize > MAX_IMAGE_SIZE) {
                throw new MediaUploadException("Base64 image size exceeds 5MB limit");
            }

            byte[] compressedBytes = compressImageInMemory(imageBytes);
            long compressedSize = compressedBytes.length;

            review.setImageOriginalSize(originalSize);
            review.setImageCompressedSize(compressedSize);
            review.setImageSize(compressedSize);
            review.setImageData(compressedBytes);
            review.setImageContentType(contentType != null ? contentType : "image/jpeg");
            review.setImageName("base64_image_" + System.currentTimeMillis() + ".jpg");

            System.out.println("📸 Base64 image: " + String.format("%.2f", originalSize / (1024.0 * 1024.0)) + "MB → " +
                    String.format("%.2f", compressedSize / (1024.0 * 1024.0)) + "MB");

        } catch (Exception e) {
            throw new MediaUploadException("Failed to process base64 image: " + e.getMessage(), e);
        }
    }

    // ==================== MEDIA RETRIEVAL ====================

    @Override
    public byte[] getReviewMedia(Long reviewId, String mediaType) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if ("image".equalsIgnoreCase(mediaType)) {
            return review.getImageData();
        } else if ("video".equalsIgnoreCase(mediaType)) {
            return review.getVideoData();
        }
        return null;
    }

    // ==================== OTHER METHODS ====================

    @Override
    public ReviewResponseDto getReviewById(Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));
        return ReviewResponseDto.fromEntity(review);
    }

    @Override
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        List<ReviewEntity> reviews = reviewRepository.findByProductId(productId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public List<ReviewResponseDto> getReviewsByUser(Long userId) {
        List<ReviewEntity> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto requestDto) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (requestDto.getRating() != null) {
            review.setRating(requestDto.getRating());
        }

        if (requestDto.getComment() != null) {
            review.setComment(requestDto.getComment());
        }

        review.setUpdatedAt(LocalDateTime.now());

        if (requestDto.getImages() != null && !requestDto.getImages().isEmpty()) {
            handleImageWithCompression(review, requestDto.getImages().get(0));
        }

        if (requestDto.getVideos() != null && !requestDto.getVideos().isEmpty()) {
            handleVideoWithCompression(review, requestDto.getVideos().get(0));
        }

        ReviewEntity updatedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));
        reviewRepository.delete(review);
    }

    @Override
    public Map<String, Object> getProductReviewSummary(Long productId) {
        Map<String, Object> summary = new HashMap<>();
        // 🔴 UPDATED: Only count approved reviews for average rating
        Double avgRating = reviewRepository.getAverageRatingForApprovedReviews(productId);
        Long reviewCount = reviewRepository.getApprovedReviewCountForProduct(productId);

        summary.put("productId", productId);
        summary.put("averageRating", avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
        summary.put("totalReviews", reviewCount != null ? reviewCount : 0);

        return summary;
    }

    @Override
    public ReviewResponseDto addImagesToReview(Long reviewId, List<MultipartFile> images) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (images != null && !images.isEmpty()) {
            handleImageWithCompression(review, images.get(0));
        }

        return ReviewResponseDto.fromEntity(reviewRepository.save(review));
    }

    @Override
    public ReviewResponseDto addVideosToReview(Long reviewId, List<MultipartFile> videos) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (videos != null && !videos.isEmpty()) {
            handleVideoWithCompression(review, videos.get(0));
        }

        return ReviewResponseDto.fromEntity(reviewRepository.save(review));
    }

    @Override
    public void removeMediaFromReview(Long reviewId, String mediaType) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if ("image".equalsIgnoreCase(mediaType)) {
            review.setImageData(null);
            review.setImageOriginalSize(null);
            review.setImageCompressedSize(null);
            review.setImageSize(null);
            review.setImageContentType(null);
            review.setImageName(null);
        } else if ("video".equalsIgnoreCase(mediaType)) {
            review.setVideoData(null);
            review.setVideoOriginalSize(null);
            review.setVideoCompressedSize(null);
            review.setVideoSize(null);
            review.setVideoContentType(null);
            review.setVideoName(null);
        }

        reviewRepository.save(review);
    }

    @Override
    public List<ReviewResponseDto> getRecentReviews(Long productId) {
        // 🔴 UPDATED: Only get recent approved reviews
        List<ReviewEntity> reviews = reviewRepository.findTopRecentApprovedReviews(productId);
        return reviews.stream()
                .limit(10)
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public List<ReviewResponseDto> getReviewsWithImages(Long productId) {
        // 🔴 UPDATED: Only get approved reviews with images
        List<ReviewEntity> reviews = reviewRepository.findApprovedReviewsWithImages(productId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public List<ReviewResponseDto> getReviewsWithVideos(Long productId) {
        // 🔴 UPDATED: Only get approved reviews with videos
        List<ReviewEntity> reviews = reviewRepository.findApprovedReviewsWithVideos(productId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    // ==================== NEW METHODS FOR ADMIN PANEL ====================

    @Override
    public List<ReviewResponseDto> getAllReviews() {
        List<ReviewEntity> reviews = reviewRepository.findAll();
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public ReviewResponseDto updateReviewStatus(Long reviewId, String status) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // 🔴 UPDATED: Set both status and approved fields
        review.setStatus(status);
        review.setApproved("approved".equalsIgnoreCase(status));
        review.setUpdatedAt(LocalDateTime.now());

        ReviewEntity updatedReview = reviewRepository.save(review);
        System.out.println("✅ Review " + reviewId + " status updated to: " + status + ", approved: " + review.getApproved());

        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public ReviewResponseDto addReplyToReview(Long reviewId, String replyComment) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // 🔴 IMPROVED: Store replies as JSON in the replies field instead of appending to comment
        List<Map<String, Object>> repliesList = new ArrayList<>();

        // Parse existing replies if any
        if (review.getReplies() != null && !review.getReplies().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                repliesList = mapper.readValue(review.getReplies(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                System.err.println("Error parsing replies: " + e.getMessage());
                repliesList = new ArrayList<>();
            }
        }

        // Add new reply
        Map<String, Object> newReply = new HashMap<>();
        newReply.put("id", System.currentTimeMillis());
        newReply.put("content", replyComment);
        newReply.put("adminName", "Admin");
        newReply.put("date", LocalDateTime.now().toString());
        repliesList.add(newReply);

        // Save replies as JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            review.setReplies(mapper.writeValueAsString(repliesList));
        } catch (Exception e) {
            System.err.println("Error saving replies: " + e.getMessage());
        }

        review.setUpdatedAt(LocalDateTime.now());
        ReviewEntity updatedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public ReviewResponseDto clearFlag(Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // 🔴 UPDATED: Actually clear the flagged field
        review.setFlagged(false);
        review.setUpdatedAt(LocalDateTime.now());

        ReviewEntity updatedReview = reviewRepository.save(review);
        System.out.println("✅ Flag cleared for review " + reviewId);

        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public List<ReviewResponseDto> bulkApproveReviews(List<Long> reviewIds) {
        List<ReviewEntity> reviews = reviewRepository.findAllById(reviewIds);
        for (ReviewEntity review : reviews) {
            review.setStatus("approved");
            review.setApproved(true);
            review.setUpdatedAt(LocalDateTime.now());
        }
        List<ReviewEntity> updatedReviews = reviewRepository.saveAll(reviews);
        System.out.println("✅ Bulk approved " + updatedReviews.size() + " reviews");

        return updatedReviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }
}