package com.bandhanbook.app.utilities;

import com.bandhanbook.app.payload.request.ContactUsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class EmailUtilities {

    public Map<String, String> getThankYouEmail(String name) {
        Map<String, String> mail = new HashMap<>();
        String subject = "Thank you for contacting us";
        String message = """
        <html>
            <body style="font-family: Arial, sans-serif; line-height:1.6;">
                <p>Hello <b>%s</b>,</p>

                <p>Thank you for contacting <b>BandhanBook</b>.</p>

                <p>We have received your request and our team will reach you shortly.</p>

                <br>
                <p>Regards,<br>
                <b>BandhanBook Team</b></p>
            </body>
        </html>
        """.formatted(name);

        mail.put("subject", subject);
        mail.put("message", message);
        return mail;
    }

    public Map<String, String> getNotifyAdminContent(ContactUsRequest req) {
        Map<String, String> mail = new HashMap<>();
        String subject = "New Contact Us Request";
        String message = """
        <html>
        <body style="font-family: Arial, sans-serif; line-height:1.6;">
        
            <h2 style="color:#333;">New Contact Request</h2>
        
            <table style="border-collapse: collapse; width: 100%%;">
                <tr>
                    <td style="padding:8px; font-weight:bold;">Name:</td>
                    <td style="padding:8px;">%s</td>
                </tr>
                <tr>
                    <td style="padding:8px; font-weight:bold;">Email:</td>
                    <td style="padding:8px;">%s</td>
                </tr>
                <tr>
                    <td style="padding:8px; font-weight:bold; vertical-align: top;">Message:</td>
                    <td style="padding:8px;">%s</td>
                </tr>
            </table>

            <br>
            <p style="color:gray; font-size:12px;">
                This message was sent from BandhanBook contact form.
            </p>

        </body>
        </html>
        """.formatted(
                req.getName(),
                req.getEmail(),
                req.getMessage().replace("\n", "<br>")
        );
        mail.put("subject", subject);
        mail.put("message", message);
        return mail;
    }

    public Map<String, String> getForgotPasswordContent(String name, String otp) {
        Map<String, String> mail = new HashMap<>();
        String subject = "BandhanBook Password Reset OTP";
        String message = ("""
                Hello %s,
                
                We received a request to reset your password.
                
                Your One-Time Password (OTP) is:
                
                %s
                
                This OTP will expire in 10 minutes.
                
                If you did not request a password reset, please ignore this email.
                
                Regards,
                BandhanBook Team
                """.formatted(name, otp));
        mail.put("subject", subject);
        mail.put("message", message);
        return mail;
    }

    public Map<String, String> getRegistrationMailContent(String name, String otp) {
        Map<String, String> mail = new HashMap<>();
        String subject = "BandhanBook Registration OTP";
        String message = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
        
            <div style="max-width:600px; margin:auto; background:#ffffff; padding:20px; border-radius:8px;">
        
                <h2 style="color:#333;">Welcome to BandhanBook 🎉</h2>
        
                <p>Hello <b>%s</b>,</p>
        
                <p>Thank you for registering with <b>BandhanBook</b>.</p>
        
                <p>Your One-Time Password (OTP) is:</p>
        
                <div style="text-align:center; margin:20px 0;">
                    <span style="
                        font-size:28px;
                        letter-spacing:4px;
                        font-weight:bold;
                        color:#2c3e50;
                        background:#f1f1f1;
                        padding:10px 20px;
                        border-radius:6px;
                        display:inline-block;">
                        %s
                    </span>
                </div>
        
                <p style="color:#555;">This OTP will expire in <b>10 minutes</b>.</p>
        
                <p style="color:red;"><b>Do not share this OTP with anyone.</b></p>
        
                <br>
        
                <p>Regards,<br>
                <b>BandhanBook Team</b></p>
        
            </div>
        
        </body>
        </html>
        """.formatted(name, otp);
        mail.put("subject", subject);
        mail.put("message", message);
        return mail;
    }
}
