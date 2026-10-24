import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DATABASE CONNECTIVITY
 * Creates ONE database connection and shares it with the whole application
 * (Singleton idea). All DAO classes ask this class for the connection, so the
 * connection details live in exactly one place.
 *
 * The phonetrace user is created automatically by sql/schema.sql.
 */
public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/phonetrace";
    private static final String USER     = "phonetrace";
    private static final String PASSWORD = "phonetrace123";

    private static Connection connection;   // the single shared connection

    // Private constructor: nobody can create a DBConnection object
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Error while closing connection: " + e.getMessage());
        }
    }
}
