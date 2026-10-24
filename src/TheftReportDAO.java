import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the 'theft_reports' table. Full CRUD + helpers used by the app.
 */
public class TheftReportDAO implements CrudOperations<TheftReport> {

    private static final String SELECT_COLUMNS =
            "SELECT id, imei, report_date, status FROM theft_reports ";

    @Override
    public boolean add(TheftReport t) {
        String sql = "INSERT INTO theft_reports (imei, report_date, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getImei());
            ps.setDate(2, Date.valueOf(t.getReportDate()));
            ps.setString(3, t.getStatus());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public TheftReport getById(String id) {
        String sql = SELECT_COLUMNS + "WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Report ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<TheftReport> getAll() {
        return runListQuery(SELECT_COLUMNS + "ORDER BY report_date DESC", null);
    }

    public List<TheftReport> getByImei(String imei) {
        return runListQuery(SELECT_COLUMNS + "WHERE imei = ? ORDER BY report_date", imei);
    }

    /** True if this phone currently has an ACTIVE theft report. */
    public boolean hasActiveReport(String imei) {
        String sql = "SELECT COUNT(*) FROM theft_reports WHERE imei = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, imei);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean update(TheftReport t) {
        String sql = "UPDATE theft_reports SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getStatus());
            ps.setInt(2, t.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM theft_reports WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeUpdate() == 1;
        } catch (NumberFormatException e) {
            System.out.println("Report ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    private List<TheftReport> runListQuery(String sql, String imei) {
        List<TheftReport> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (imei != null) {
                ps.setString(1, imei);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    private TheftReport mapRow(ResultSet rs) throws SQLException {
        return new TheftReport(
                rs.getInt("id"),
                rs.getString("imei"),
                rs.getDate("report_date").toLocalDate(),
                rs.getString("status"));
    }
}
