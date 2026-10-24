import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the 'owners' table. Full CRUD via the CrudOperations interface.
 */
public class OwnerDAO implements CrudOperations<Owner> {

    @Override
    public boolean add(Owner owner) {
        String sql = "INSERT INTO owners (name, phone) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.getName());
            ps.setString(2, owner.getPhone());
            if (ps.executeUpdate() == 1) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        owner.setOwnerId(keys.getInt(1));   // store the auto-generated ID
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("An owner with this mobile number already exists.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Owner getById(String id) {
        String sql = "SELECT owner_id, name, phone FROM owners WHERE owner_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Owner ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Owner> getAll() {
        List<Owner> list = new ArrayList<>();
        String sql = "SELECT owner_id, name, phone FROM owners ORDER BY owner_id";
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
    public boolean update(Owner owner) {
        String sql = "UPDATE owners SET name = ?, phone = ? WHERE owner_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, owner.getName());
            ps.setString(2, owner.getPhone());
            ps.setInt(3, owner.getOwnerId());
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Another owner already uses this mobile number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM owners WHERE owner_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            // ON DELETE RESTRICT protects the history
            System.out.println("Cannot delete: this owner appears in a phone's ownership history.");
            System.out.println("History records are protected so the trail can never be erased.");
        } catch (NumberFormatException e) {
            System.out.println("Owner ID must be a number.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    private Owner mapRow(ResultSet rs) throws SQLException {
        return new Owner(rs.getInt("owner_id"), rs.getString("name"), rs.getString("phone"));
    }
}
