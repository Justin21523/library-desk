package com.justin.libradesk.config;

import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.domain.service.AuditLogService;
import com.justin.libradesk.domain.service.AuthService;
import com.justin.libradesk.domain.service.AuthorityService;
import com.justin.libradesk.domain.service.BorrowingPolicy;
import com.justin.libradesk.domain.service.CalendarService;
import com.justin.libradesk.domain.service.CatalogSearchService;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.CircPolicyService;
import com.justin.libradesk.domain.service.CirculationService;
import com.justin.libradesk.domain.service.DashboardService;
import com.justin.libradesk.domain.service.FineService;
import com.justin.libradesk.domain.service.LocationService;
import com.justin.libradesk.domain.service.NoticeService;
import com.justin.libradesk.domain.service.PatronAccountService;
import com.justin.libradesk.domain.service.PatronService;
import com.justin.libradesk.domain.service.ReportsService;
import com.justin.libradesk.domain.service.ReservationService;
import com.justin.libradesk.domain.service.SettingsService;
import com.justin.libradesk.domain.service.UserService;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.infrastructure.export.CsvService;
import com.justin.libradesk.infrastructure.export.PdfService;
import com.justin.libradesk.infrastructure.marc.LocAuthorityClient;
import com.justin.libradesk.infrastructure.marc.LocSruClient;
import com.justin.libradesk.infrastructure.marc.MarcService;
import com.justin.libradesk.infrastructure.notify.LoggingMailer;
import com.justin.libradesk.infrastructure.notify.Mailer;
import com.justin.libradesk.repository.jdbc.JdbcAuditLogRepository;
import com.justin.libradesk.repository.jdbc.JdbcAuthorRepository;
import com.justin.libradesk.repository.jdbc.JdbcAuthorityRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookCopyRepository;
import com.justin.libradesk.repository.jdbc.JdbcBookRepository;
import com.justin.libradesk.repository.jdbc.JdbcBranchRepository;
import com.justin.libradesk.repository.jdbc.JdbcCalendarRepository;
import com.justin.libradesk.repository.jdbc.JdbcCategoryRepository;
import com.justin.libradesk.repository.jdbc.JdbcCircPolicyRepository;
import com.justin.libradesk.repository.jdbc.JdbcFineRepository;
import com.justin.libradesk.repository.jdbc.JdbcLoanRepository;
import com.justin.libradesk.repository.jdbc.JdbcLocationRepository;
import com.justin.libradesk.repository.jdbc.JdbcPatronRepository;
import com.justin.libradesk.repository.jdbc.JdbcPaymentRepository;
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
    private final CatalogSearchService catalogSearchService;
    private final AuthorityService authorityService;
    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final CircPolicyService circPolicyService;
    private final CalendarService calendarService;
    private final LocationService locationService;
    private final PatronAccountService patronAccountService;
    private final NoticeService noticeService;
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
        JdbcAuthorityRepository authorityRepository = new JdbcAuthorityRepository(databaseManager);
        JdbcFineRepository fineRepository = new JdbcFineRepository(databaseManager);
        JdbcPaymentRepository paymentRepository = new JdbcPaymentRepository(databaseManager);
        JdbcAuditLogRepository auditLogRepository = new JdbcAuditLogRepository(databaseManager);
        JdbcBranchRepository branchRepository = new JdbcBranchRepository(databaseManager);
        JdbcLocationRepository locationRepository = new JdbcLocationRepository(databaseManager);
        JdbcCircPolicyRepository circPolicyRepository = new JdbcCircPolicyRepository(databaseManager);
        JdbcCalendarRepository calendarRepository = new JdbcCalendarRepository(databaseManager);

        this.auditLogService = new AuditLogService(auditLogRepository, clock);
        this.settingsService = new SettingsService(settingsRepository, config, auditLogService);
        this.fineService = new FineService(fineRepository, paymentRepository, settingsService,
                auditLogService, clock);
        this.circPolicyService = new CircPolicyService(circPolicyRepository, settingsService, auditLogService);
        this.calendarService = new CalendarService(calendarRepository, auditLogService);
        this.locationService = new LocationService(branchRepository, locationRepository, auditLogService);
        this.patronAccountService = new PatronAccountService(patronRepository, loanRepository,
                reservationRepository, fineRepository, settingsService);
        this.authService = new AuthService(userRepository);
        this.userService = new UserService(userRepository, auditLogService, clock);
        this.patronService = new PatronService(patronRepository, auditLogService);
        this.authorityService = new AuthorityService(authorityRepository, authorRepository,
                subjectRepository, new LocAuthorityClient(config), auditLogService);
        this.catalogService = new CatalogService(bookRepository, bookCopyRepository, authorRepository,
                publisherRepository, categoryRepository, subjectRepository, authorityService,
                auditLogService, clock);
        this.catalogSearchService = new CatalogSearchService(bookRepository, authorRepository,
                subjectRepository, publisherRepository);
        this.reservationService = new ReservationService(reservationRepository, patronRepository,
                bookRepository, auditLogService, settingsService, circPolicyService, clock);
        this.circulationService = new CirculationService(patronRepository, bookCopyRepository,
                bookRepository, loanRepository, auditLogService, reservationService, fineService,
                settingsService, circPolicyService, calendarService, patronAccountService,
                new BorrowingPolicy(), clock);
        Mailer mailer = new LoggingMailer();
        this.noticeService = new NoticeService(loanRepository, reservationRepository, patronRepository,
                bookCopyRepository, bookRepository, settingsService, auditLogService, mailer, clock);
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

    /** @return the context if initialised, otherwise {@code null} (no exception). */
    public static AppContext tryGet() {
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

    public AuthorityService authorityService() {
        return authorityService;
    }

    public CatalogSearchService catalogSearchService() {
        return catalogSearchService;
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

    public CircPolicyService circPolicyService() {
        return circPolicyService;
    }

    public CalendarService calendarService() {
        return calendarService;
    }

    public LocationService locationService() {
        return locationService;
    }

    public PatronAccountService patronAccountService() {
        return patronAccountService;
    }

    public NoticeService noticeService() {
        return noticeService;
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
