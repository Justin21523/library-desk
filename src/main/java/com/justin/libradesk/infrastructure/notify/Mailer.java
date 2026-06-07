package com.justin.libradesk.infrastructure.notify;

/**
 * Seam for delivering a notice to a patron. The default {@link LoggingMailer}
 * records notices to the log/audit trail (offline, test-friendly); a real SMTP
 * implementation can be substituted behind this interface without touching the
 * services that generate notices.
 */
public interface Mailer {

    /**
     * Delivers one message.
     *
     * @param to      recipient address (a notice is skipped upstream when null/blank)
     * @param subject message subject
     * @param body    message body
     */
    void send(String to, String subject, String body);
}
