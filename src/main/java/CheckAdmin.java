import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckAdmin {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:store.db");
                Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT username, password, force_password_change FROM users")) {
                while (rs.next()) {
                    System.out.println("User: " + rs.getString("username") + ", Pass: " + rs.getString("password"));
                }
            } catch (Exception e) {
                System.out.println("Error querying users: " + e.getMessage());
            }
        }

        String hash = "$2a$10$Y1rVON1Qp4uWzE1U4RHY9ebl8.6R5A0LpG2w0vj2.t9t8Wq4H9rWq";
        boolean match = org.mindrot.jbcrypt.BCrypt.checkpw("admin", hash);
        System.out.println("Does admin match hash? " + match);

        System.out.println("New hash for admin: "
                + org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt(10)));
    }
}
