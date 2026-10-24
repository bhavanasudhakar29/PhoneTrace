import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Record management for the web page ("Manage records" screen).
 *
 * Every action here reuses the SAME DAO classes as the console menu,
 * so the rules are identical: model validation, PreparedStatements,
 * transactions for registering and transferring, and protected history.
 */
public class RegistryApi {

    private final PhoneDAO phoneDAO = new PhoneDAO();
    private final OwnerDAO ownerDAO = new OwnerDAO();
    private final OwnershipDAO ownershipDAO = new OwnershipDAO();
    private final RepairDAO repairDAO = new RepairDAO();
    private final TheftReportDAO theftDAO = new TheftReportDAO();

    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if ("GET".equals(method)) {
                Map<String, String> q = WebServer.parseQuery(ex.getRequestURI().getRawQuery());
                switch (path) {
                    case "/api/owners":          WebServer.sendJson(ex, 200, ownersJson()); return;
                    case "/api/registry/phones": WebServer.sendJson(ex, 200, phonesJson()); return;
                    case "/api/repairs":         WebServer.sendJson(ex, 200, repairsJson(q.getOrDefault("imei", ""))); return;
                    case "/api/thefts":          WebServer.sendJson(ex, 200, theftsJson()); return;
                    default: break;
                }
            } else if ("POST".equals(method)) {
                Map<String, String> f = WebServer.parseQuery(
                        new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String message;
                switch (path) {
                    case "/api/owners/add":      message = addOwner(f); break;
                    case "/api/owners/update":   message = updateOwner(f); break;
                    case "/api/owners/delete":   message = deleteOwner(f); break;
                    case "/api/phones/add":      message = addPhone(f); break;
                    case "/api/phones/update":   message = updatePhone(f); break;
                    case "/api/phones/delete":   message = deletePhone(f); break;
                    case "/api/transfer":        message = transfer(f); break;
                    case "/api/repairs/add":     message = addRepair(f); break;
                    case "/api/repairs/update":  message = updateRepair(f); break;
                    case "/api/repairs/delete":  message = deleteRepair(f); break;
                    case "/api/thefts/add":      message = reportStolen(f); break;
                    case "/api/thefts/recover":  message = markRecovered(f); break;
                    case "/api/reset":           message = resetSampleData(); break;
                    default: message = null;
                }
                if (message != null) {
                    WebServer.sendJson(ex, 200, "{\"ok\":true,\"message\":\"" + WebServer.esc(message) + "\"}");
                    return;
                }
            }
            WebServer.sendJson(ex, 404, error("Unknown request."));
        } catch (NumberFormatException e) {
            WebServer.sendJson(ex, 400, error("Please choose a valid option."));
        } catch (IllegalArgumentException e) {
            // Validation messages from the model classes and the checks below
            WebServer.sendJson(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            WebServer.sendJson(ex, 500, error("Server error: " + e.getMessage()));
        }
    }

    // ================================================================ OWNERS

    private String addOwner(Map<String, String> f) {
        Owner owner = new Owner(0, need(f, "name", "Name"), need(f, "phone", "Mobile number"));
        for (Owner other : ownerDAO.getAll()) {
            check(!other.getPhone().equals(owner.getPhone()), "Another owner already uses this mobile number.");
        }
        check(ownerDAO.add(owner), "Could not save the owner.");
        return owner.getName() + " added with owner ID " + owner.getOwnerId() + ".";
    }

    private String updateOwner(Map<String, String> f) {
        Owner owner = ownerDAO.getById(need(f, "id", "Owner"));
        check(owner != null, "That owner no longer exists.");
        owner.setName(need(f, "name", "Name"));
        owner.setPhone(need(f, "phone", "Mobile number"));
        for (Owner other : ownerDAO.getAll()) {
            check(other.getOwnerId() == owner.getOwnerId() || !other.getPhone().equals(owner.getPhone()),
                    "Another owner already uses this mobile number.");
        }
        check(ownerDAO.update(owner), "Could not update the owner.");
        return owner.getName() + "'s details updated.";
    }

