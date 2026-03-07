package com.artezo.dto.request;

import lombok.Data;


public class InstallationStepRequestDto {

    private int step;                    // required: 1, 2, 3...

    private String title;
    private String shortDescription;
    private String shortNote;

    private byte[] stepImage;            // image for this step (optional)
    private byte[] videoFile;            // video file for this step (optional, e.g. mp4 bytes)

    // Optional: if you want to support text-only steps or future extensions
    private String textContent;

    private Boolean hasNewImage;
    private Boolean hasNewVideo;

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getShortNote() {
        return shortNote;
    }

    public void setShortNote(String shortNote) {
        this.shortNote = shortNote;
    }

    public byte[] getStepImage() {
        return stepImage;
    }

    public void setStepImage(byte[] stepImage) {
        this.stepImage = stepImage;
    }

    public byte[] getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(byte[] videoFile) {
        this.videoFile = videoFile;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Boolean getHasNewImage() {
        return hasNewImage;
    }

    public void setHasNewImage(Boolean hasNewImage) {
        this.hasNewImage = hasNewImage;
    }

    public Boolean getHasNewVideo() {
        return hasNewVideo;
    }

    public void setHasNewVideo(Boolean hasNewVideo) {
        this.hasNewVideo = hasNewVideo;
    }
}