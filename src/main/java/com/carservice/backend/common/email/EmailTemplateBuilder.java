package com.carservice.backend.common.email;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class EmailTemplateBuilder {

    private static final String PRIMARY_COLOR = "#2563eb";
    private static final String BG_COLOR = "#f8fafc";
    private static final String CARD_BG = "#ffffff";
    private static final String TEXT_DARK = "#0f172a";
    private static final String TEXT_MUTED = "#64748b";
    private static final String BORDER_COLOR = "#e2e8f0";

    private String wrapWithLayout(String title, String subtitle, String content) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>" + title + "</title>"
                + "<style>"
                + "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: " + BG_COLOR + "; margin: 0; padding: 24px; color: " + TEXT_DARK + "; }"
                + "  .container { max-width: 600px; margin: 0 auto; background: " + CARD_BG + "; border-radius: 16px; border: 1px solid " + BORDER_COLOR + "; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }"
                + "  .header { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); padding: 32px 24px; text-align: center; color: #ffffff; }"
                + "  .brand { font-size: 20px; font-weight: 800; letter-spacing: -0.5px; color: #ffffff; margin: 0; }"
                + "  .brand span { color: #60a5fa; }"
                + "  .title { font-size: 22px; font-weight: 700; margin: 16px 0 6px 0; color: #ffffff; }"
                + "  .subtitle { font-size: 13px; color: #94a3b8; margin: 0; }"
                + "  .content { padding: 32px 24px; font-size: 15px; line-height: 1.6; color: #334155; }"
                + "  .info-box { background: #f1f5f9; border-left: 4px solid " + PRIMARY_COLOR + "; padding: 16px; border-radius: 8px; margin: 20px 0; }"
                + "  .btn { display: inline-block; background-color: " + PRIMARY_COLOR + "; color: #ffffff !important; font-weight: 600; font-size: 14px; padding: 12px 28px; border-radius: 8px; text-decoration: none; margin-top: 20px; }"
                + "  .footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12px; color: " + TEXT_MUTED + "; border-top: 1px solid " + BORDER_COLOR + "; }"
                + "  .table { width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 14px; }"
                + "  .table td { padding: 8px 0; border-bottom: 1px solid #f1f5f9; }"
                + "  .table td.label { color: " + TEXT_MUTED + "; font-weight: 500; width: 40%; }"
                + "  .table td.val { color: " + TEXT_DARK + "; font-weight: 600; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "  <div class=\"header\">"
                + "    <h1 class=\"brand\">Addior <span>Mechanics Pro</span></h1>"
                + "    <h2 class=\"title\">" + title + "</h2>"
                + "    <p class=\"subtitle\">" + (subtitle != null ? subtitle : "Automotive Service & Fleet Management Platform") + "</p>"
                + "  </div>"
                + "  <div class=\"content\">"
                + content
                + "  </div>"
                + "  <div class=\"footer\">"
                + "    <p>© " + LocalDate.now().getYear() + " Addior Mechanics Pro. All rights reserved.</p>"
                + "    <p>This is an automated transactional notification. Please do not reply directly to this email.</p>"
                + "  </div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    public String buildWelcomeEmail(String name) {
        String body = "<p>Hello <strong>" + escape(name) + "</strong>,</p>"
                + "<p>Welcome to <strong>Addior Mechanics Pro</strong>! Your account has been successfully created.</p>"
                + "<p>You now have access to verified certified workshops, transparent pricing, seamless real-time vehicle telemetry, and instant scheduling.</p>"
                + "<div class=\"info-box\">"
                + "  <strong>Getting Started:</strong><br>"
                + "  • Add your vehicle to My Garage<br>"
                + "  • Browse curated service packages<br>"
                + "  • Schedule maintenance with instant dispatch to top-rated nearby garages"
                + "</div>"
                + "<p>If you have any questions, our 24/7 customer concierge is always ready to assist you.</p>";
        return wrapWithLayout("Welcome to Addior Mechanics Pro", "Your automotive maintenance companion", body);
    }

    public String buildVerificationEmail(String name, String verificationUrl) {
        String body = "<p>Hello <strong>" + escape(name) + "</strong>,</p>"
                + "<p>Please verify your email address to complete your registration and activate security features.</p>"
                + "<div style=\"text-align: center;\">"
                + "  <a href=\"" + escape(verificationUrl) + "\" class=\"btn\">Verify Email Address</a>"
                + "</div>"
                + "<p style=\"margin-top: 24px; font-size: 13px; color: " + TEXT_MUTED + ";\">If the button above does not work, copy and paste this link into your browser:<br>"
                + "<a href=\"" + escape(verificationUrl) + "\" style=\"color: " + PRIMARY_COLOR + "; word-break: break-all;\">" + escape(verificationUrl) + "</a></p>"
                + "<p>This verification link will expire in 24 hours.</p>";
        return wrapWithLayout("Verify Your Email Address", "Security Confirmation", body);
    }

    public String buildPasswordResetEmail(String name, String resetUrl) {
        String body = "<p>Hello <strong>" + escape(name) + "</strong>,</p>"
                + "<p>We received a request to reset your password for Addior Mechanics Pro.</p>"
                + "<div style=\"text-align: center;\">"
                + "  <a href=\"" + escape(resetUrl) + "\" class=\"btn\">Reset Your Password</a>"
                + "</div>"
                + "<p style=\"margin-top: 24px; font-size: 13px; color: " + TEXT_MUTED + ";\">If you did not request this password reset, please ignore this email or contact support immediately.</p>"
                + "<p style=\"font-size: 13px; color: " + TEXT_MUTED + ";\">Link expires in 15 minutes.</p>";
        return wrapWithLayout("Password Reset Request", "Account Security Alert", body);
    }

    public String buildBookingConfirmationEmail(String name, String bookingRef, String date, String timeSlot, BigDecimal totalAmount) {
        String body = "<p>Hello <strong>" + escape(name) + "</strong>,</p>"
                + "<p>Your service booking has been created successfully and dispatched to certified nearby workshops!</p>"
                + "<table class=\"table\">"
                + "  <tr><td class=\"label\">Booking Reference</td><td class=\"val\">" + escape(bookingRef) + "</td></tr>"
                + "  <tr><td class=\"label\">Scheduled Date</td><td class=\"val\">" + escape(date) + "</td></tr>"
                + "  <tr><td class=\"label\">Time Slot</td><td class=\"val\">" + escape(timeSlot) + "</td></tr>"
                + "  <tr><td class=\"label\">Estimated Amount</td><td class=\"val\">₹" + (totalAmount != null ? totalAmount.toPlainString() : "0.00") + "</td></tr>"
                + "</table>"
                + "<div class=\"info-box\">"
                + "  <strong>What's Next?</strong><br>"
                + "  Nearby certified workshops are reviewing your request. You will receive an instant notification once an expert workshop accepts your slot."
                + "</div>";
        return wrapWithLayout("Booking Confirmation #" + escape(bookingRef), "Service Appointment Confirmed", body);
    }

    public String buildBookingStatusEmail(String name, String bookingRef, String status, String details) {
        String body = "<p>Hello <strong>" + escape(name) + "</strong>,</p>"
                + "<p>Your booking <strong>#" + escape(bookingRef) + "</strong> has been updated to status: <strong>" + escape(status) + "</strong>.</p>"
                + "<div class=\"info-box\">"
                + "  <strong>Update Details:</strong><br>"
                + escape(details)
                + "</div>"
                + "<p>You can track the live telemetry and repair progress anytime in your customer dashboard.</p>";
        return wrapWithLayout("Booking Status Update: " + escape(status), "Reference #" + escape(bookingRef), body);
    }

    public String buildWorkshopApprovalEmail(String businessName) {
        String body = "<p>Dear <strong>" + escape(businessName) + "</strong>,</p>"
                + "<p>Congratulations! Your workshop partner registration for <strong>Addior Mechanics Pro</strong> has been <strong style=\"color: #16a34a;\">APPROVED</strong>.</p>"
                + "<div class=\"info-box\">"
                + "  <strong>Account Activated:</strong><br>"
                + "  • Your workshop profile is now live on the marketplace.<br>"
                + "  • You can now receive and claim nearby customer repair leads in your operating radius.<br>"
                + "  • Access workshop tools, floor job management, and telemetry dashboards."
                + "</div>"
                + "<p>Please log in to your partner portal to configure your workshop bays, top up your wallet, and begin claiming customer orders.</p>";
        return wrapWithLayout("Workshop Registration Approved!", "Welcome to our Certified Partner Network", body);
    }

    public String buildWorkshopRejectionEmail(String businessName, String reason) {
        String body = "<p>Dear <strong>" + escape(businessName) + "</strong>,</p>"
                + "<p>Thank you for your interest in joining Addior Mechanics Pro. After careful review, your workshop registration has not been approved at this time.</p>"
                + "<div class=\"info-box\" style=\"border-left-color: #dc2626;\">"
                + "  <strong>Reason for Decision:</strong><br>"
                + (reason != null && !reason.isBlank() ? escape(reason) : "Application did not meet our current partner verification criteria.")
                + "</div>"
                + "<p>If you believe this is in error or would like to submit updated documentation, please reach out to our partner onboarding team.</p>";
        return wrapWithLayout("Workshop Application Status", "Partner Verification Review", body);
    }

    public String buildAdminNotificationEmail(String subject, String message) {
        String body = "<p><strong>System Event Alert:</strong></p>"
                + "<div class=\"info-box\">"
                + escape(message)
                + "</div>"
                + "<p style=\"font-size: 12px; color: " + TEXT_MUTED + ";\">Time: " + java.time.LocalDateTime.now() + "</p>";
        return wrapWithLayout(subject, "Administrative Governance Alert", body);
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
