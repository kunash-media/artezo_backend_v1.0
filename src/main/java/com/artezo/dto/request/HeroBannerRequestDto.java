package com.artezo.dto.request;

public class HeroBannerRequestDto {

    private Integer bannerId;
    private byte[] bannerImg;         // upload as bytes
    private String imgDescription;

    public Integer getBannerId() {
        return bannerId;
    }

    public void setBannerId(Integer bannerId) {
        this.bannerId = bannerId;
    }

    public byte[] getBannerImg() {
        return bannerImg;
    }

    public void setBannerImg(byte[] bannerImg) {
        this.bannerImg = bannerImg;
    }

    public String getImgDescription() {
        return imgDescription;
    }

    public void setImgDescription(String imgDescription) {
        this.imgDescription = imgDescription;
    }
}