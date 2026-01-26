package com.aws.carddemo.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeepWikiGenerator {

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(99, 102, 241);
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(79, 70, 229);
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb DARK_COLOR = new DeviceRgb(30, 41, 59);
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb CODE_BG = new DeviceRgb(241, 245, 249);

    public static void generateWiki(String outputPath) throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addCoverPage(document);
        addOverviewSection(document);
        addArchitectureDiagram(document);
        addComponentGraph(document);
        addDataFlowDiagram(document);
        addEntityRelationshipDiagram(document);
        addApiEndpointsSection(document);
        addServicesDeepDive(document);
        addBatchProcessingSection(document);
        addMessagingArchitecture(document);
        addSecurityArchitecture(document);
        addDatabaseSchema(document);
        addConfigurationReference(document);
        addDependencyGraph(document);
        addFileStructure(document);
        addMigrationMapping(document);

        document.close();
    }

    private static void addCoverPage(Document document) {
        document.add(new Paragraph("\n\n\n\n"));
        
        Div titleBox = new Div()
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(40)
                .setMarginBottom(30);
        
        titleBox.add(new Paragraph("DeepWiki")
                .setFontSize(16)
                .setFontColor(ColorConstants.WHITE)
                .setOpacity(0.8f));
        
        titleBox.add(new Paragraph("CardDemo Java Migration")
                .setFontSize(42)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setMarginTop(10));
        
        titleBox.add(new Paragraph("Complete Technical Documentation")
                .setFontSize(18)
                .setFontColor(ColorConstants.WHITE)
                .setOpacity(0.9f)
                .setMarginTop(15));
        
        document.add(titleBox);

        Div infoBox = new Div()
                .setBackgroundColor(LIGHT_BG)
                .setPadding(25)
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1));

        infoBox.add(new Paragraph("Repository Information")
                .setFontSize(14)
                .setBold()
                .setFontColor(DARK_COLOR));
        
        infoBox.add(new Paragraph("Project: aws-mainframe-modernization-carddemo")
                .setFontSize(11).setMarginTop(10));
        infoBox.add(new Paragraph("Language: Java 17")
                .setFontSize(11));
        infoBox.add(new Paragraph("Framework: Spring Boot 3.2.0")
                .setFontSize(11));
        infoBox.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")))
                .setFontSize(11));
        
        document.add(infoBox);

        Div statsBox = new Div()
                .setMarginTop(20)
                .setPadding(20);

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));
        
        addStatCell(statsTable, "91", "Java Classes");
        addStatCell(statsTable, "50+", "REST Endpoints");
        addStatCell(statsTable, "17", "Database Tables");
        addStatCell(statsTable, "6", "Batch Jobs");
        
        statsBox.add(statsTable);
        document.add(statsBox);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addStatCell(Table table, String value, String label) {
        Cell cell = new Cell()
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(15)
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1));
        cell.add(new Paragraph(value).setFontSize(28).setBold().setFontColor(PRIMARY_COLOR));
        cell.add(new Paragraph(label).setFontSize(10).setFontColor(DARK_COLOR));
        table.addCell(cell);
    }

    private static void addOverviewSection(Document document) {
        addSectionTitle(document, "Overview", "High-level summary of the CardDemo Java migration");

        Div summaryBox = new Div()
                .setBackgroundColor(LIGHT_BG)
                .setPadding(20)
                .setMarginBottom(20);
        
        summaryBox.add(new Paragraph("CardDemo is a credit card management application originally built on IBM mainframe technology (COBOL/CICS/VSAM). This migration transforms it into a modern cloud-native Java application while preserving 100% of the original business logic.")
                .setFontSize(11));
        
        document.add(summaryBox);

        addSubsectionTitle(document, "Key Features");
        
        Table featuresTable = new Table(UnitValue.createPercentArray(new float[]{5, 95}))
                .setWidth(UnitValue.createPercentValue(100));
        
        addFeatureRow(featuresTable, "Account Management - Create, view, update, and deactivate credit card accounts");
        addFeatureRow(featuresTable, "Card Operations - Issue, manage, and track credit cards linked to accounts");
        addFeatureRow(featuresTable, "Transaction Processing - Post and validate transactions with 4 validation rules");
        addFeatureRow(featuresTable, "User Authentication - JWT-based security with admin and regular user roles");
        addFeatureRow(featuresTable, "Batch Processing - Daily and monthly batch jobs for interest, statements, expiration");
        addFeatureRow(featuresTable, "Real-time Authorization - RabbitMQ messaging for authorization requests");
        addFeatureRow(featuresTable, "Reporting - Generate account and transaction reports");
        
        document.add(featuresTable);

        addSubsectionTitle(document, "Technology Stack");
        
        Table techTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));
        addTechRow(techTable, "Runtime", "Java 17 (LTS)");
        addTechRow(techTable, "Framework", "Spring Boot 3.2.0, Spring Security, Spring Batch, Spring Data JPA");
        addTechRow(techTable, "Database", "PostgreSQL 14+ with Flyway migrations");
        addTechRow(techTable, "Messaging", "RabbitMQ with topic exchanges");
        addTechRow(techTable, "Caching", "Redis with entity-specific TTLs");
        addTechRow(techTable, "Security", "JWT tokens with BCrypt password encoding");
        addTechRow(techTable, "Build", "Maven 3.8+");
        document.add(techTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addFeatureRow(Table table, String feature) {
        table.addCell(new Cell().add(new Paragraph("\u2022").setFontColor(ACCENT_COLOR).setFontSize(12)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(feature).setFontSize(10)).setBorder(null));
    }

    private static void addTechRow(Table table, String category, String value) {
        table.addCell(new Cell().add(new Paragraph(category).setFontSize(10).setBold()).setBackgroundColor(LIGHT_BG).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(10)).setPadding(8));
    }

    private static void addArchitectureDiagram(Document document) {
        addSectionTitle(document, "System Architecture", "High-level architecture diagram showing component interactions");

        Div diagramBox = new Div()
                .setBackgroundColor(CODE_BG)
                .setPadding(20)
                .setMarginBottom(20);

        String diagram = 
            "+-----------------------------------------------------------------------------+\n" +
            "|                              CLIENT LAYER                                   |\n" +
            "|  +-------------+  +-------------+  +-------------+  +-------------+        |\n" +
            "|  |  Web App    |  | Mobile App  |  |  Admin UI   |  |  API Client |        |\n" +
            "|  +------+------+  +------+------+  +------+------+  +------+------+        |\n" +
            "+---------|----------------|----------------|----------------|---------------+\n" +
            "          |                |                |                |                 \n" +
            "          +----------------+----------------+----------------+                 \n" +
            "                                    |                                          \n" +
            "                                    v                                          \n" +
            "+-----------------------------------------------------------------------------+\n" +
            "|                           API GATEWAY / LOAD BALANCER                       |\n" +
            "+-------------------------------------+---------------------------------------+\n" +
            "                                      |                                        \n" +
            "                                      v                                        \n" +
            "+-----------------------------------------------------------------------------+\n" +
            "|                         SPRING BOOT APPLICATION                             |\n" +
            "|  +-----------------------------------------------------------------------+  |\n" +
            "|  |                        REST CONTROLLERS                               |  |\n" +
            "|  |  Auth | Account | Card | Customer | Transaction | User | Report       |  |\n" +
            "|  +-----------------------------------+-----------------------------------+  |\n" +
            "|                                      |                                      |\n" +
            "|  +-----------------------------------v-----------------------------------+  |\n" +
            "|  |                         SERVICE LAYER                                 |  |\n" +
            "|  |  AccountService | TransactionService | AuthService | CardService      |  |\n" +
            "|  +-----------------------------------+-----------------------------------+  |\n" +
            "|                                      |                                      |\n" +
            "|  +-----------------------------------v-----------------------------------+  |\n" +
            "|  |                       REPOSITORY LAYER                                |  |\n" +
            "|  |  AccountRepo | CardRepo | TransactionRepo | UserRepo | CustomerRepo   |  |\n" +
            "|  +-----------------------------------+-----------------------------------+  |\n" +
            "+--------------------------------------+--------------------------------------+\n" +
            "                                       |                                       \n" +
            "          +---------------------------+-+---------------------------+         \n" +
            "          v                           v v                           v         \n" +
            "+-----------------+         +-----------------+         +-----------------+   \n" +
            "|   PostgreSQL    |         |    RabbitMQ     |         |     Redis       |   \n" +
            "|   (Primary DB)  |         |   (Messaging)   |         |   (Caching)     |   \n" +
            "+-----------------+         +-----------------+         +-----------------+   ";

        diagramBox.add(new Paragraph(diagram).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(diagramBox);

        addSubsectionTitle(document, "Architecture Layers");
        
        document.add(new Paragraph("Controller Layer: REST endpoints handling HTTP requests. Maps directly to original CICS transactions. Handles request validation, response formatting, and error handling.")
                .setFontSize(10).setMarginBottom(8));
        document.add(new Paragraph("Service Layer: Contains all business logic from original COBOL programs. Implements transaction validation rules (codes 100-103), account management, and user authentication.")
                .setFontSize(10).setMarginBottom(8));
        document.add(new Paragraph("Repository Layer: Spring Data JPA repositories providing data access. Replaces VSAM file operations with database queries. Includes custom JPQL queries for complex operations.")
                .setFontSize(10).setMarginBottom(8));
        document.add(new Paragraph("Entity Layer: JPA entities mapping to PostgreSQL tables. Derived from COBOL copybooks (CVACT01Y, CVCUS01Y, CVACT02Y, etc.) with preserved field names.")
                .setFontSize(10));

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addComponentGraph(Document document) {
        addSectionTitle(document, "Component Graph", "Detailed view of application components and their relationships");

        Div graphBox = new Div()
                .setBackgroundColor(CODE_BG)
                .setPadding(20)
                .setMarginBottom(20);

        String graph = 
            "                    +------------------------------------------+                    \n" +
            "                    |           CardDemoApplication            |                    \n" +
            "                    |              (Main Class)                |                    \n" +
            "                    +--------------------+---------------------+                    \n" +
            "                                         |                                          \n" +
            "         +-------------------------------+-------------------------------+         \n" +
            "         |                               |                               |         \n" +
            "         v                               v                               v         \n" +
            "+-----------------+           +-----------------+           +-----------------+   \n" +
            "|   Controllers   |           |    Services     |           |     Config      |   \n" +
            "+-----------------+           +-----------------+           +-----------------+   \n" +
            "| AuthController  |---------->| AuthService     |           | SecurityConfig  |   \n" +
            "| AccountController|--------->| AccountService  |           | CacheConfig     |   \n" +
            "| CardController  |---------->| CardService     |           | AsyncConfig     |   \n" +
            "| CustomerController|-------->| CustomerService |           | RabbitMQConfig  |   \n" +
            "| TransactionController|----->| TransactionService|         | BatchConfig     |   \n" +
            "| UserController  |---------->| UserService     |           +-----------------+   \n" +
            "| ReportController|---------->| ReportService   |                                 \n" +
            "+-----------------+           +--------+--------+                                 \n" +
            "                                       |                                          \n" +
            "                                       v                                          \n" +
            "                            +-----------------+                                   \n" +
            "                            |  Repositories   |                                   \n" +
            "                            +-----------------+                                   \n" +
            "                            | AccountRepository|                                  \n" +
            "                            | CardRepository   |                                  \n" +
            "                            | CustomerRepository|                                 \n" +
            "                            | TransactionRepository|                              \n" +
            "                            | UserRepository   |                                  \n" +
            "                            | CardXrefRepository|                                 \n" +
            "                            +--------+--------+                                   \n" +
            "                                     |                                            \n" +
            "                                     v                                            \n" +
            "                            +-----------------+                                   \n" +
            "                            |    Entities     |                                   \n" +
            "                            +-----------------+                                   \n" +
            "                            | Account         |                                   \n" +
            "                            | Card            |                                   \n" +
            "                            | Customer        |                                   \n" +
            "                            | Transaction     |                                   \n" +
            "                            | User            |                                   \n" +
            "                            | CardXref        |                                   \n" +
            "                            +-----------------+                                   ";

        graphBox.add(new Paragraph(graph).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(graphBox);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addDataFlowDiagram(Document document) {
        addSectionTitle(document, "Data Flow", "How data flows through the system for key operations");

        addSubsectionTitle(document, "Transaction Posting Flow");
        
        Div flowBox = new Div()
                .setBackgroundColor(CODE_BG)
                .setPadding(15)
                .setMarginBottom(15);

        String flow = 
            "+---------+    +------------------+    +-------------------+    +--------------+\n" +
            "| Client  |--->| TransactionController|--->| TransactionService |--->| Validation   |\n" +
            "| Request |    | POST /transactions/post|   | postTransaction()  |    | Rules 100-103|\n" +
            "+---------+    +------------------+    +---------+---------+    +------+-------+\n" +
            "                                                 |                      |         \n" +
            "                                                 |    +-----------------+         \n" +
            "                                                 |    |                           \n" +
            "                                                 v    v                           \n" +
            "                                        +-----------------+                       \n" +
            "                                        |  CardXrefRepo   |                       \n" +
            "                                        |  (Lookup Card)  |                       \n" +
            "                                        +--------+--------+                       \n" +
            "                                                 |                                \n" +
            "                                                 v                                \n" +
            "                                        +-----------------+                       \n" +
            "                                        |  AccountRepo    |                       \n" +
            "                                        | (Check Balance) |                       \n" +
            "                                        +--------+--------+                       \n" +
            "                                                 |                                \n" +
            "                    +----------------------------+----------------------------+   \n" +
            "                    |                            |                            |   \n" +
            "                    v                            v                            v   \n" +
            "           +---------------+           +---------------+           +---------------+\n" +
            "           | Save Transaction|         | Update Account |         | Return Response|\n" +
            "           | to Database    |          | Balance        |          | to Client     |\n" +
            "           +---------------+           +---------------+           +---------------+";

        flowBox.add(new Paragraph(flow).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(flowBox);

        addSubsectionTitle(document, "Validation Rules (from CBTRN02C.cbl)");
        
        Table validationTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 60}))
                .setWidth(UnitValue.createPercentValue(100));
        
        validationTable.addHeaderCell(createHeaderCell("Code"));
        validationTable.addHeaderCell(createHeaderCell("Error Message"));
        validationTable.addHeaderCell(createHeaderCell("Validation Logic"));
        
        addValidationRow(validationTable, "100", "INVALID CARD NUMBER FOUND", "Card number lookup in card_xref table fails. Original: READ XREF-FILE INVALID KEY");
        addValidationRow(validationTable, "101", "ACCOUNT RECORD NOT FOUND", "Account associated with card not found. Original: READ ACCOUNT-FILE INVALID KEY");
        addValidationRow(validationTable, "102", "OVERLIMIT TRANSACTION", "Projected balance (current + transaction) exceeds credit limit. Original: IF ACCT-CREDIT-LIMIT >= WS-TEMP-BAL");
        addValidationRow(validationTable, "103", "TRANSACTION RECEIVED AFTER ACCT EXPIRATION", "Transaction date is after account expiration date. Original: IF ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS");
        
        document.add(validationTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addValidationRow(Table table, String code, String message, String logic) {
        table.addCell(new Cell().add(new Paragraph(code).setFontSize(10).setBold().setFontColor(PRIMARY_COLOR)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(message).setFontSize(9)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(logic).setFontSize(9)).setPadding(8));
    }

    private static void addEntityRelationshipDiagram(Document document) {
        addSectionTitle(document, "Entity Relationships", "Database entity relationships derived from COBOL copybooks");

        Div erdBox = new Div()
                .setBackgroundColor(CODE_BG)
                .setPadding(20)
                .setMarginBottom(20);

        String erd = 
            "+---------------------+         +---------------------+         +---------------------+\n" +
            "|      CUSTOMER       |         |       ACCOUNT       |         |        CARD         |\n" +
            "|   (CVCUS01Y.cpy)    |         |   (CVACT01Y.cpy)    |         |   (CVACT02Y.cpy)    |\n" +
            "+---------------------+         +---------------------+         +---------------------+\n" +
            "| PK cust_id          |<---+    | PK acct_id          |<---+    | PK card_num         |\n" +
            "|    cust_first_name  |    |    |    acct_active_status|    |    | FK card_acct_id     |---+\n" +
            "|    cust_last_name   |    |    |    acct_curr_bal     |    |    |    card_cvv_cd      |   |\n" +
            "|    cust_addr_line1  |    |    |    acct_credit_limit |    |    |    card_embossed_name|   |\n" +
            "|    cust_addr_city   |    |    |    acct_cash_credit  |    |    |    card_expiration  |   |\n" +
            "|    cust_addr_state  |    |    |    acct_open_date    |    |    |    card_active_status|   |\n" +
            "|    cust_ssn         |    |    |    acct_expiration   |    |    +---------------------+   |\n" +
            "|    cust_dob         |    |    |    acct_curr_cyc_credit|   |                             |\n" +
            "|    cust_fico_score  |    |    |    acct_curr_cyc_debit|    |                             |\n" +
            "+---------------------+    |    |    acct_group_id     |    |                             |\n" +
            "                           |    +---------------------+    |                             |\n" +
            "                           |              ^                |                             |\n" +
            "                           |              |                |                             |\n" +
            "+---------------------+    |    +---------+-------------+    |    +---------------------+   |\n" +
            "|     CARD_XREF       |    |    |     TRANSACTION     |    |    |        USER         |   |\n" +
            "|   (CVACT03Y.cpy)    |    |    |   (CVTRA05Y.cpy)    |    |    |   (CSUSR01Y.cpy)    |   |\n" +
            "+---------------------+    |    +---------------------+    |    +---------------------+   |\n" +
            "| PK card_num         |----+-->| PK tran_id          |    |    | PK user_id          |   |\n" +
            "| FK cust_id          |----+    | FK tran_card_num    |----+----+    user_first_name  |   |\n" +
            "| FK acct_id          |---------+    tran_type_cd     |    |    |    user_last_name   |   |\n" +
            "+---------------------+         |    tran_cat_cd      |    |    |    user_password    |   |\n" +
            "                                |    tran_amt         |    |    |    user_type (A/U)  |   |\n" +
            "                                |    tran_merchant_id |    |    +---------------------+   |\n" +
            "                                |    tran_orig_ts     |    |                             |\n" +
            "                                |    tran_proc_ts     |    |                             |\n" +
            "                                +---------------------+    |                             |\n" +
            "                                                          |                             |\n" +
            "                                                          +-----------------------------+";

        erdBox.add(new Paragraph(erd).setFontSize(6).setFontColor(DARK_COLOR));
        document.add(erdBox);

        addSubsectionTitle(document, "Entity Details");
        
        Table entityTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 60}))
                .setWidth(UnitValue.createPercentValue(100));
        
        entityTable.addHeaderCell(createHeaderCell("Entity"));
        entityTable.addHeaderCell(createHeaderCell("Copybook"));
        entityTable.addHeaderCell(createHeaderCell("Key Fields"));
        
        addEntityRow(entityTable, "Account", "CVACT01Y.cpy", "acctId (Long), acctActiveStatus, acctCurrBal, acctCreditLimit, acctExpirationDate");
        addEntityRow(entityTable, "Customer", "CVCUS01Y.cpy", "custId (Long), custFirstName, custLastName, custSsn, custFicoScore");
        addEntityRow(entityTable, "Card", "CVACT02Y.cpy", "cardNum (String), cardAcctId, cardCvvCd, cardExpirationDate, cardActiveStatus");
        addEntityRow(entityTable, "CardXref", "CVACT03Y.cpy", "cardNum (String), custId, acctId - Cross-reference table");
        addEntityRow(entityTable, "Transaction", "CVTRA05Y.cpy", "tranId (String), tranTypeCd, tranAmt, tranOrigTs, tranProcTs");
        addEntityRow(entityTable, "User", "CSUSR01Y.cpy", "userId (String), userPassword, userType ('A' admin, 'U' user)");
        
        document.add(entityTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addEntityRow(Table table, String entity, String copybook, String fields) {
        table.addCell(new Cell().add(new Paragraph(entity).setFontSize(10).setBold()).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(copybook).setFontSize(9).setFontColor(PRIMARY_COLOR)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(fields).setFontSize(9)).setPadding(8));
    }

    private static void addApiEndpointsSection(Document document) {
        addSectionTitle(document, "API Endpoints", "Complete REST API reference");

        addSubsectionTitle(document, "Authentication API");
        Table authTable = createApiTable();
        addApiRow(authTable, "POST", "/api/auth/login", "Authenticate user, returns JWT token", "Public");
        document.add(authTable);

        addSubsectionTitle(document, "Account API");
        Table accountTable = createApiTable();
        addApiRow(accountTable, "GET", "/api/accounts/{acctId}", "Get account by ID", "USER, ADMIN");
        addApiRow(accountTable, "GET", "/api/accounts", "List all accounts (paginated)", "USER, ADMIN");
        addApiRow(accountTable, "GET", "/api/accounts/active", "List active accounts", "USER, ADMIN");
        addApiRow(accountTable, "GET", "/api/accounts/expired", "List expired accounts", "USER, ADMIN");
        addApiRow(accountTable, "GET", "/api/accounts/overlimit", "List overlimit accounts", "USER, ADMIN");
        addApiRow(accountTable, "POST", "/api/accounts", "Create new account", "ADMIN");
        addApiRow(accountTable, "PUT", "/api/accounts/{acctId}", "Update account", "ADMIN");
        addApiRow(accountTable, "PATCH", "/api/accounts/{acctId}/deactivate", "Deactivate account", "ADMIN");
        document.add(accountTable);

        addSubsectionTitle(document, "Transaction API");
        Table tranTable = createApiTable();
        addApiRow(tranTable, "GET", "/api/transactions/{tranId}", "Get transaction by ID", "USER, ADMIN");
        addApiRow(tranTable, "GET", "/api/transactions", "List all transactions", "USER, ADMIN");
        addApiRow(tranTable, "GET", "/api/transactions/card/{cardNum}", "List by card number", "USER, ADMIN");
        addApiRow(tranTable, "POST", "/api/transactions/post", "Post transaction with validation", "USER, ADMIN");
        addApiRow(tranTable, "POST", "/api/transactions/validate", "Validate without posting", "USER, ADMIN");
        addApiRow(tranTable, "GET", "/api/transactions/card/{cardNum}/sum", "Sum by card", "USER, ADMIN");
        document.add(tranTable);

        addSubsectionTitle(document, "User API");
        Table userTable = createApiTable();
        addApiRow(userTable, "GET", "/api/users/{userId}", "Get user by ID", "ADMIN");
        addApiRow(userTable, "GET", "/api/users", "List all users", "ADMIN");
        addApiRow(userTable, "POST", "/api/users", "Create new user", "ADMIN");
        addApiRow(userTable, "PUT", "/api/users/{userId}", "Update user", "ADMIN");
        addApiRow(userTable, "DELETE", "/api/users/{userId}", "Delete user", "ADMIN");
        document.add(userTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static Table createApiTable() {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 30, 40, 20}))
                .setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell(createHeaderCell("Method"));
        table.addHeaderCell(createHeaderCell("Endpoint"));
        table.addHeaderCell(createHeaderCell("Description"));
        table.addHeaderCell(createHeaderCell("Roles"));
        return table;
    }

    private static void addApiRow(Table table, String method, String endpoint, String description, String roles) {
        DeviceRgb methodColor = method.equals("GET") ? ACCENT_COLOR : 
                               method.equals("POST") ? PRIMARY_COLOR :
                               method.equals("PUT") ? new DeviceRgb(245, 158, 11) :
                               method.equals("DELETE") ? new DeviceRgb(239, 68, 68) :
                               new DeviceRgb(139, 92, 246);
        
        table.addCell(new Cell().add(new Paragraph(method).setFontSize(9).setBold().setFontColor(methodColor)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(endpoint).setFontSize(8)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(description).setFontSize(9)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(roles).setFontSize(8)).setPadding(6));
    }

    private static void addServicesDeepDive(Document document) {
        addSectionTitle(document, "Services Deep Dive", "Detailed documentation of service layer components");

        addSubsectionTitle(document, "TransactionService");
        Div tranServiceBox = new Div().setBackgroundColor(LIGHT_BG).setPadding(15).setMarginBottom(15);
        tranServiceBox.add(new Paragraph("Purpose: Implements transaction processing logic from CBTRN02C.cbl").setFontSize(10).setBold());
        tranServiceBox.add(new Paragraph("Key Methods:").setFontSize(10).setMarginTop(10));
        tranServiceBox.add(new Paragraph("  - postTransaction(TransactionDto) - Posts transaction with full validation").setFontSize(9).setMarginLeft(10));
        tranServiceBox.add(new Paragraph("  - validateTransaction(TransactionDto) - Validates without posting").setFontSize(9).setMarginLeft(10));
        tranServiceBox.add(new Paragraph("  - getTransactionsByCard(cardNum, pageable) - Retrieves transactions by card").setFontSize(9).setMarginLeft(10));
        tranServiceBox.add(new Paragraph("  - sumTransactionsByCard(cardNum) - Calculates total by card").setFontSize(9).setMarginLeft(10));
        document.add(tranServiceBox);

        addSubsectionTitle(document, "AuthenticationService");
        Div authServiceBox = new Div().setBackgroundColor(LIGHT_BG).setPadding(15).setMarginBottom(15);
        authServiceBox.add(new Paragraph("Purpose: Implements sign-on logic from COSGN00C.cbl").setFontSize(10).setBold());
        authServiceBox.add(new Paragraph("Authentication Flow:").setFontSize(10).setMarginTop(10));
        authServiceBox.add(new Paragraph("1. Receive userId and password from login request").setFontSize(9).setMarginLeft(10));
        authServiceBox.add(new Paragraph("2. Look up user in database (replaces USRSEC VSAM file read)").setFontSize(9).setMarginLeft(10));
        authServiceBox.add(new Paragraph("3. Validate password using BCrypt encoder").setFontSize(9).setMarginLeft(10));
        authServiceBox.add(new Paragraph("4. Determine user type: 'A' for admin, 'U' for regular user").setFontSize(9).setMarginLeft(10));
        authServiceBox.add(new Paragraph("5. Generate JWT token with appropriate ROLE_ADMIN or ROLE_USER").setFontSize(9).setMarginLeft(10));
        document.add(authServiceBox);

        addSubsectionTitle(document, "AccountService");
        Div acctServiceBox = new Div().setBackgroundColor(LIGHT_BG).setPadding(15).setMarginBottom(15);
        acctServiceBox.add(new Paragraph("Purpose: Account lifecycle management from COACTUPC.cbl, COACTVWC.cbl").setFontSize(10).setBold());
        acctServiceBox.add(new Paragraph("Key Methods:").setFontSize(10).setMarginTop(10));
        acctServiceBox.add(new Paragraph("  - getAccount(acctId) - Retrieves account with caching (@Cacheable)").setFontSize(9).setMarginLeft(10));
        acctServiceBox.add(new Paragraph("  - updateAccountBalance(acctId, amount) - Updates balance and cycle credits/debits").setFontSize(9).setMarginLeft(10));
        acctServiceBox.add(new Paragraph("  - getOverlimitAccounts() - Finds accounts where balance > credit limit").setFontSize(9).setMarginLeft(10));
        acctServiceBox.add(new Paragraph("  - deactivateAccount(acctId) - Sets acctActiveStatus to 'N'").setFontSize(9).setMarginLeft(10));
        document.add(acctServiceBox);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addBatchProcessingSection(Document document) {
        addSectionTitle(document, "Batch Processing", "Spring Batch jobs replacing JCL batch processing");

        Table batchTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 50}))
                .setWidth(UnitValue.createPercentValue(100));
        
        batchTable.addHeaderCell(createHeaderCell("Batch Job"));
        batchTable.addHeaderCell(createHeaderCell("JCL Replaced"));
        batchTable.addHeaderCell(createHeaderCell("Description"));
        
        addBatchRow(batchTable, "TransactionPostingBatchJob", "POSTTRAN.jcl", "Posts daily transactions with validation rules 100-103. Rejected transactions written to rejected_transactions table.");
        addBatchRow(batchTable, "InterestCalculationBatchJob", "INTCALC.jcl", "Calculates monthly interest at 19.99% APR. Updates account balances and logs to interest_calculations table.");
        addBatchRow(batchTable, "DailyProcessingBatchJob", "CLOSEFIL, TRANBKP, WAITSTEP, OPENFIL", "Orchestrates daily processing sequence: backup, wait, process. Logs all steps to batch_job_log.");
        addBatchRow(batchTable, "MonthlyProcessingBatchJob", "INTCALC, COMBTRAN", "Monthly processing: interest calculation, transaction combination, statement preparation.");
        addBatchRow(batchTable, "AccountExpirationBatchJob", "N/A (new)", "Deactivates accounts past expiration date. Runs daily to maintain data integrity.");
        addBatchRow(batchTable, "StatementGenerationBatchJob", "N/A (new)", "Generates monthly statements with opening/closing balance, credits, debits, minimum payment.");
        
        document.add(batchTable);

        addSubsectionTitle(document, "Batch Configuration");
        Div configBox = new Div().setBackgroundColor(CODE_BG).setPadding(15).setMarginTop(15);
        configBox.add(new Paragraph("Chunk Size: 100 records per transaction").setFontSize(10));
        configBox.add(new Paragraph("Skip Policy: Skip TransactionValidationException, log to rejected_transactions").setFontSize(10));
        configBox.add(new Paragraph("Retry Policy: 3 retries for transient database failures").setFontSize(10));
        configBox.add(new Paragraph("Thread Pool: Dedicated batchExecutor with core=5, max=20 threads").setFontSize(10));
        document.add(configBox);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addBatchRow(Table table, String job, String jcl, String description) {
        table.addCell(new Cell().add(new Paragraph(job).setFontSize(9).setBold()).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(jcl).setFontSize(9).setFontColor(PRIMARY_COLOR)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(description).setFontSize(9)).setPadding(8));
    }

    private static void addMessagingArchitecture(Document document) {
        addSectionTitle(document, "Messaging Architecture", "RabbitMQ configuration replacing IBM MQ");

        Div mqDiagram = new Div().setBackgroundColor(CODE_BG).setPadding(20).setMarginBottom(20);
        String diagram = 
            "+-----------------------------------------------------------------------------+\n" +
            "|                           RABBITMQ MESSAGING                                |\n" +
            "+-----------------------------------------------------------------------------+\n" +
            "|                                                                             |\n" +
            "|  +---------------------------------------------------------------------+   |\n" +
            "|  |                    Exchange: carddemo.authorization                  |   |\n" +
            "|  |                           (Topic Exchange)                           |   |\n" +
            "|  +-----------------------------------+----------------------------------+   |\n" +
            "|                                      |                                      |\n" +
            "|              +-------------------+---+-------------------+                  |\n" +
            "|              |                                          |                  |\n" +
            "|              v                                          v                  |\n" +
            "|  +-------------------------+           +-------------------------+         |\n" +
            "|  | authorization.request   |           | authorization.response  |         |\n" +
            "|  | Queue                   |           | Queue                   |         |\n" +
            "|  +------------+------------+           +------------+------------+         |\n" +
            "|               |                                     |                      |\n" +
            "|               v                                     v                      |\n" +
            "|  +-------------------------+           +-------------------------+         |\n" +
            "|  | AuthorizationMessage    |           | AuthorizationMessage    |         |\n" +
            "|  | Consumer                |           | Producer                |         |\n" +
            "|  +-------------------------+           +-------------------------+         |\n" +
            "|                                                                             |\n" +
            "|  +---------------------------------------------------------------------+   |\n" +
            "|  |                    Exchange: carddemo.account                        |   |\n" +
            "|  |                           (Topic Exchange)                           |   |\n" +
            "|  +-----------------------------------+----------------------------------+   |\n" +
            "|                                      |                                      |\n" +
            "|              +-------------------+---+-------------------+                  |\n" +
            "|              v                                          v                  |\n" +
            "|  +-------------------------+           +-------------------------+         |\n" +
            "|  | account.extract.request |           | account.extract.response|         |\n" +
            "|  | Queue                   |           | Queue                   |         |\n" +
            "|  +-------------------------+           +-------------------------+         |\n" +
            "|                                                                             |\n" +
            "+-----------------------------------------------------------------------------+";
        mqDiagram.add(new Paragraph(diagram).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(mqDiagram);

        addSubsectionTitle(document, "Message Flow");
        document.add(new Paragraph("Authorization Request Flow: Client sends authorization request -> AuthorizationMessageProducer publishes to authorization.request queue -> AuthorizationMessageConsumer processes request -> Calls AuthorizationService -> Publishes response to authorization.response queue")
                .setFontSize(10).setMarginBottom(10));
        document.add(new Paragraph("Account Extraction Flow: External system requests account extraction with criteria (ACTIVE, OVERLIMIT, EXPIRED, ALL) -> AccountExtractionMessageConsumer queries database -> Returns matching accounts via response queue")
                .setFontSize(10));

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addSecurityArchitecture(Document document) {
        addSectionTitle(document, "Security Architecture", "JWT-based authentication replacing VSAM security");

        Div secDiagram = new Div().setBackgroundColor(CODE_BG).setPadding(20).setMarginBottom(20);
        String diagram = 
            "+-----------------------------------------------------------------------------+\n" +
            "|                         AUTHENTICATION FLOW                                 |\n" +
            "+-----------------------------------------------------------------------------+\n" +
            "|                                                                             |\n" +
            "|  +---------+    POST /api/auth/login    +-----------------+                |\n" +
            "|  | Client  | -------------------------> | AuthController  |                |\n" +
            "|  |         |    {userId, password}      |                 |                |\n" +
            "|  +----+----+                            +--------+--------+                |\n" +
            "|       |                                          |                         |\n" +
            "|       |                                          v                         |\n" +
            "|       |                                 +-----------------+                |\n" +
            "|       |                                 | Authentication  |                |\n" +
            "|       |                                 | Service         |                |\n" +
            "|       |                                 +--------+--------+                |\n" +
            "|       |                                          |                         |\n" +
            "|       |                                          v                         |\n" +
            "|       |                                 +-----------------+                |\n" +
            "|       |                                 | UserRepository  |                |\n" +
            "|       |                                 | (USRSEC file)   |                |\n" +
            "|       |                                 +--------+--------+                |\n" +
            "|       |                                          |                         |\n" +
            "|       |                                          v                         |\n" +
            "|       |                                 +-----------------+                |\n" +
            "|       |                                 | JwtTokenProvider|                |\n" +
            "|       |                                 | Generate Token  |                |\n" +
            "|       |                                 +--------+--------+                |\n" +
            "|       |                                          |                         |\n" +
            "|       |<-----------------------------------------+                         |\n" +
            "|       |         {token, userType, userId}                                  |\n" +
            "|       |                                                                    |\n" +
            "|       |    Authorization: Bearer <token>                                   |\n" +
            "|       | ---------------------------------------------------------->       |\n" +
            "|       |                                                                    |\n" +
            "|       |                                 +-----------------+                |\n" +
            "|       |                                 | JwtAuthFilter   |                |\n" +
            "|       |                                 | Validate Token  |                |\n" +
            "|       |                                 +-----------------+                |\n" +
            "|                                                                             |\n" +
            "+-----------------------------------------------------------------------------+";
        secDiagram.add(new Paragraph(diagram).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(secDiagram);

        addSubsectionTitle(document, "Role-Based Access Control");
        Table rbacTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}))
                .setWidth(UnitValue.createPercentValue(100));
        rbacTable.addHeaderCell(createHeaderCell("Resource"));
        rbacTable.addHeaderCell(createHeaderCell("ROLE_ADMIN"));
        rbacTable.addHeaderCell(createHeaderCell("ROLE_USER"));
        
        addRbacRow(rbacTable, "Account Read Operations", "Yes", "Yes");
        addRbacRow(rbacTable, "Account Write Operations", "Yes", "No");
        addRbacRow(rbacTable, "Transaction Posting", "Yes", "Yes");
        addRbacRow(rbacTable, "User Management", "Yes", "No");
        addRbacRow(rbacTable, "Report Generation", "Yes", "Yes");
        addRbacRow(rbacTable, "Batch Job Execution", "Yes", "No");
        
        document.add(rbacTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addRbacRow(Table table, String resource, String admin, String user) {
        table.addCell(new Cell().add(new Paragraph(resource).setFontSize(10)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(admin).setFontSize(10).setFontColor(admin.equals("Yes") ? ACCENT_COLOR : new DeviceRgb(239, 68, 68))).setPadding(8).setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph(user).setFontSize(10).setFontColor(user.equals("Yes") ? ACCENT_COLOR : new DeviceRgb(239, 68, 68))).setPadding(8).setTextAlignment(TextAlignment.CENTER));
    }

    private static void addDatabaseSchema(Document document) {
        addSectionTitle(document, "Database Schema", "PostgreSQL tables derived from VSAM file structures");

        Table schemaTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 40}))
                .setWidth(UnitValue.createPercentValue(100));
        
        schemaTable.addHeaderCell(createHeaderCell("Table"));
        schemaTable.addHeaderCell(createHeaderCell("VSAM File"));
        schemaTable.addHeaderCell(createHeaderCell("Primary Key"));
        schemaTable.addHeaderCell(createHeaderCell("Key Columns"));
        
        addSchemaRow(schemaTable, "accounts", "ACCTDATA", "acct_id (BIGINT)", "acct_active_status, acct_curr_bal, acct_credit_limit, acct_expiration_date");
        addSchemaRow(schemaTable, "customers", "CUSTDATA", "cust_id (BIGINT)", "cust_first_name, cust_last_name, cust_ssn, cust_fico_score");
        addSchemaRow(schemaTable, "cards", "CARDDATA", "card_num (VARCHAR 16)", "card_acct_id, card_cvv_cd, card_expiration_date, card_active_status");
        addSchemaRow(schemaTable, "card_xref", "CARDXREF", "card_num (VARCHAR 16)", "cust_id, acct_id - Cross-reference for lookups");
        addSchemaRow(schemaTable, "transactions", "TRANSACT", "tran_id (VARCHAR 16)", "tran_card_num, tran_type_cd, tran_amt, tran_orig_ts, tran_proc_ts");
        addSchemaRow(schemaTable, "users", "USRSEC", "user_id (VARCHAR 8)", "user_password, user_type, user_first_name, user_last_name");
        addSchemaRow(schemaTable, "transaction_types", "DB2 TRAN_TYPE", "type_cd (VARCHAR 2)", "type_description, type_category");
        addSchemaRow(schemaTable, "tran_cat_balance", "TCATBALF", "Composite", "acct_id + type_cd + cat_cd, tran_cat_bal");
        addSchemaRow(schemaTable, "rejected_transactions", "DALYREJS", "id (AUTO)", "tran_id, card_num, rejection_code, rejection_reason");
        addSchemaRow(schemaTable, "batch_job_log", "N/A", "id (AUTO)", "job_name, step_name, status, records_processed");
        
        document.add(schemaTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addSchemaRow(Table table, String tableName, String vsam, String pk, String columns) {
        table.addCell(new Cell().add(new Paragraph(tableName).setFontSize(9).setBold()).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(vsam).setFontSize(9).setFontColor(PRIMARY_COLOR)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(pk).setFontSize(8)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(columns).setFontSize(8)).setPadding(6));
    }

    private static void addConfigurationReference(Document document) {
        addSectionTitle(document, "Configuration Reference", "Application configuration properties");

        Div configBox = new Div().setBackgroundColor(CODE_BG).setPadding(20).setMarginBottom(20);
        String config = 
            "# application.yml\n\n" +
            "spring:\n" +
            "  datasource:\n" +
            "    url: jdbc:postgresql://localhost:5432/carddemo\n" +
            "    username: ${DB_USERNAME:postgres}\n" +
            "    password: ${DB_PASSWORD:postgres}\n" +
            "    hikari:\n" +
            "      maximum-pool-size: 20\n" +
            "      minimum-idle: 5\n\n" +
            "  jpa:\n" +
            "    hibernate:\n" +
            "      ddl-auto: validate\n" +
            "    show-sql: false\n\n" +
            "  rabbitmq:\n" +
            "    host: ${RABBITMQ_HOST:localhost}\n" +
            "    port: ${RABBITMQ_PORT:5672}\n" +
            "    username: ${RABBITMQ_USER:guest}\n" +
            "    password: ${RABBITMQ_PASS:guest}\n\n" +
            "  redis:\n" +
            "    host: ${REDIS_HOST:localhost}\n" +
            "    port: ${REDIS_PORT:6379}\n\n" +
            "jwt:\n" +
            "  secret: ${JWT_SECRET}\n" +
            "  expiration: 86400000  # 24 hours\n\n" +
            "carddemo:\n" +
            "  interest-rate: 0.1999  # 19.99% APR";
        configBox.add(new Paragraph(config).setFontSize(8).setFontColor(DARK_COLOR));
        document.add(configBox);

        addSubsectionTitle(document, "Cache TTL Configuration");
        Table cacheTable = new Table(UnitValue.createPercentArray(new float[]{30, 30, 40}))
                .setWidth(UnitValue.createPercentValue(100));
        cacheTable.addHeaderCell(createHeaderCell("Cache Name"));
        cacheTable.addHeaderCell(createHeaderCell("TTL"));
        cacheTable.addHeaderCell(createHeaderCell("Purpose"));
        addCacheRow(cacheTable, "accounts", "15 minutes", "Account lookups by ID");
        addCacheRow(cacheTable, "customers", "15 minutes", "Customer lookups by ID");
        addCacheRow(cacheTable, "cards", "10 minutes", "Card lookups by number");
        addCacheRow(cacheTable, "transactionTypes", "1 hour", "Reference data (rarely changes)");
        addCacheRow(cacheTable, "users", "5 minutes", "User authentication data");
        document.add(cacheTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addCacheRow(Table table, String name, String ttl, String purpose) {
        table.addCell(new Cell().add(new Paragraph(name).setFontSize(10).setBold()).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(ttl).setFontSize(10)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(purpose).setFontSize(10)).setPadding(8));
    }

    private static void addDependencyGraph(Document document) {
        addSectionTitle(document, "Dependency Graph", "Maven dependencies and their purposes");

        Table depTable = new Table(UnitValue.createPercentArray(new float[]{35, 15, 50}))
                .setWidth(UnitValue.createPercentValue(100));
        
        depTable.addHeaderCell(createHeaderCell("Dependency"));
        depTable.addHeaderCell(createHeaderCell("Version"));
        depTable.addHeaderCell(createHeaderCell("Purpose"));
        
        addDepRow(depTable, "spring-boot-starter-web", "3.2.0", "REST API, embedded Tomcat");
        addDepRow(depTable, "spring-boot-starter-data-jpa", "3.2.0", "JPA/Hibernate ORM");
        addDepRow(depTable, "spring-boot-starter-security", "3.2.0", "Authentication/Authorization");
        addDepRow(depTable, "spring-boot-starter-batch", "3.2.0", "Batch processing framework");
        addDepRow(depTable, "spring-boot-starter-amqp", "3.2.0", "RabbitMQ messaging");
        addDepRow(depTable, "spring-boot-starter-data-redis", "3.2.0", "Redis caching");
        addDepRow(depTable, "spring-boot-starter-validation", "3.2.0", "Bean validation");
        addDepRow(depTable, "postgresql", "42.6.0", "PostgreSQL JDBC driver");
        addDepRow(depTable, "flyway-core", "9.22.3", "Database migrations");
        addDepRow(depTable, "jjwt-api/impl/jackson", "0.12.3", "JWT token handling");
        addDepRow(depTable, "mapstruct", "1.5.5", "DTO-Entity mapping");
        addDepRow(depTable, "lombok", "1.18.30", "Boilerplate reduction");
        addDepRow(depTable, "itext7-core", "7.2.5", "PDF generation");
        
        document.add(depTable);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addDepRow(Table table, String dep, String version, String purpose) {
        table.addCell(new Cell().add(new Paragraph(dep).setFontSize(9)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(version).setFontSize(9).setFontColor(PRIMARY_COLOR)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(purpose).setFontSize(9)).setPadding(6));
    }

    private static void addFileStructure(Document document) {
        addSectionTitle(document, "File Structure", "Complete project directory structure");

        Div structureBox = new Div().setBackgroundColor(CODE_BG).setPadding(20);
        String structure = 
            "carddemo-java/\n" +
            "|-- pom.xml\n" +
            "|-- src/\n" +
            "|   |-- main/\n" +
            "|   |   |-- java/com/aws/carddemo/\n" +
            "|   |   |   |-- CardDemoApplication.java          # Main entry point\n" +
            "|   |   |   |-- batch/                            # Spring Batch jobs\n" +
            "|   |   |   |   |-- DailyProcessingBatchJob.java\n" +
            "|   |   |   |   |-- MonthlyProcessingBatchJob.java\n" +
            "|   |   |   |   |-- TransactionPostingBatchJob.java\n" +
            "|   |   |   |   |-- InterestCalculationBatchJob.java\n" +
            "|   |   |   |   |-- AccountExpirationBatchJob.java\n" +
            "|   |   |   |   |-- StatementGenerationBatchJob.java\n" +
            "|   |   |   |-- config/                           # Configuration\n" +
            "|   |   |   |   |-- SecurityConfig.java\n" +
            "|   |   |   |   |-- CacheConfig.java\n" +
            "|   |   |   |   |-- AsyncConfig.java\n" +
            "|   |   |   |   |-- RabbitMQConfig.java\n" +
            "|   |   |   |-- controller/                       # REST Controllers\n" +
            "|   |   |   |   |-- AuthController.java\n" +
            "|   |   |   |   |-- AccountController.java\n" +
            "|   |   |   |   |-- CardController.java\n" +
            "|   |   |   |   |-- CustomerController.java\n" +
            "|   |   |   |   |-- TransactionController.java\n" +
            "|   |   |   |   |-- UserController.java\n" +
            "|   |   |   |   |-- ReportController.java\n" +
            "|   |   |   |-- dto/                              # Data Transfer Objects\n" +
            "|   |   |   |-- entity/                           # JPA Entities\n" +
            "|   |   |   |   |-- Account.java\n" +
            "|   |   |   |   |-- Card.java\n" +
            "|   |   |   |   |-- Customer.java\n" +
            "|   |   |   |   |-- Transaction.java\n" +
            "|   |   |   |   |-- User.java\n" +
            "|   |   |   |   |-- CardXref.java\n" +
            "|   |   |   |-- exception/                        # Custom Exceptions\n" +
            "|   |   |   |-- mapper/                           # MapStruct Mappers\n" +
            "|   |   |   |-- messaging/                        # RabbitMQ\n" +
            "|   |   |   |-- repository/                       # JPA Repositories\n" +
            "|   |   |   |-- security/                         # JWT Security\n" +
            "|   |   |   |-- service/                          # Business Logic\n" +
            "|   |   |   |-- util/                             # Utilities\n" +
            "|   |   |-- resources/\n" +
            "|   |       |-- application.yml\n" +
            "|   |       |-- db/migration/\n" +
            "|   |           |-- V1__initial_schema.sql\n" +
            "|   |-- test/java/                                # Test Classes\n" +
            "|-- target/                                       # Build Output";
        structureBox.add(new Paragraph(structure).setFontSize(7).setFontColor(DARK_COLOR));
        document.add(structureBox);

        document.add(new com.itextpdf.layout.element.AreaBreak());
    }

    private static void addMigrationMapping(Document document) {
        addSectionTitle(document, "Migration Mapping", "Complete mapping from COBOL to Java components");

        addSubsectionTitle(document, "COBOL Program to Java Service Mapping");
        Table progTable = new Table(UnitValue.createPercentArray(new float[]{25, 35, 40}))
                .setWidth(UnitValue.createPercentValue(100));
        
        progTable.addHeaderCell(createHeaderCell("COBOL Program"));
        progTable.addHeaderCell(createHeaderCell("Java Component"));
        progTable.addHeaderCell(createHeaderCell("Function"));
        
        addMappingRow(progTable, "COSGN00C.cbl", "AuthController + AuthenticationService", "User sign-on with admin/regular routing");
        addMappingRow(progTable, "CBTRN02C.cbl", "TransactionPostingBatchJob + TransactionService", "Transaction posting with validation rules 100-103");
        addMappingRow(progTable, "COACTUPC.cbl", "AccountController + AccountService", "Account update operations");
        addMappingRow(progTable, "COACTVWC.cbl", "AccountController + AccountService", "Account view operations");
        addMappingRow(progTable, "COCRDLIC.cbl", "CardController + CardService", "Card list operations");
        addMappingRow(progTable, "COCRDUPC.cbl", "CardController + CardService", "Card update operations");
        addMappingRow(progTable, "COUSR00C-03C.cbl", "UserController + UserService", "User CRUD operations");
        addMappingRow(progTable, "CBACT04C.cbl", "InterestCalculationBatchJob", "Interest calculation batch");
        addMappingRow(progTable, "COPAUA0C.cbl", "AuthorizationController + RabbitMQ", "Authorization processing");
        addMappingRow(progTable, "CORPT00C.cbl", "ReportController + ReportService", "Report generation");
        
        document.add(progTable);

        addSubsectionTitle(document, "Data Type Mapping");
        Table typeTable = new Table(UnitValue.createPercentArray(new float[]{30, 30, 40}))
                .setWidth(UnitValue.createPercentValue(100));
        
        typeTable.addHeaderCell(createHeaderCell("COBOL Type"));
        typeTable.addHeaderCell(createHeaderCell("Java Type"));
        typeTable.addHeaderCell(createHeaderCell("Notes"));
        
        addTypeRow(typeTable, "PIC 9(11)", "Long", "Account/Customer IDs");
        addTypeRow(typeTable, "PIC X(16)", "String", "Card numbers, Transaction IDs");
        addTypeRow(typeTable, "PIC S9(9)V99 COMP-3", "BigDecimal", "Monetary amounts");
        addTypeRow(typeTable, "PIC X(8)", "String", "User IDs");
        addTypeRow(typeTable, "PIC X(1) VALUE 'Y'/'N'", "String", "Status flags");
        addTypeRow(typeTable, "CCYYMMDD", "LocalDate", "Date fields");
        addTypeRow(typeTable, "Timestamp", "LocalDateTime", "Timestamp fields");
        
        document.add(typeTable);
    }

    private static void addMappingRow(Table table, String cobol, String java, String function) {
        table.addCell(new Cell().add(new Paragraph(cobol).setFontSize(9).setFontColor(PRIMARY_COLOR)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(java).setFontSize(9)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(function).setFontSize(9)).setPadding(6));
    }

    private static void addTypeRow(Table table, String cobol, String java, String notes) {
        table.addCell(new Cell().add(new Paragraph(cobol).setFontSize(9)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(java).setFontSize(9).setBold()).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(notes).setFontSize(9)).setPadding(6));
    }

    private static void addSectionTitle(Document document, String title, String subtitle) {
        Div titleBox = new Div()
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(15)
                .setMarginTop(20)
                .setMarginBottom(15);
        
        titleBox.add(new Paragraph(title)
                .setFontSize(20)
                .setBold()
                .setFontColor(ColorConstants.WHITE));
        
        if (subtitle != null && !subtitle.isEmpty()) {
            titleBox.add(new Paragraph(subtitle)
                    .setFontSize(11)
                    .setFontColor(ColorConstants.WHITE)
                    .setOpacity(0.9f));
        }
        
        document.add(titleBox);
    }

    private static void addSubsectionTitle(Document document, String title) {
        document.add(new Paragraph(title)
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR)
                .setMarginTop(15)
                .setMarginBottom(10));
    }

    private static Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(10).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(SECONDARY_COLOR)
                .setPadding(8);
    }

    public static void main(String[] args) {
        try {
            String outputPath = args.length > 0 ? args[0] : "CardDemo_Java_DeepWiki.pdf";
            generateWiki(outputPath);
            System.out.println("DeepWiki generated successfully: " + outputPath);
        } catch (FileNotFoundException e) {
            System.err.println("Error generating wiki: " + e.getMessage());
        }
    }
}
