import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.mindrot.jbcrypt.BCrypt;

public class FixAdmin {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String hash = BCrypt.hashpw("admin", BCrypt.gensalt(10));

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:store.db")) {
            try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE users SET password = ? WHERE username = 'admin'")) {
                stmt.setString(1, hash);
                int rows = stmt.executeUpdate();
                System.out.println("Updated admin rows: " + rows);
            }
            System.out.println("NEW_HASH=" + hash);
        }
    }
}