    private String deleteOwner(Map<String, String> f) {
        String id = need(f, "id", "Owner");
        Owner owner = ownerDAO.getById(id);
        check(owner != null, "That owner no longer exists.");
        // ON DELETE RESTRICT in the database is what actually blocks this
        check(ownerDAO.delete(id), owner.getName()
                + " appears in a phone's ownership history, so the record is protected and can't be deleted.");
        return owner.getName() + " deleted.";
    }

    // ================================================================ PHONES

    private String addPhone(Map<String, String> f) {
        Phone phone = new Phone(need(f, "imei", "IMEI"), need(f, "brand", "Brand"), need(f, "model", "Model"));
        check(phoneDAO.getById(phone.getImei()) == null, "This IMEI is already registered.");
        Owner owner = ownerDAO.getById(need(f, "ownerId", "First owner"));
        check(owner != null, "Choose the phone's first owner.");
        LocalDate date = dateOrToday(f, "date");
        check(ownershipDAO.registerPhoneWithFirstOwner(phone, owner.getOwnerId(), date),
                "Registration failed, so nothing was saved.");
        return phone.getBrand() + " " + phone.getModel() + " registered to " + owner.getName() + ".";
    }

    private String updatePhone(Map<String, String> f) {
        Phone phone = phoneDAO.getById(need(f, "imei", "IMEI"));
        check(phone != null, "That phone no longer exists.");
        phone.setBrand(need(f, "brand", "Brand"));
        phone.setModel(need(f, "model", "Model"));
        check(phoneDAO.update(phone), "Could not update the phone.");
        return "Phone details updated.";
    }

    private String deletePhone(Map<String, String> f) {
        Phone phone = phoneDAO.getById(need(f, "imei", "IMEI"));
        check(phone != null, "That phone no longer exists.");
        check(phoneDAO.delete(phone.getImei()), "Could not delete the phone.");
        return phone.getBrand() + " " + phone.getModel() + " deleted, along with its history.";
    }

    // ================================================================ TRANSFER (transaction)

    private String transfer(Map<String, String> f) {
        String imei = need(f, "imei", "Phone");
        check(phoneDAO.getById(imei) != null, "That phone isn't registered.");
        check(!theftDAO.hasActiveReport(imei), "This phone has an active theft report, so it can't change hands.");
        Owner newOwner = ownerDAO.getById(need(f, "ownerId", "New owner"));
        check(newOwner != null, "Choose the new owner.");
        Ownership current = ownershipDAO.getCurrent(imei);
        check(current == null || current.getOwnerId() != newOwner.getOwnerId(), "This person already owns the phone.");
        LocalDate date = dateOrToday(f, "date");
        check(current == null || !date.isBefore(current.getFromDate()),
                "The transfer date can't be before " + (current == null ? "" : current.getOwnerName())
                + " got the phone (" + (current == null ? "" : current.getFromDate()) + ").");
        check(ownershipDAO.transferOwnership(imei, newOwner.getOwnerId(), date),
                "The transfer failed and was rolled back.");
        return "Ownership moved from " + (current == null ? "nobody" : current.getOwnerName())
                + " to " + newOwner.getName() + ".";
    }

    // ================================================================ REPAIRS

    private String addRepair(Map<String, String> f) {
        String imei = need(f, "imei", "Phone");
        check(phoneDAO.getById(imei) != null, "That phone isn't registered.");
        Repair r = new Repair(0, imei, need(f, "part", "Part"), need(f, "shop", "Shop name"),
                dateOrToday(f, "date"), "Y".equals(f.get("genuine")));
        check(repairDAO.add(r), "Could not save the repair.");
        return r.getPart() + " repair recorded" + (r.isGenuine() ? "." : " as non-genuine.");
    }

    private String updateRepair(Map<String, String> f) {
        Repair r = repairDAO.getById(need(f, "id", "Repair"));
        check(r != null, "That repair record no longer exists.");
        r.setPart(need(f, "part", "Part"));
        r.setShopName(need(f, "shop", "Shop name"));
        r.setRepairDate(dateOrToday(f, "date"));
        r.setGenuine("Y".equals(f.get("genuine")));
        check(repairDAO.update(r), "Could not update the repair.");
        return "Repair record updated.";
    }

