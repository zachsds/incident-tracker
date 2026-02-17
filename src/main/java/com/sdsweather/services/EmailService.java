package com.sdsweather.services;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * EmailService - Sends outbound email notifications via SMTP.
 *
 * Currently handles password reset request emails to the system administrator.
 * SMTP credentials are read from environment variables to keep them out of
 * source code.
 *
 * Required environment variables:
 *   SDS_SMTP_HOST  - SMTP server hostname
 *   SDS_SMTP_PORT  - SMTP port (typically 587 for STARTTLS)
 *   SDS_SMTP_USER  - SMTP username / sender address
 *   SDS_SMTP_PASS  - SMTP password or app password
 *   SDS_ADMIN_EMAIL - Administrator's email address
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class EmailService {

    public static void sendPasswordResetRequest(String username, String details) throws Exception {

        String host = env("SDS_SMTP_HOST");
        String port = env("SDS_SMTP_PORT");
        String user = env("SDS_SMTP_USER");
        String pass = env("SDS_SMTP_PASS");
        String to = env("SDS_ADMIN_EMAIL");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(user));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject("Password Reset Request");

        String body = "Username: " + safe(username) + "\n\nDetails:\n" + safe(details);
        msg.setText(body);

        Transport.send(msg);
    }

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env var: " + key);
        return v;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
