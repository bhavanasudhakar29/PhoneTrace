import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) for the 'phones' table.
 * Implements the CrudOperations interface -> full CRUD.
 * Every query uses PreparedStatement, which prevents SQL injection.
 */
public class PhoneDAO implements CrudOperations<Phone> {

    @Override
    public boolean add(Phone phone) {
        String sql = "INSERT INTO phones (imei, brand, model) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, phone.getImei());
            ps.setString(2, phone.getBrand());
            ps.setString(3, phone.getModel());
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("A phone with this IMEI is already registered.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Phone getById(String imei) {
        String sql = "SELECT imei, brand, model FROM phones WHERE imei = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, imei);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Phone> getAll() {
        List<Phone> list = new ArrayList<>();
        String sql = "SELECT imei, brand, model FROM phones ORDER BY brand, model";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean update(Phone phone) {
        String sql = "UPDATE phones SET brand = ?, model = ? WHERE imei = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, phone.getBrand());
            ps.setString(2, phone.getModel());
            ps.setString(3, phone.getImei());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String imei) {
        // ON DELETE CASCADE removes this phone's ownerships, repairs and theft reports too
        String sql = "DELETE FROM phones WHERE imei = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, imei);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // Converts one database row into a Phone object
    private Phone mapRow(ResultSet rs) throws SQLException {
        return new Phone(rs.getString("imei"), rs.getString("brand"), rs.getString("model"));
    }
}
