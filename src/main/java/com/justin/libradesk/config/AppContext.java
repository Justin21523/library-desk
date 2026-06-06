package com.justin.libradesk.config;

import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.domain.service.AuditLogService;
import com.justin.libradesk.domain.service.AuthService;
import com.justin.libradesk.domain.service.BorrowingPolicy;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.CirculationService;
import com.justin.libradesk.domain.service.DashboardService;
import com.justin.libradesk.domain.service.FineService;
import com.justin.libradesk.domain.service.PatronService;
import com.justin.libradesk.domain.service.ReportsService;
import com.justin.libradesk.domain.service.ReservationService;
import com.justin.libradesk.domain.service.SettingsService;
import com.justin.libradesk.domain.service.UserService;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.infrastructure.export.CsvService;
import com.justin.libradesk.infrastructure.export.PdfService;
import com.justin.libradesk.infrastructure.marc.LocSruClient;
import com.justin.libradesk.infrastructure.marc.MarcService;
import com.justin.libradesk.repository.jdbc.JdbcAuditLogRepository;
import com.justin.libradesk.repository.jdbc.JdbcAuthorRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookCopyRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookRepository;
import com.justin.libradesk.repository.jdbc.JdbcCategoryRepository;
import com.justin.libradesk.repository.jdbc.JdbcFineRepository;
import com.justin.libradesk.repository.jdbc.JdbcLoanRepository;
import com.justin.libradesk.repository.jdbc.JdbcPatronRepository;
import com.justin.libradesk.repository.jdbc.JdbcPublisherRepository;
import com.justin.libradesk.repository.jdbc.JdbcReservationRepository;
import com.justin.libradesk.repository.jdbc.JdbcSettingsRepository;
import com.justin.libradesk.repository.jdbc.JdbcSubjectRepository;
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
    private final UserService userService;
    private final PatronService patronService;
    private final CatalogService catalogService;
    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final DashboardService dashboardService;
    private final ReportsService reportsService;
    private final SettingsService settingsService;
    private final CsvService csvService;
    private final PdfService pdfService;
    private final MarcService marcService;
    private final LocSruClient locSruClient;
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
        JdbcSettingsRepository settingsRepository = new JdbcSettingsRepository(databaseManager);
        JdbcAuthorRepository authorRepository = new JdbcAuthorRepository(databaseManager);
        JdbcPublisherRepository publisherRepository = new JdbcPublisherRepository(databaseManager);
        JdbcCategoryRepository categoryRepository = new JdbcCategoryRepository(databaseManager);
        JdbcSubjectRepository subjectRepository = new JdbcSubjectRepository(databaseManager);
        JdbcFineRepository fineRepository = new JdbcFineRepository(databaseManager);
        JdbcAuditLogRepository auditLogRepository = new JdbcAuditLogRepository(databaseManager);

        this.auditLogService = new AuditLogService(auditLogRepository, clock);
        this.settingsService = new SettingsService(settingsRepository, config, auditLogService);
        this.fineService = new FineService(fineRepository, settingsService, auditLogService, clock);
        this.authService = new AuthService(userRepository);
        this.userService = new UserService(userRepository, auditLogService, clock);
        this.patronService = new PatronService(patronRepository, auditLogService);
        this.catalogService = new CatalogService(bookRepository, bookCopyRepository, authorRepository,
                publisherRepository, categoryRepository, subjectRepository, auditLogService, clock);
        this.reservationService = new ReservationService(reservationRepository, patronRepository,
                bookRepository, auditLogService, settingsService, clock);
        this.circulationService = new CirculationService(patronRepository, bookCopyRepository,
                loanRepository, auditLogService, reservationService, fineService, settingsService,
                new BorrowingPolicy(), clock);
        this.dashboardService = new DashboardService(bookRepository, bookCopyRepository,
                patronRepository, loanRepository, reservationRepository);
        this.reportsService = new ReportsService(loanRepository, bookRepository, bookCopyRepository,
                patronRepository, clock);
        this.csvService = new CsvService();
        this.pdfService = new PdfService();
        this.marcService = new MarcService();
        this.locSruClient = new LocSruClient(marcService, config);
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

    public UserService userService() {
        return userService;
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

    public FineService fineService() {
        return fineService;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public ReportsService reportsService() {
        return reportsService;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public CsvService csvService() {
        return csvService;
    }

    public PdfService pdfService() {
        return pdfService;
    }

    public MarcService marcService() {
        return marcService;
    }

    public LocSruClient locSruClient() {
        return locSruClient;
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
