package com.justin.libradesk.config;

import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.domain.service.AuditLogService;
import com.justin.libradesk.domain.service.AuthService;
import com.justin.libradesk.domain.service.BorrowingPolicy;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.CirculationService;
import com.justin.libradesk.domain.service.DashboardService;
import com.justin.libradesk.domain.service.PatronService;
import com.justin.libradesk.domain.service.ReservationService;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.jdbc.JdbcAuditLogRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookCopyRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookRepository;
import com.justin.libradesk.repository.jdbc.JdbcLoanRepository;
import com.justin.libradesk.repository.jdbc.JdbcPatronRepository;
import com.justin.libradesk.repository.jdbc.JdbcReservationRepository;
import com.justin.libradesk.repository.jdbc.JdbcUserRepository;

import java.time.Clock;

/**
 * Composition root: wires configuration, the database, repositories, and
 * services into a single object graph that lives for the whole application.
 *
 * <p>JavaFX instantiates FXML controllers with a no-arg constructor, so a
 * single instance is exposed via {@link #get()} for controllers to reach the
 * services. It also holds the currently signed-in {@link User} (the session).
 */
public final class AppContext implements AutoCloseable {

    private static AppContext instance;

    private final AppConfig config;
    private final DatabaseManager databaseManager;

    private final AuthService authService;
    private final PatronService patronService;
    private final CatalogService catalogService;
    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    private User currentUser;

    private AppContext(AppConfig config, DatabaseManager databaseManager) {
        this.config = config;
        this.databaseManager = databaseManager;
        Clock clock = Clock.systemDefaultZone();

        JdbcUserRepository userRepository = new JdbcUserRepository(databaseManager);
        JdbcPatronRepository patronRepository = new JdbcPatronRepository(databaseManager);
        JdbcBookRepository bookRepository = new JdbcBookRepository(databaseManager);
        JdbcBookCopyRepository bookCopyRepository = new JdbcBookCopyRepository(databaseManager);
        JdbcLoanRepository loanRepository = new JdbcLoanRepository(databaseManager);
        JdbcReservationRepository reservationRepository = new JdbcReservationRepository(databaseManager);
        JdbcAuditLogRepository auditLogRepository = new JdbcAuditLogRepository(databaseManager);

        this.auditLogService = new AuditLogService(auditLogRepository, clock);
        this.authService = new AuthService(userRepository);
        this.patronService = new PatronService(patronRepository, auditLogService);
        this.catalogService = new CatalogService(bookRepository, bookCopyRepository, auditLogService, clock);
        this.reservationService = new ReservationService(reservationRepository, patronRepository,
                bookRepository, auditLogService, clock);
        this.circulationService = new CirculationService(patronRepository, bookCopyRepository,
                loanRepository, auditLogService, reservationService, new BorrowingPolicy(), config, clock);
        this.dashboardService = new DashboardService(bookRepository, bookCopyRepository,
                patronRepository, loanRepository, reservationRepository);
    }

    /** Builds the single application context. Call once at startup. */
    public static AppContext initialize(AppConfig config, DatabaseManager databaseManager) {
        if (instance != null) {
            throw new IllegalStateException("AppContext already initialised");
        }
        instance = new AppContext(config, databaseManager);
        return instance;
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("AppContext not initialised");
        }
        return instance;
    }

    public AppConfig config() {
        return config;
    }

    public AuthService authService() {
        return authService;
    }

    public PatronService patronService() {
        return patronService;
    }

    public CatalogService catalogService() {
        return catalogService;
    }

    public CirculationService circulationService() {
        return circulationService;
    }

    public ReservationService reservationService() {
        return reservationService;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public AuditLogService auditLogService() {
        return auditLogService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public void close() {
        databaseManager.close();
        instance = null;
    }
}
