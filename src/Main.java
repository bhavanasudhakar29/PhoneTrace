import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * PhoneTrace - Used Phone History Verification System
 * Entry point: console menu that connects the user to every feature.
 */
public class Main {

    private static final Scanner sc = new Scanner(System.in);

    private static final PhoneDAO phoneDAO = new PhoneDAO();
    private static final OwnerDAO ownerDAO = new OwnerDAO();
    private static final OwnershipDAO ownershipDAO = new OwnershipDAO();
    private static final RepairDAO repairDAO = new RepairDAO();
    private static final TheftReportDAO theftDAO = new TheftReportDAO();
    private static final HistoryService historyService = new HistoryService();
    private static final RiskAnalyzer analyzer = new RiskAnalyzer();

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   PHONETRACE - Used Phone History Checker");
        System.out.println("==============================================");

        if (!connectToDatabase()) {
            return;
        }

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("================ MAIN MENU ================");
            System.out.println(" 1. Check phone history (IMEI search)");
            System.out.println(" 2. Manage phones");
            System.out.println(" 3. Manage owners");
            System.out.println(" 4. Transfer ownership");
            System.out.println(" 5. Manage repairs");
            System.out.println(" 6. Theft reports");
            System.out.println(" 0. Exit");
            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1: checkHistory(); break;
                case 2: managePhones(); break;
                case 3: manageOwners(); break;
                case 4: transferOwnership(); break;
                case 5: manageRepairs(); break;
                case 6: manageTheftReports(); break;
                case 0: running = false; break;
                default: System.out.println("Invalid option, try again.");
            }
        }
        DBConnection.close();
        System.out.println("Thank you for using PhoneTrace. Goodbye!");
    }

    private static boolean connectToDatabase() {
        try {
            DBConnection.getConnection();
            System.out.println("Connected to database successfully.");
            return true;
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check that: (1) MySQL is running, (2) schema.sql was executed,");
            System.out.println("(3) the password in DBConnection.java is correct, and");
            System.out.println("(4) the MySQL Connector/J jar is on the classpath.");
            return false;
        }
    }

    // =====================================================================
    // 1. CHECK HISTORY  (the main feature)
    // =====================================================================
    private static void checkHistory() {
        String imei = readImei();
        String seller = readOptionalMobile("Seller's mobile number (press Enter to skip): ");

        PhoneHistory history = historyService.buildHistory(imei, seller);
        if (history == null) {
            System.out.println();
            System.out.println("[!] No record found for IMEI " + imei + ".");
            System.out.println("    A phone with no registered history is itself a risk.");
            System.out.println("    Ask the seller for the original purchase bill.");
            return;
        }
        AnalysisResult result = analyzer.analyze(history);
        HistoryReport.print(history, result);
    }

    // =====================================================================
    // 2. PHONES
    // =====================================================================
    private static void managePhones() {
        System.out.println("\n--- MANAGE PHONES ---");
        System.out.println(" 1. Register new phone");
        System.out.println(" 2. View all phones");
        System.out.println(" 3. Update phone details");
        System.out.println(" 4. Delete phone");
        switch (readInt("Choose: ")) {
            case 1: registerPhone(); break;
            case 2: listPhones(); break;
            case 3: updatePhone(); break;
            case 4: deletePhone(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void registerPhone() {
        String imei = readImei();
        Phone phone;
        try {
            phone = new Phone(imei, readNonEmpty("Brand: "), readNonEmpty("Model: "));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        listOwners();
        int ownerId = readInt("First owner's ID (add the owner from menu 3 if not listed): ");
        if (ownerDAO.getById(String.valueOf(ownerId)) == null) {
            System.out.println("Owner not found.");
            return;
        }
        LocalDate date = readDate("Purchase date (yyyy-mm-dd, Enter = today): ");
        if (ownershipDAO.registerPhoneWithFirstOwner(phone, ownerId, date)) {
            System.out.println("Phone registered successfully with its first owner.");
        }
    }

    private static void listPhones() {
        List<Phone> phones = phoneDAO.getAll();
        System.out.println("\nRegistered phones:");
        if (phones.isEmpty()) System.out.println("  (none)");
        for (Phone p : phones) {
            System.out.println("  " + p);
        }
    }

    private static void updatePhone() {
        Phone phone = phoneDAO.getById(readImei());
        if (phone == null) {
            System.out.println("Phone not found.");
            return;
        }
        try {
            phone.setBrand(readOrKeep("Brand", phone.getBrand()));
            phone.setModel(readOrKeep("Model", phone.getModel()));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        System.out.println(phoneDAO.update(phone) ? "Phone updated." : "Update failed.");
    }

    private static void deletePhone() {
        String imei = readImei();
        if (phoneDAO.getById(imei) == null) {
            System.out.println("Phone not found.");
            return;
        }
        System.out.println("Warning: this also deletes the phone's ownership, repair and theft history.");
        if (readYesNo("Are you sure? (Y/N): ")) {
            System.out.println(phoneDAO.delete(imei) ? "Phone deleted." : "Delete failed.");
        }
    }

    // =====================================================================
    // 3. OWNERS
    // =====================================================================
    private static void manageOwners() {
        System.out.println("\n--- MANAGE OWNERS ---");
        System.out.println(" 1. Add owner");
        System.out.println(" 2. View all owners");
        System.out.println(" 3. Update owner");
        System.out.println(" 4. Delete owner");
        switch (readInt("Choose: ")) {
            case 1: addOwner(); break;
            case 2: listOwners(); break;
            case 3: updateOwner(); break;
            case 4: deleteOwner(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void addOwner() {
        try {
            Owner owner = new Owner(0, readNonEmpty("Name: "), readNonEmpty("Mobile number (10 digits): "));
            if (ownerDAO.add(owner)) {
                System.out.println("Owner added with ID " + owner.getOwnerId() + ".");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void listOwners() {
        List<Owner> owners = ownerDAO.getAll();
        System.out.println("\nOwners:");
        if (owners.isEmpty()) System.out.println("  (none)");
        for (Owner o : owners) {
            System.out.println("  " + o);
        }
    }

    private static void updateOwner() {
        Owner owner = ownerDAO.getById(String.valueOf(readInt("Owner ID: ")));
        if (owner == null) {
            System.out.println("Owner not found.");
            return;
        }
        try {
            owner.setName(readOrKeep("Name", owner.getName()));
            owner.setPhone(readOrKeep("Mobile", owner.getPhone()));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        System.out.println(ownerDAO.update(owner) ? "Owner updated." : "Update failed.");
    }

    private static void deleteOwner() {
        String id = String.valueOf(readInt("Owner ID to delete: "));
        if (ownerDAO.getById(id) == null) {
            System.out.println("Owner not found.");
            return;
        }
        if (readYesNo("Are you sure? (Y/N): ") && ownerDAO.delete(id)) {
            System.out.println("Owner deleted.");
        }
    }

    // =====================================================================
    // 4. TRANSFER OWNERSHIP  (runs as a database transaction)
    // =====================================================================
    private static void transferOwnership() {
        String imei = readImei();
        if (phoneDAO.getById(imei) == null) {
            System.out.println("This phone is not registered.");
            return;
        }
        if (theftDAO.hasActiveReport(imei)) {
            System.out.println("[BLOCKED] This phone has an ACTIVE theft report. Ownership cannot be transferred.");
            return;
        }
        Ownership current = ownershipDAO.getCurrent(imei);
        if (current != null) {
            System.out.println("Current owner: " + current.getOwnerName() + " (since " + current.getFromDate() + ")");
        }
        listOwners();
        int newOwnerId = readInt("New owner's ID: ");
        if (ownerDAO.getById(String.valueOf(newOwnerId)) == null) {
            System.out.println("Owner not found.");
            return;
        }
        if (current != null && current.getOwnerId() == newOwnerId) {
            System.out.println("This person already owns the phone.");
            return;
        }
        LocalDate date = readDate("Transfer date (yyyy-mm-dd, Enter = today): ");
        if (ownershipDAO.transferOwnership(imei, newOwnerId, date)) {
            System.out.println("Ownership transferred successfully.");
        }
    }

    // =====================================================================
    // 5. REPAIRS
    // =====================================================================
    private static void manageRepairs() {
        System.out.println("\n--- MANAGE REPAIRS ---");
        System.out.println(" 1. Add repair record");
        System.out.println(" 2. View repairs of a phone");
        System.out.println(" 3. Update repair record");
        System.out.println(" 4. Delete repair record");
        switch (readInt("Choose: ")) {
            case 1: addRepair(); break;
            case 2: viewRepairs(); break;
            case 3: updateRepair(); break;
            case 4: deleteRepair(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void addRepair() {
        String imei = readImei();
        if (phoneDAO.getById(imei) == null) {
            System.out.println("This phone is not registered.");
            return;
        }
        try {
            Repair r = new Repair(0, imei,
                    readNonEmpty("Part replaced (e.g. Display, Battery): "),
                    readNonEmpty("Shop name: "),
                    readDate("Repair date (yyyy-mm-dd, Enter = today): "),
                    readYesNo("Genuine part? (Y/N): "));
            System.out.println(repairDAO.add(r) ? "Repair record added." : "Could not add repair.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void viewRepairs() {
        List<Repair> repairs = repairDAO.getByImei(readImei());
        if (repairs.isEmpty()) System.out.println("No repairs recorded.");
        for (Repair r : repairs) {
            System.out.println("  " + r);
        }
    }

    private static void updateRepair() {
        Repair r = repairDAO.getById(String.valueOf(readInt("Repair ID: ")));
        if (r == null) {
            System.out.println("Repair record not found.");
            return;
        }
        System.out.println("Current: " + r);
        try {
            r.setPart(readOrKeep("Part", r.getPart()));
            r.setShopName(readOrKeep("Shop name", r.getShopName()));
            r.setGenuine(readYesNo("Genuine part? (Y/N): "));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        System.out.println(repairDAO.update(r) ? "Repair updated." : "Update failed.");
    }

    private static void deleteRepair() {
        String id = String.valueOf(readInt("Repair ID to delete: "));
        if (repairDAO.getById(id) == null) {
            System.out.println("Repair record not found.");
            return;
        }
        if (readYesNo("Are you sure? (Y/N): ")) {
            System.out.println(repairDAO.delete(id) ? "Repair deleted." : "Delete failed.");
        }
    }

    // =====================================================================
    // 6. THEFT REPORTS
    // =====================================================================
    private static void manageTheftReports() {
        System.out.println("\n--- THEFT REPORTS ---");
        System.out.println(" 1. Report a phone as stolen");
        System.out.println(" 2. View theft reports of a phone");
        System.out.println(" 3. Mark a report as RECOVERED");
        System.out.println(" 4. View all theft reports");
        switch (readInt("Choose: ")) {
            case 1: reportStolen(); break;
            case 2: viewTheftReports(); break;
            case 3: markRecovered(); break;
            case 4: listAllTheftReports(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void reportStolen() {
        String imei = readImei();
        if (phoneDAO.getById(imei) == null) {
            System.out.println("This phone is not registered.");
            return;
        }
        if (theftDAO.hasActiveReport(imei)) {
            System.out.println("This phone already has an active theft report.");
            return;
        }
        LocalDate date = readDate("Date of theft report (yyyy-mm-dd, Enter = today): ");
        TheftReport report = new TheftReport(0, imei, date, TheftReport.ACTIVE);
        System.out.println(theftDAO.add(report) ? "Phone reported as STOLEN." : "Could not file report.");
    }

    private static void viewTheftReports() {
        List<TheftReport> reports = theftDAO.getByImei(readImei());
        if (reports.isEmpty()) System.out.println("No theft reports for this phone.");
        for (TheftReport t : reports) {
            System.out.println("  " + t);
        }
    }

    private static void markRecovered() {
        TheftReport report = theftDAO.getById(String.valueOf(readInt("Theft report ID: ")));
        if (report == null) {
            System.out.println("Report not found.");
            return;
        }
        if (!report.isActive()) {
            System.out.println("This report is already marked RECOVERED.");
            return;
        }
        report.setStatus(TheftReport.RECOVERED);
        System.out.println(theftDAO.update(report) ? "Report marked as RECOVERED." : "Update failed.");
    }

    private static void listAllTheftReports() {
        List<TheftReport> reports = theftDAO.getAll();
        if (reports.isEmpty()) System.out.println("No theft reports.");
        for (TheftReport t : reports) {
            System.out.println("  " + t + " | IMEI " + t.getImei());
        }
    }

    // =====================================================================
    // INPUT HELPERS - keep asking until the input is valid
    // =====================================================================
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("This field cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readImei() {
        while (true) {
            String s = readLine("Enter 15-digit IMEI: ");
            if (Phone.isValidImei(s)) return s;
            System.out.println("Invalid IMEI. It must be exactly 15 digits (dial *#06# on a phone to see it).");
        }
    }

    private static String readOptionalMobile(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty() || s.matches("\\d{10}")) return s;
            System.out.println("Mobile number must be 10 digits, or press Enter to skip.");
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) return LocalDate.now();
            try {
                LocalDate date = LocalDate.parse(s);
                if (date.isAfter(LocalDate.now())) {
                    System.out.println("Date cannot be in the future.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use the format yyyy-mm-dd, e.g. 2026-05-14.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            String s = readLine(prompt).toUpperCase();
            if (s.equals("Y")) return true;
            if (s.equals("N")) return false;
            System.out.println("Please enter Y or N.");
        }
    }

    // Shows the current value; pressing Enter keeps it
    private static String readOrKeep(String label, String current) {
        String s = readLine(label + " [" + current + "] (Enter to keep): ");
        return s.isEmpty() ? current : s;
    }
}
