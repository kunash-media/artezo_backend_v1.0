package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ReviewRequestDto;
import com.artezo.dto.response.ReviewResponseDto;
import com.artezo.entity.ReviewEntity;
import com.artezo.exceptions.MediaUploadException;
import com.artezo.exceptions.ReviewNotFoundException;
import com.artezo.repository.ReviewRepository;

import com.artezo.service.ReviewService;
import com.artezo.service.MediaCompressorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;  // 🔴 ADD THIS
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private MediaCompressorService compressorService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // Active profile check
    private boolean isLocalMode() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("local");
    }

    @Override
    public ReviewResponseDto createReview(ReviewRequestDto requestDto) {

        // Check if user already reviewed this product
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

        long startTime = System.currentTimeMillis();
        System.out.println("⏱️ STARTING REVIEW CREATION AT: " + startTime);

        // Handle image uploads
        if (requestDto.getImages() != null && !requestDto.getImages().isEmpty()) {
            handleImageUpload(review, requestDto.getImages());
        }

        // Handle video uploads
        if (requestDto.getVideos() != null && !requestDto.getVideos().isEmpty()) {
            handleVideoUpload(review, requestDto.getVideos());
        }

        // Handle base64 images (for local testing)
        if (requestDto.getImageBase64() != null && !requestDto.getImageBase64().isEmpty()) {
            handleBase64Images(review, requestDto.getImageBase64());
        }

        // Handle base64 videos (for local testing)
        if (requestDto.getVideoBase64() != null && !requestDto.getVideoBase64().isEmpty()) {
            handleBase64Videos(review, requestDto.getVideoBase64());
        }

        ReviewEntity savedReview = reviewRepository.save(review);

        long endTime = System.currentTimeMillis();
        System.out.println("⏱️ FINISHED REVIEW CREATION AT: " + endTime);
        System.out.println("⏱️ TOTAL TIME: " + (endTime - startTime) + "ms (" +
                ((endTime - startTime) / 1000) + " seconds)");

        return ReviewResponseDto.fromEntity(savedReview);
    }

    // 🔴 UPDATED handleImageUpload with timeout
    private void handleImageUpload(ReviewEntity review, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            System.out.println("📸 No images to process");
            return;
        }

        System.out.println("📸 Processing " + images.size() + " images");
        List<String> imageUrls = new ArrayList<>();
        List<CompletableFuture<File>> futures = new ArrayList<>();
        List<MultipartFile> originalImages = new ArrayList<>(images); // Store originals for metadata

        // Submit all images for async compression
        for (MultipartFile image : images) {
            try {
                System.out.println("📸 Queuing image: " + image.getOriginalFilename());
                CompletableFuture<File> future = compressorService.compressImageAsync(image, review.getUserId());
                futures.add(future);
            } catch (Exception e) {
                throw new MediaUploadException("Failed to queue image: " + image.getOriginalFilename(), e);
            }
        }

        // 🔴 WAIT FOR ALL COMPRESSIONS TO COMPLETE
        try {
            System.out.println("⏳ Waiting for " + futures.size() + " images to compress...");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("✅ All images compressed successfully");
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("❌ Image compression timed out after 30 seconds");
            throw new MediaUploadException("Image compression took too long - timeout");
        } catch (Exception e) {
            System.err.println("❌ Image compression failed: " + e.getMessage());
            throw new MediaUploadException("Image compression failed", e);
        }

        // 🔴 PROCESS RESULTS - YE MISSING THA!
        for (int i = 0; i < futures.size(); i++) {
            try {
                CompletableFuture<File> future = futures.get(i);
                File compressedFile = future.get(); // Will return immediately since we waited

                MultipartFile originalImage = originalImages.get(i);

                // Generate URL with user ID
                String fileUrl = "/api/user/" + review.getUserId() + "/images/" + compressedFile.getName();
                imageUrls.add(fileUrl);

                // Store metadata (using first image's content type as reference)
                if (review.getImageContentType() == null) {
                    review.setImageContentType(originalImage.getContentType());
                }

                System.out.println("✅ Image " + (i+1) + " ready: " + compressedFile.getName() +
                        " | Size: " + (compressedFile.length() / 1024) + "KB");

            } catch (Exception e) {
                System.err.println("❌ Failed to process compressed image " + (i+1) + ": " + e.getMessage());
                throw new MediaUploadException("Failed to process compressed image", e);
            }
        }

        // 🔴 UPDATE TOTAL IMAGE SIZE (sum of all images)
        long totalImageSize = 0;
        for (CompletableFuture<File> future : futures) {
            try {
                totalImageSize += future.get().length();
            } catch (Exception e) {
                // Ignore, already handled
            }
        }
        review.setImageSize(totalImageSize);

        // Append new URLs to existing ones
        if (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) {
            List<String> existingUrls = new ArrayList<>(Arrays.asList(review.getImageUrlsArray()));
            existingUrls.addAll(imageUrls);
            review.setImageUrlsArray(existingUrls.toArray(new String[0]));
            System.out.println("📸 Appended " + imageUrls.size() + " URLs to existing " + existingUrls.size());
        } else {
            review.setImageUrlsArray(imageUrls.toArray(new String[0]));
            System.out.println("📸 Added " + imageUrls.size() + " new image URLs");
        }
    }

    // 🔴 UPDATED handleVideoUpload with timeout
    private void handleVideoUpload(ReviewEntity review, List<MultipartFile> videos) {
        if (videos == null || videos.isEmpty()) return;
        if (videos.size() > 1) throw new MediaUploadException("Only 1 video allowed");

        List<String> videoUrls = new ArrayList<>();
        MultipartFile video = videos.get(0);

        try {
            CompletableFuture<File> future = compressorService.compressVideoAsync(video, review.getUserId());

            // 🔴 WAIT WITH TIMEOUT
            File compressedFile = future.get(90, java.util.concurrent.TimeUnit.SECONDS);  // 90 sec timeout

            String fileUrl = "/api/user/" + review.getUserId() + "/videos/" + compressedFile.getName();
            videoUrls.add(fileUrl);

            review.setVideoContentType("video/mp4");
            review.setVideoSize(compressedFile.length());

        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("❌ Video compression timed out after 90 seconds");
            throw new MediaUploadException("Video compression timeout - try with smaller video");
        } catch (Exception e) {
            throw new MediaUploadException("Failed to upload video", e);
        }

        if (review.getVideoUrls() != null && !review.getVideoUrls().isEmpty()) {
            List<String> existingUrls = new ArrayList<>(Arrays.asList(review.getVideoUrlsArray()));
            existingUrls.addAll(videoUrls);
            review.setVideoUrlsArray(existingUrls.toArray(new String[0]));
        } else {
            review.setVideoUrlsArray(videoUrls.toArray(new String[0]));
        }
    }

    /**
     * HANDLE BASE64 IMAGES
     */
    private void handleBase64Images(ReviewEntity review, List<String> base64Images) {
        if (base64Images == null || base64Images.isEmpty()) return;

        List<String> imageUrls = new ArrayList<>();

        for (String base64 : base64Images) {
            try {
                String[] parts = base64.split(",");
                byte[] imageBytes = Base64.getDecoder().decode(parts.length > 1 ? parts[1] : parts[0]);

                String fileName = System.currentTimeMillis() + "_base64_image.jpg";
                saveBytesToDisk(imageBytes, "images", fileName);

                // USER ID IN URL
                imageUrls.add("/api/user/" + review.getUserId() + "/images/" + fileName);
                review.setImageContentType("image/jpeg");
                review.setImageSize((long) imageBytes.length);

            } catch (IllegalArgumentException | IOException e) {
                throw new MediaUploadException("Invalid base64 image data", e);
            }
        }

        if (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) {
            List<String> existingUrls = new ArrayList<>(Arrays.asList(review.getImageUrlsArray()));
            existingUrls.addAll(imageUrls);
            review.setImageUrlsArray(existingUrls.toArray(new String[0]));
        } else {
            review.setImageUrlsArray(imageUrls.toArray(new String[0]));
        }
    }

    /**
     * HANDLE BASE64 VIDEOS
     */
    private void handleBase64Videos(ReviewEntity review, List<String> base64Videos) {
        if (base64Videos == null || base64Videos.isEmpty()) return;

        List<String> videoUrls = new ArrayList<>();

        for (String base64 : base64Videos) {
            try {
                String[] parts = base64.split(",");
                byte[] videoBytes = Base64.getDecoder().decode(parts.length > 1 ? parts[1] : parts[0]);

                String fileName = System.currentTimeMillis() + "_base64_video.mp4";
                saveBytesToDisk(videoBytes, "videos", fileName);

                // USER ID IN URL
                videoUrls.add("/api/user/" + review.getUserId() + "/videos/" + fileName);
                review.setVideoContentType("video/mp4");
                review.setVideoSize((long) videoBytes.length);

            } catch (IllegalArgumentException | IOException e) {
                throw new MediaUploadException("Invalid base64 video data", e);
            }
        }

        if (review.getVideoUrls() != null && !review.getVideoUrls().isEmpty()) {
            List<String> existingUrls = new ArrayList<>(Arrays.asList(review.getVideoUrlsArray()));
            existingUrls.addAll(videoUrls);
            review.setVideoUrlsArray(existingUrls.toArray(new String[0]));
        } else {
            review.setVideoUrlsArray(videoUrls.toArray(new String[0]));
        }
    }

    /**
     * SAVE FILE AND GET URL
     */
    private String saveFileAndGetUrl(MultipartFile file, String subDirectory) throws IOException {
        String directory = uploadDir + "/" + subDirectory + "/";
        Path uploadPath = Paths.get(directory);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = System.currentTimeMillis() + "_" + originalFileName.replaceAll("\\s+", "_");

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return "/uploads/" + subDirectory + "/" + fileName;
    }

    /**
     * SAVE BYTES TO DISK
     */
    private void saveBytesToDisk(byte[] data, String subDirectory, String fileName) throws IOException {
        String directory = uploadDir + "/" + subDirectory + "/";
        Path uploadPath = Paths.get(directory);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, data);
    }

    /**
     * DELETE MEDIA FILES
     */
    private void deleteMediaFiles(ReviewEntity review) {
        if (review.getImageUrls() != null) {
            for (String imageUrl : review.getImageUrlsArray()) {
                deleteFile(imageUrl);
            }
        }

        if (review.getVideoUrls() != null) {
            for (String videoUrl : review.getVideoUrlsArray()) {
                deleteFile(videoUrl);
            }
        }
    }

    /**
     * DELETE SINGLE FILE
     */
    private void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String subDirectory = fileUrl.contains("/images/") ? "images" : "videos";
            Path filePath = Paths.get(uploadDir + "/" + subDirectory + "/" + fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + fileUrl);
        }
    }

    /**
     * CONVERT RATING DISTRIBUTION
     */
    private Map<Integer, Long> convertRatingDistribution(List<Object[]> distribution) {
        Map<Integer, Long> result = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            result.put(i, 0L);
        }

        if (distribution != null) {
            for (Object[] row : distribution) {
                Integer rating = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();
                result.put(rating, count);
            }
        }

        return result;
    }

    // ==================== EXISTING METHODS (UNCHANGED) ====================
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
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponseDto> getReviewsByUser(Long userId) {
        List<ReviewEntity> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .collect(Collectors.toList());
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
            handleImageUpload(review, requestDto.getImages());
        }

        if (requestDto.getVideos() != null && !requestDto.getVideos().isEmpty()) {
            handleVideoUpload(review, requestDto.getVideos());
        }

        ReviewEntity updatedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (!isLocalMode()) {
            deleteMediaFiles(review);
        }

        reviewRepository.delete(review);
    }

    @Override
    public Map<String, Object> getProductReviewSummary(Long productId) {
        Map<String, Object> summary = new HashMap<>();

        Double avgRating = reviewRepository.getAverageRatingForProduct(productId);
        Long reviewCount = reviewRepository.getReviewCountForProduct(productId);
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistribution(productId);

        summary.put("productId", productId);
        summary.put("averageRating", avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
        summary.put("totalReviews", reviewCount != null ? reviewCount : 0);
        summary.put("ratingDistribution", convertRatingDistribution(ratingDistribution));

        Long reviewsWithImages = (long) reviewRepository.findReviewsWithImages(productId).size();
        Long reviewsWithVideos = (long) reviewRepository.findReviewsWithVideos(productId).size();

        summary.put("reviewsWithImages", reviewsWithImages);
        summary.put("reviewsWithVideos", reviewsWithVideos);

        return summary;
    }

    @Override
    public byte[] getReviewMedia(Long reviewId, String mediaType) {
        throw new UnsupportedOperationException("Media is served via URLs, not binary");
    }

    @Override
    public ReviewResponseDto addImagesToReview(Long reviewId, List<MultipartFile> images) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        handleImageUpload(review, images);
        ReviewEntity updatedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public ReviewResponseDto addVideosToReview(Long reviewId, List<MultipartFile> videos) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        handleVideoUpload(review, videos);
        ReviewEntity updatedReview = reviewRepository.save(review);
        return ReviewResponseDto.fromEntity(updatedReview);
    }

    @Override
    public void removeMediaFromReview(Long reviewId, String mediaUrl) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (review.getImageUrls() != null && review.getImageUrls().contains(mediaUrl)) {
            List<String> urls = new ArrayList<>(Arrays.asList(review.getImageUrlsArray()));
            urls.remove(mediaUrl);
            review.setImageUrlsArray(urls.toArray(new String[0]));

            if (!isLocalMode()) {
                deleteFile(mediaUrl);
            }
        }

        if (review.getVideoUrls() != null && review.getVideoUrls().contains(mediaUrl)) {
            List<String> urls = new ArrayList<>(Arrays.asList(review.getVideoUrlsArray()));
            urls.remove(mediaUrl);
            review.setVideoUrlsArray(urls.toArray(new String[0]));

            if (!isLocalMode()) {
                deleteFile(mediaUrl);
            }
        }

        reviewRepository.save(review);
    }

    @Override
    public List<ReviewResponseDto> getRecentReviews(Long productId) {
        List<ReviewEntity> reviews = reviewRepository.findTopRecentReviews(productId);
        return reviews.stream()
                .limit(10)
                .map(ReviewResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponseDto> getReviewsWithImages(Long productId) {
        List<ReviewEntity> reviews = reviewRepository.findReviewsWithImages(productId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponseDto> getReviewsWithVideos(Long productId) {
        List<ReviewEntity> reviews = reviewRepository.findReviewsWithVideos(productId);
        return reviews.stream()
                .map(ReviewResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
