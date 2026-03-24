package com.artezo.service.serviceImpl;

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
                                           BigDecimal totalAmount, List<String> productNames, String mobile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Order Placed Successfully - Artezo");

            String htmlContent = buildOrderConfirmationEmailTemplate(customerName, orderId, totalAmount, productNames, mobile);
            helper.setText(htmlContent, true);

            // Try to add logo, but don't fail if it doesn't exist
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
                                                       BigDecimal totalAmount, List<String> productNames, String mobile) {

        // Build product details list in the requested format
        StringBuilder productDetailsList = new StringBuilder();
        for (int i = 0; i < productNames.size(); i++) {
            productDetailsList.append("<div class='product-item'>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Product:</span>")
                    .append("<span class='product-value'>").append(productNames.get(i)).append("</span>")
                    .append("</div>")
                    .append("<div class='product-row'>")
                    .append("<span class='product-label'>Quantity: </span>")
                    .append("<span class='product-value'>1</span>")
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
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }" +
                ".header { text-align: center; padding: 40px 30px 30px 30px; background-color: white; }" +
                ".logo { max-width: 120px; height: auto; margin-bottom: 25px; }" +
                ".company-name { font-size: 28px; font-weight: bold; color: #d35400; margin-bottom: 20px; display: none; }" +
                ".title { font-size: 28px; font-weight: 600; color: #d35400; margin: 15px 0 10px 0; }" +
                ".subtitle { font-size: 16px; color: #d35400; margin: 5px 0 20px 0; opacity: 0.8; }" +
                ".content { padding: 0 30px; }" +
                ".greeting { color: #666; font-size: 16px; margin: 20px 0; line-height: 1.6; }" +
                ".customer-greeting { color: #666; font-size: 16px; margin: 15px 0; }" +
                ".customer-name { color: #d35400; font-weight: 600; }" +
                ".sparkle { color: #f39c12; }" +
                ".order-section { background-color: #fff; border-left: 4px solid #d35400; padding: 25px; margin: 25px 0; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }" +
                ".order-title { font-size: 20px; font-weight: 600; color: #d35400; margin-bottom: 20px; text-decoration: underline; }" +
                ".product-list { margin: 15px 0; }" +
                ".product-item { background-color: #f8f9fa; padding: 15px; margin-bottom: 15px; border-radius: 8px; border-left: 3px solid #d35400; }" +
                ".product-row { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 8px; }" +
                ".product-row:last-child { margin-bottom: 0; }" +
                ".product-label { font-weight: 600; color: #d35400; min-width: 120px; font-size: 15px; }" +
                ".product-value { color: #333; font-size: 15px; margin-left: 10px; }" +
                ".total-section { background: linear-gradient(135deg, #d35400 0%, #e67e22 100%); color: white; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; }" +
                ".total-amount { font-size: 22px; font-weight: 700; }" +
                ".contact-section { background: linear-gradient(135deg, #fef9f3 0%, #fcf3e9 100%); padding: 25px; border-radius: 10px; margin: 25px 0; text-align: center; border: 1px solid #d35400; }" +
                ".contact-text { font-size: 16px; color: #d35400; margin-bottom: 18px; font-weight: 500; }" +
                ".mobile-number { font-weight: 700; color: #d35400; }" +
                ".contact-link { display: inline-block; background: linear-gradient(135deg, #d35400 0%, #e67e22 100%); color: white; padding: 14px 28px; text-decoration: none; border-radius: 25px; font-weight: 600; font-size: 15px; transition: all 0.3s ease; box-shadow: 0 3px 10px rgba(211, 84, 0, 0.3); }" +
                ".contact-link:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(211, 84, 0, 0.4); }" +
                ".divider { text-align: center; margin: 30px 0; color: #d35400; font-size: 24px; }" +
                ".closing { text-align: center; margin: 30px 0; padding: 0 30px; }" +
                ".happy-shopping { font-size: 20px; font-weight: 600; margin: 20px 0; color: #d35400; }" +
                ".shopping-bag { color: #f39c12; }" +
                ".party-emoji { color: #f39c12; }" +
                ".contact-info { font-size: 15px; color: #666; margin: 15px 0; line-height: 1.5; }" +
                ".footer { text-align: center; background-color: #f8f9fa; padding: 25px 30px; color: #6c757d; font-size: 12px; line-height: 1.4; }" +
                ".footer p { margin: 5px 0; }" +
                "@media (max-width: 600px) {" +
                ".container { margin: 10px; }" +
                ".header, .content, .closing { padding: 20px; }" +
                ".title { font-size: 24px; }" +
                ".product-row { flex-direction: column; align-items: flex-start; }" +
                ".product-label { min-width: auto; margin-bottom: 5px; }" +
                ".product-value { margin-left: 0; }" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='cid:logo' alt='Pharmacy Logo' class='logo' onerror='this.style.display=\"none\"; document.querySelector(\".company-name\").style.display=\"block\";'>" +
                "<div class='company-name'>Artezo</div>" +
                "<div class='title'>Order Placed Successfully!</div>" +
                "<div class='subtitle'>Thank you for your order from Artezo!</div>" +
                "</div>" +

                "<div class='content'>"+
                "<div class='greeting'>"+
                "We're dedicated to providing you with quality medications and personalized care! Your health and well-being are our priority. ✨"+
                "</div>"+
                "</div>"+

                "<div class='customer-greeting'>" +
                "Hi <span class='customer-name'>" + customerName + "</span>," +
                "</div>" +

                "<div class='order-section'>" +
                "<div class='order-title'>Order Details:</div>" +
                "<div class='product-list'>" +
                productDetailsList.toString() +
                "</div>" +
                "<div class='total-section'>" +
                "<div class='total-amount'>Total Amount: ₹" + totalAmount.toPlainString() + "</div>" +
                "</div>" +
                "</div>" +

                "<div class='contact-section'>" +
                "<div class='contact-text'>For any queries about your order, please contact us at: <span class='mobile-number'>" + mobile + "</span></div>" +
                "<a href='tel:" + mobile + "' class='contact-link'>Call Us Now</a>" +
                "</div>" +
                "</div>" +

                "<div class='divider'>•••</div>" +

                "<div class='closing'>" +
                "<div class='happy-shopping'><span class='shopping-bag'>🛒</span> Wishing you health and wellness with every prescription! <span class='party-emoji'></span></div>" +
                "<div class='contact-info'>If you have any questions, feel free to reach out anytime.</div>" +
                "</div>" +

                "<div class='footer'>" +
                "<p>&copy; 2025 Artezo. All rights reserved.</p>" +
                "<p>This is an automated email, please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}