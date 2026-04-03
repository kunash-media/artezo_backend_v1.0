package com.artezo.service.serviceImpl;

import com.artezo.dto.response.OrderResponse;
import com.artezo.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String subject, String message) {
        try {
            MimeMessage messageOtp = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(messageOtp, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Extract OTP from message (format: "Your OTP is: <otp>. Valid for 5 minutes.")
            String otp = message.replace("Your OTP is: ", "").replace(". Valid for 5 minutes.", "");
            String htmlContent = buildOtpEmailTemplate(otp);
            helper.setText(htmlContent, true);

            // Try to add logo, but don't fail if it doesn't exist
            addLogoIfExists(helper);

            mailSender.send(messageOtp);
            logger.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, String customerName, String orderId,
                                           BigDecimal totalAmount,
                                           List<OrderResponse.OrderItemResponse> orderItems,
                                           String mobile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Order Placed Successfully - Artezo");

            String htmlContent = buildOrderConfirmationEmailTemplate(customerName, orderId, totalAmount, orderItems, mobile);
            helper.setText(htmlContent, true);

            addLogoIfExists(helper);

            mailSender.send(message);
            logger.info("Order confirmation email sent successfully to: {} for order: {}", toEmail, orderId);
        } catch (MessagingException e) {
            logger.error("Failed to send order confirmation email to {} for order {}: {}", toEmail, orderId, e.getMessage());
            throw new RuntimeException("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    /**
     * Attempts to add logo as inline attachment if the file exists
     * If file doesn't exist, logs a warning but doesn't throw an exception
     */
    private void addLogoIfExists(MimeMessageHelper helper) {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/Images/artezo_logo.jpg");
            if (logoResource.exists() && logoResource.isReadable()) {
                helper.addInline("logo", logoResource);
                logger.info("Logo added successfully to email from: {}", logoResource.getURL());
            } else {
                logger.warn("Logo file not found or not readable at static/Images/Logo.png - email will be sent without logo");
                logger.warn("Logo resource exists: {}, Logo resource readable: {}",
                        logoResource.exists(), logoResource.isReadable());
            }
        } catch (Exception e) {
            logger.warn("Could not add logo to email: {} - email will be sent without logo", e.getMessage());
            logger.debug("Full exception: ", e);
        }
    }

    private String buildOtpEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; border-radius: 10px; }" +
                ".header { text-align: center; border-bottom: 2px solid #00BFFF; padding-bottom: 20px; margin-bottom: 20px; }" +
                ".logo { max-width: 150px; height: auto; }" +
                ".company-name { font-size: 24px; font-weight: bold; color: #00BFFF; }" +
                "h1 { color: #333333; margin: 10px 0; }" +
                ".info { margin: 20px 0; line-height: 1.6; color: #555555; }" +
                ".otp-box { text-align: center; margin: 30px 0; }" +
                ".otp-code { font-size: 32px; font-weight: bold; color: #00BFFF; text-align: center; margin: 20px auto; padding: 20px; background-color: #f8f9fa; border-radius: 10px; border: 3px solid #00BFFF; letter-spacing: 10px; font-family: 'Courier New', monospace; display: inline-block; min-width: 250px; }" +
                ".copy-hint { color: #666666; font-size: 14px; margin-top: 10px; }" +
                "ul { margin: 10px 0; padding-left: 20px; }" +
                "li { margin-bottom: 8px; }" +
                ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #dddddd; text-align: center; color: #888888; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='cid:logo' alt='Logo' class='logo' style='display:block; margin: 0 auto;'>" +
                "<div class='company-name' style='display:none;'>Artezo</div>" +
                "<h1>OTP Verification</h1>" +
                "</div>" +
                "<div class='info'>" +
                "<p>Hello Dear,</p>" +
                "<p>You have requested an OTP for verification. Please use the following code:</p>" +
                "</div>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>" + otp + "</div>" +
                "<div class='copy-hint'>Select and copy the OTP above</div>" +
                "</div>" +
                "<div class='info'>" +
                "<p><strong>Important:</strong></p>" +
                "<ul>" +
                "<li>This OTP is valid for <strong>5 minutes</strong> only</li>" +
                "<li>Please do not share this code with anyone</li>" +
                "<li>If you didn't request this OTP, please ignore this email</li>" +
                "</ul>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2026 Artezo. All rights reserved.</p>" +
                "<p>This is an automated email, please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }


    private String buildOrderConfirmationEmailTemplate(String customerName, String orderId,
                                                       BigDecimal totalAmount,
                                                       List<OrderResponse.OrderItemResponse> orderItems,
                                                       String mobile) {

        StringBuilder productDetailsList = new StringBuilder();

        for (OrderResponse.OrderItemResponse item : orderItems) {
            double unitPrice = item.getSellingPrice() != null ? item.getSellingPrice() : 0.0;
            double subtotal = item.getItemTotal() != null ? item.getItemTotal() : 0.0;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;

            productDetailsList.append("<div class='product-item'>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Item</span>")
                    .append("<span class='product-value'>").append(item.getProductName()).append("</span>")
                    .append("</div>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Quantity</span>")
                    .append("<span class='product-value'>").append(quantity).append("</span>")
                    .append("</div>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Price</span>")
                    .append("<span class='product-value'>₹").append(String.format("%.2f", unitPrice)).append("</span>")
                    .append("</div>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Subtotal</span>")
                    .append("<span class='product-value' style='font-weight:700; color:#133F53;'>₹")
                    .append(String.format("%.2f", subtotal)).append("</span>")
                    .append("</div>")
                    .append("</div>");
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Order Confirmation - Artezo</title>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f8f5f0; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 16px; overflow: hidden; box-shadow: 0 6px 25px rgba(0,0,0,0.08); }" +
                ".header { text-align: center; padding: 50px 30px 35px 30px; background: linear-gradient(135deg, #133F53 0%, #1a4a63 100%); color: white; }" +
                ".logo { max-width: 130px; height: auto; margin-bottom: 20px; }" +
                ".company-name { font-size: 32px; font-weight: 700; color: #D89F34; margin-bottom: 15px; letter-spacing: 2px; }" +
                ".title { font-size: 26px; font-weight: 600; color: #D89F34; margin: 10px 0; }" +
                ".subtitle { font-size: 16px; color: #e0ae47; margin: 8px 0 0 0; }" +
                ".content { padding: 0 35px; }" +
                ".greeting { color: #555; font-size: 16px; margin: 25px 0; line-height: 1.7; }" +
                ".customer-greeting { color: #444; font-size: 17px; margin: 20px 0 10px 0; }" +
                ".customer-name { color: #133F53; font-weight: 600; }" +
                ".order-section { background-color: #fff; border-left: 5px solid #D89F34; padding: 30px; margin: 30px 0; border-radius: 10px; box-shadow: 0 3px 12px rgba(0,0,0,0.06); }" +
                ".order-title { font-size: 21px; font-weight: 600; color: #133F53; margin-bottom: 22px; border-bottom: 2px solid #e0ae47; padding-bottom: 12px; }" +
                ".product-list { margin: 20px 0; }" +
                ".product-item { background-color: #f9f6f0; padding: 20px; margin-bottom: 18px; border-radius: 12px; border-left: 5px solid #D89F34; }" +
                ".product-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #e0ae47; }" +
                ".product-row:last-child { margin-bottom: 0; padding-bottom: 0; border-bottom: none; }" +
                ".product-label { font-weight: 600; color: #133F53; font-size: 15px; min-width: 100px; }" +
                ".product-value { color: #222; font-size: 15.5px; text-align: right; flex: 1; }" +
                ".total-section { background: linear-gradient(135deg, #D89F34 0%, #e0ae47 100%); color: white; padding: 28px; border-radius: 12px; margin: 30px 0; text-align: center; }" +
                ".total-amount { font-size: 24px; font-weight: 700; }" +

                ".site-link { color: #D89F34; text-decoration: none; font-weight: 600; font-size: 15.5px; } .site-link:hover { color: #b8822c; text-decoration: underline; }" +
                ".contact-section { background: linear-gradient(135deg, #f8f5f0 0%, #f0e9d8 100%); padding: 30px; border-radius: 12px; margin: 30px 0; text-align: center; border: 1px solid #D89F34; }" +
                ".contact-text { font-size: 16px; color: #133F53; margin-bottom: 18px; font-weight: 500; }" +
                ".mobile-number { font-weight: 700; color: #133F53; }" +
                ".contact-link { display: inline-block; background: linear-gradient(135deg, #133F53 0%, #1a4a63 100%); color: white; padding: 15px 32px; text-decoration: none; border-radius: 30px; font-weight: 600; font-size: 15px; }" +
                ".divider { text-align: center; margin: 40px 0 25px 0; color: #D89F34; font-size: 28px; letter-spacing: 6px; }" +
                ".closing { text-align: center; margin: 30px 0; color: #444; line-height: 1.6; }" +
                ".happy-shopping { font-size: 21px; font-weight: 600; color: #133F53; margin-bottom: 12px; }" +
                "@media (max-width: 600px) {" +
                ".product-row { flex-direction: column; align-items: flex-start; text-align: left; }" +
                ".product-value { text-align: left; margin-top: 4px; }" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +

                "<div class='header'>" +
                "<img src='cid:logo' alt='Artezo Logo' class='logo' onerror='this.style.display=\"none\"; document.querySelector(\".company-name\").style.display=\"block\";'>" +
                "<div class='company-name'>ARTEZO</div>" +
                "<div class='title'>Order Placed Successfully!</div>" +
                "<div class='subtitle'>Thank you for choosing Artezo</div>" +
                "</div>" +

                "<div class='content'>" +
                "<div class='greeting'>" +
                "We're thrilled to bring your artistic vision to life. Every piece is crafted with passion to beautify your space." +
                "</div>" +
                "</div>" +

                "<div class='customer-greeting'>" +
                "Hi <span class='customer-name'>" + customerName + "</span>," +
                "</div>" +

                "<div class='order-section'>" +
                "<div class='order-title'>Your Order Details</div>" +
                "<div class='product-list'>" + productDetailsList + "</div>" +
                "<div class='total-section'>" +
                "<div class='total-amount'>Total Amount: ₹" + totalAmount.toPlainString() + "</div>" +
                "</div>" +
                "</div>" +

                "<div class='contact-section'>" +
                "<div class='contact-text'>For any queries or customization needs, reach us at:</div>" +
                "<span class='mobile-number'>" + "+91 79016 55023" + "</span><br><br>" +
                "<a href='tel:" + "+91 79016 55023" + "' class='contact-link'>Call Us Now</a>" +
                "</div>" +

                "<div style='text-align:center; margin: 30px 0;'>" +
                "<a href='https://artezo-7xs7.vercel.app/' class='site-link' target='_blank'>View Full Order Details on Website</a>" +
                "</div>" +

                "<div class='divider'>✧ ✧ ✧</div>" +

                "<div class='closing'>" +
                "<div class='happy-shopping'><span class='shopping-bag'>🖼️</span> Thank you for trusting Artezo!</div>" +
                "<div>We can't wait to see your space transformed with our art & decor.</div>" +
                "</div>" +

                "<div class='footer'>" +
                "<p>&copy; 2026 Artezo. All rights reserved.</p>" +
                "<p>Handcrafted with passion | Made for beautiful spaces</p>" +
                "<p>This is an automated email, please do not reply.</p>" +
                "</div>" +

                "</div>" +
                "</body>" +
                "</html>";
    }
}