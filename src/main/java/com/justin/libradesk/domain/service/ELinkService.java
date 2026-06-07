package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.ELink;
import com.justin.libradesk.infrastructure.web.LinkChecker;
import com.justin.libradesk.repository.ELinkRepository;
import com.justin.libradesk.validation.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages 856 e-resource links for a bib and verifies them via the injectable
 * {@link LinkChecker} seam (default real HTTP; tests supply a fake).
 */
public class ELinkService {

    private final ELinkRepository eLinkRepository;
    private final LinkChecker linkChecker;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ELinkService(ELinkRepository eLinkRepository, LinkChecker linkChecker,
                        AuditLogService auditLogService, Clock clock) {
        this.eLinkRepository = eLinkRepository;
        this.linkChecker = linkChecker;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    public List<ELink> listForBook(Long bookId) {
        return eLinkRepository.findByBook(bookId);
    }

    public ELink add(Long bookId, String url, String label, String actor) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("Link URL is required");
        }
        ELink saved = eLinkRepository.save(new ELink(null, bookId, url.trim(), label, null, null));
        auditLogService.record(actor, "ELINK_ADDED", "ELink", saved.id(), url);
        return saved;
    }

    public void delete(Long id, String actor) {
        eLinkRepository.deleteById(id);
        auditLogService.record(actor, "ELINK_DELETED", "ELink", id, null);
    }

    /**
     * Checks every link of a bib, storing each link's HTTP status and timestamp.
     *
     * @return the updated links
     */
    public List<ELink> checkLinks(Long bookId, String actor) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (ELink link : eLinkRepository.findByBook(bookId)) {
            int status = linkChecker.check(link.url());
            eLinkRepository.save(new ELink(link.id(), link.bookId(), link.url(), link.label(), status, now));
        }
        auditLogService.record(actor, "ELINKS_CHECKED", "Book", bookId, null);
        return eLinkRepository.findByBook(bookId);
    }
}
