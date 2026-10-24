import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the 'repairs' table. Full CRUD + a search by IMEI.
 */
public class RepairDAO implements CrudOperations<Repair> {

    private static final String SELECT_COLUMNS =
            "SELECT id, imei, part, shop_name, repair_date, genuine FROM repairs ";

    @Override
    public boolean add(Repair r) {
        String sql = "INSERT INTO repairs (imei, part, shop_name, repair_date, genuine) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getImei());
            ps.setString(2, r.getPart());
            ps.setString(3, r.getShopName());
            ps.setDate(4, Date.valueOf(r.getRepairDate()));
            ps.setString(5, r.isGenuine() ? "Y" : "N");
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Repair getById(String id) {
        String sql = SELECT_COLUMNS + "WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Repair ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Repair> getAll() {
        return runListQuery(SELECT_COLUMNS + "ORDER BY repair_date", null);
    }

    /** All repairs of one phone, oldest first. */
    public List<Repair> getByImei(String imei) {
        return runListQuery(SELECT_COLUMNS + "WHERE imei = ? ORDER BY repair_date", imei);
    }

    @Override
    public boolean update(Repair r) {
        String sql = "UPDATE repairs SET part = ?, shop_name = ?, repair_date = ?, genuine = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getPart());
            ps.setString(2, r.getShopName());
            ps.setDate(3, Date.valueOf(r.getRepairDate()));
            ps.setString(4, r.isGenuine() ? "Y" : "N");
            ps.setInt(5, r.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM repairs WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeUpdate() == 1;
        } catch (NumberFormatException e) {
            System.out.println("Repair ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    private List<Repair> runListQuery(String sql, String imei) {
        List<Repair> list = new ArrayList<>();
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

    private Repair mapRow(ResultSet rs) throws SQLException {
        return new Repair(
                rs.getInt("id"),
                rs.getString("imei"),
                rs.getString("part"),
                rs.getString("shop_name"),
                rs.getDate("repair_date").toLocalDate(),
                "Y".equals(rs.getString("genuine")));
    }
}