    private String deleteRepair(Map<String, String> f) {
        String id = need(f, "id", "Repair");
        check(repairDAO.getById(id) != null, "That repair record no longer exists.");
        check(repairDAO.delete(id), "Could not delete the repair.");
        return "Repair record deleted.";
    }

    // ================================================================ THEFT REPORTS

    private String reportStolen(Map<String, String> f) {
        String imei = need(f, "imei", "Phone");
        Phone phone = phoneDAO.getById(imei);
        check(phone != null, "That phone isn't registered.");
        check(!theftDAO.hasActiveReport(imei), "This phone already has an active theft report.");
        check(theftDAO.add(new TheftReport(0, imei, dateOrToday(f, "date"), TheftReport.ACTIVE)),
                "Could not file the report.");
        return phone.getBrand() + " " + phone.getModel() + " reported as stolen.";
    }

    private String markRecovered(Map<String, String> f) {
        TheftReport t = theftDAO.getById(need(f, "id", "Report"));
        check(t != null, "That report no longer exists.");
        check(t.isActive(), "This report is already marked recovered.");
        t.setStatus(TheftReport.RECOVERED);
        check(theftDAO.update(t), "Could not update the report.");
        return "Report marked as recovered.";
    }

    // ================================================================ LISTS (JSON)

    private String ownersJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Owner> owners = ownerDAO.getAll();
        for (int i = 0; i < owners.size(); i++) {
            Owner o = owners.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(o.getOwnerId())
              .append(",\"name\":\"").append(WebServer.esc(o.getName()))
              .append("\",\"phone\":\"").append(o.getPhone()).append("\"}");
        }
        return sb.append(']').toString();
    }

    private String phonesJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Phone> phones = phoneDAO.getAll();
        for (int i = 0; i < phones.size(); i++) {
            Phone p = phones.get(i);
            Ownership cur = ownershipDAO.getCurrent(p.getImei());
            if (i > 0) sb.append(',');
            sb.append("{\"imei\":\"").append(p.getImei())
              .append("\",\"brand\":\"").append(WebServer.esc(p.getBrand()))
              .append("\",\"model\":\"").append(WebServer.esc(p.getModel()))
              .append("\",\"owner\":").append(cur == null ? "null" : "\"" + WebServer.esc(cur.getOwnerName()) + "\"")
              .append(",\"ownerId\":").append(cur == null ? "null" : String.valueOf(cur.getOwnerId()))
              .append(",\"since\":").append(cur == null ? "null" : "\"" + cur.getFromDate() + "\"")
              .append(",\"stolen\":").append(theftDAO.hasActiveReport(p.getImei())).append('}');
        }
        return sb.append(']').toString();
    }

    private String repairsJson(String imei) {
        StringBuilder sb = new StringBuilder("[");
        List<Repair> repairs = repairDAO.getByImei(imei);
        for (int i = 0; i < repairs.size(); i++) {
            Repair r = repairs.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(r.getId())
              .append(",\"part\":\"").append(WebServer.esc(r.getPart()))
              .append("\",\"shop\":\"").append(WebServer.esc(r.getShopName()))
              .append("\",\"date\":\"").append(r.getRepairDate())
              .append("\",\"genuine\":").append(r.isGenuine()).append('}');
        }
        return sb.append(']').toString();
    }

    private String theftsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<TheftReport> reports = theftDAO.getAll();
        for (int i = 0; i < reports.size(); i++) {
            TheftReport t = reports.get(i);
            Phone p = phoneDAO.getById(t.getImei());
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(t.getId())
              .append(",\"imei\":\"").append(t.getImei())
              .append("\",\"device\":\"").append(p == null ? "" : WebServer.esc(p.getBrand() + " " + p.getModel()))
              .append("\",\"date\":\"").append(t.getReportDate())
              .append("\",\"status\":\"").append(t.getStatus()).append("\"}");
        }
        return sb.append(']').toString();
    }

    // ================================================================ RESET SAMPLE DATA

    private static final String[] SAMPLE_DATA = {
        "DELETE FROM theft_reports", "DELETE FROM repairs", "DELETE FROM ownerships",
        "DELETE FROM phones", "DELETE FROM owners",
        "INSERT INTO owners (owner_id, name, phone) VALUES (1,'Karthik','9840011111'),(2,'Arun','9840022222'),"
            + "(3,'Bala','9840033333'),(4,'Charan','9840044444'),(5,'Deepak','9840055555'),(6,'Priya','9840066666'),"
            + "(7,'Meena','9840077777'),(8,'Ravi','9840088888')",
        "INSERT INTO phones (imei, brand, model) VALUES ('356789012345671','Samsung','Galaxy S23'),"
            + "('356789012345672','Apple','iPhone 13'),('356789012345673','OnePlus','11'),('356789012345674','Redmi','Note 12')",
        "INSERT INTO ownerships (imei, owner_id, from_date, to_date) VALUES "
            + "('356789012345671',1,DATE_SUB(CURDATE(),INTERVAL 580 DAY),NULL),"
            + "('356789012345672',2,DATE_SUB(CURDATE(),INTERVAL 650 DAY),DATE_SUB(CURDATE(),INTERVAL 300 DAY)),"
            + "('356789012345672',3,DATE_SUB(CURDATE(),INTERVAL 300 DAY),DATE_SUB(CURDATE(),INTERVAL 215 DAY)),"
            + "('356789012345672',4,DATE_SUB(CURDATE(),INTERVAL 215 DAY),DATE_SUB(CURDATE(),INTERVAL 110 DAY)),"
            + "('356789012345672',5,DATE_SUB(CURDATE(),INTERVAL 110 DAY),NULL),"
            + "('356789012345673',6,DATE_SUB(CURDATE(),INTERVAL 900 DAY),NULL),"
            + "('356789012345674',7,DATE_SUB(CURDATE(),INTERVAL 400 DAY),NULL)",
        "INSERT INTO repairs (imei, part, shop_name, repair_date, genuine) VALUES "
            + "('356789012345671','Battery','Samsung Service Centre',DATE_SUB(CURDATE(),INTERVAL 100 DAY),'Y'),"
            + "('356789012345672','Display','Quick Fix Mobiles',DATE_SUB(CURDATE(),INTERVAL 250 DAY),'N'),"
            + "('356789012345672','Display','Cell World',DATE_SUB(CURDATE(),INTERVAL 120 DAY),'N'),"
            + "('356789012345672','Battery','iCare Repairs',DATE_SUB(CURDATE(),INTERVAL 80 DAY),'Y'),"
            + "('356789012345674','Back Panel','Mi Service Centre',DATE_SUB(CURDATE(),INTERVAL 150 DAY),'Y')",
        "INSERT INTO theft_reports (imei, report_date, status) VALUES "
            + "('356789012345673',DATE_SUB(CURDATE(),INTERVAL 21 DAY),'ACTIVE'),"
            + "('356789012345674',DATE_SUB(CURDATE(),INTERVAL 200 DAY),'RECOVERED')"
    };

    /** Restores the sample data in ONE transaction: all of it, or none of it. */
    private String resetSampleData() throws SQLException {
        Connection con = DBConnection.getConnection();
        con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {
            for (String sql : SAMPLE_DATA) {
                st.executeUpdate(sql);
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
        return "Sample data restored: 4 phones and 8 owners.";
    }

    // ================================================================ HELPERS

    private static String need(Map<String, String> f, String key, String label) {
        String v = f.getOrDefault(key, "").trim();
        if (v.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return v;
    }

    private static LocalDate dateOrToday(Map<String, String> f, String key) {
        String v = f.getOrDefault(key, "").trim();
        if (v.isEmpty()) return LocalDate.now();
        LocalDate d;
        try {
            d = LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates use the format yyyy-mm-dd.");
        }
        if (d.isAfter(LocalDate.now())) throw new IllegalArgumentException("The date can't be in the future.");
        return d;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    private static String error(String message) {
        return "{\"error\":\"" + WebServer.esc(message) + "\"}";
    }
}
