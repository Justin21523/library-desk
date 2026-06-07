package com.justin.libradesk.infrastructure.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Mailer} that writes notices to the log instead of sending real
 * email. Keeps the application self-contained and tests offline; swap in an SMTP
 * implementation (config-driven) to deliver real mail.
 */
public class LoggingMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailer.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("NOTICE to={} subject=\"{}\" body=\"{}\"", to, subject, body);
    }
}
