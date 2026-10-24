import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the 'ownerships' table.
 *
 * This class deliberately does NOT implement CrudOperations: ownership
 * history is an audit trail, so rows are never edited or deleted directly.
 * The only way history changes is through the two TRANSACTIONS below.
 */
public class OwnershipDAO {

    private final PhoneDAO phoneDAO = new PhoneDAO();

    /**
     * TRANSACTION 1: register a new phone together with its first owner.
     * Either both rows are saved, or neither is.
     */
    public boolean registerPhoneWithFirstOwner(Phone phone, int ownerId, LocalDate fromDate) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);                    // start transaction

            if (!phoneDAO.add(phone)) {                  // same shared connection
                con.rollback();
                return false;
            }
            insertOwnership(con, phone.getImei(), ownerId, fromDate);

            con.commit();                                // save both together
            return true;
        } catch (SQLException e) {
            rollback(con);
            System.out.println("Registration failed, nothing was saved: " + e.getMessage());
            return false;
        } finally {
            resetAutoCommit(con);
        }
    }

    /**
     * TRANSACTION 2: transfer ownership.
     * Step 1 closes the current owner's record, step 2 adds the new owner.
     * Commit only if both succeed -> a phone never has two owners or zero owners.
     */
    public boolean transferOwnership(String imei, int newOwnerId, LocalDate transferDate) {
        String closeSql = "UPDATE ownerships SET to_date = ? WHERE imei = ? AND to_date IS NULL";
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);                    // start transaction

            try (PreparedStatement ps = con.prepareStatement(closeSql)) {
                ps.setDate(1, Date.valueOf(transferDate));
                ps.setString(2, imei);
                if (ps.executeUpdate() != 1) {
                    con.rollback();
                    System.out.println("Transfer failed: no current owner found for this phone.");
                    return false;
                }
            }
            insertOwnership(con, imei, newOwnerId, transferDate);

            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            System.out.println("Transfer failed and was rolled back: " + e.getMessage());
            System.out.println("(Tip: the transfer date cannot be before the current owner's start date.)");
            return false;
        } finally {
            resetAutoCommit(con);
        }
    }

    /** Full ownership history of a phone, oldest first. Uses a JOIN with owners. */
    public List<Ownership> getByImei(String imei) {
        List<Ownership> list = new ArrayList<>();
        String sql = "SELECT o.id, o.imei, o.owner_id, w.name, w.phone, o.from_date, o.to_date "
                   + "FROM ownerships o JOIN owners w ON o.owner_id = w.owner_id "
                   + "WHERE o.imei = ? ORDER BY o.from_date";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, imei);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date to = rs.getDate("to_date");
                    list.add(new Ownership(
                            rs.getInt("id"),
                            rs.getString("imei"),
                            rs.getInt("owner_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getDate("from_date").toLocalDate(),
                            to == null ? null : to.toLocalDate()));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    /** The current owner of a phone, or null. */
    public Ownership getCurrent(String imei) {
        for (Ownership o : getByImei(imei)) {
            if (o.isCurrent()) {
                return o;
            }
        }
        return null;
    }

    // ---------- private helpers ----------

    private void insertOwnership(Connection con, String imei, int ownerId, LocalDate fromDate)
            throws SQLException {
        String sql = "INSERT INTO ownerships (imei, owner_id, from_date, to_date) VALUES (?, ?, ?, NULL)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ps.setInt(2, ownerId);
            ps.setDate(3, Date.valueOf(fromDate));
            ps.executeUpdate();
        }
    }

    private void rollback(Connection con) {
        try {
            if (con != null) con.rollback();
        } catch (SQLException e) {
            System.out.println("Rollback failed: " + e.getMessage());
        }
    }

    private void resetAutoCommit(Connection con) {
        try {
            if (con != null) con.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println("Could not reset auto-commit: " + e.getMessage());
        }
    }
}
