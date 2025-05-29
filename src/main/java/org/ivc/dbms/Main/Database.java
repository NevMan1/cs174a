package org.ivc.dbms.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Database {
    public static boolean student = true;
    public static Connection conn;
    public static Statement stmt;

    public static String loggedInPerm = null;  // Changed to String for CHAR(7) consistency

    private static final String dbUsername = "ADMIN";
    private static final String dbPassword = "WINNEV174Adb";
    private static final String strConn = "jdbc:oracle:thin:@winnev1_tp?TNS_ADMIN=/Users/helenwang/Documents/Wallet_winnev1";

    public static void main(String[] args) {
        try {
            openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (!showLoginDialog()) {
            System.exit(0);
        }

        System.out.println("✅ Logged in as perm: " + loggedInPerm);
        System.out.println("🚀 Launching Admin thread...");
        new Thread(Admin.Ref()).start();

        SwingUtilities.invokeLater(() -> {
        StudentGUI sgui = new StudentGUI(); 
        RegistrarGUI rgui = new RegistrarGUI();
            if (student) {
                sgui.setVisible(true);
            } else {
                rgui.setVisible(true);
            }
        });
    }

    public static void openConnection() throws SQLException {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        } catch (Exception e) {
            e.printStackTrace();
        }
        conn = DriverManager.getConnection(strConn, dbUsername, dbPassword);
        stmt = conn.createStatement();
    }

    public static boolean showLoginDialog() {
        // Ask user if logging in as student or admin
        String[] roles = {"Student", "Admin"};
        int role = JOptionPane.showOptionDialog(null, "Login as:", "User Role",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                roles, roles[0]);

        if (role != 0 && role != 1) {
            JOptionPane.showMessageDialog(null, "Login cancelled.");
            return false;
        }
        student = (role == 0);

        JTextField permField = new JTextField();
        JPasswordField pinField = new JPasswordField();
        Object[] fields = {
            "Perm Number (7 characters):", permField,
            "PIN (4 digits):", pinField
        };

        int credentialsOption = JOptionPane.showConfirmDialog(
                null, fields, "Login to Registrar System", JOptionPane.OK_CANCEL_OPTION);

        if (credentialsOption != JOptionPane.OK_OPTION) {
            return false;
        }

        String perm = permField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();

        if (!validateCredentials(perm, pin)) {
            JOptionPane.showMessageDialog(null, "❌ Invalid Perm Number or PIN. Try again.");
            return false;
        }

        loggedInPerm = perm;
        return true;
    }

        public static boolean validateCredentials(String perm, String pin) {
            try {
                String hashedPin = HashUtil.sha256(pin.trim());

                System.out.println("=== DEBUG LOGIN ===");
                System.out.println("perm: [" + perm + "]");
                System.out.println("raw pin: [" + pin + "]");
                System.out.println("hashed pin: [" + hashedPin + "]");

                String sql = "SELECT pin FROM test_students WHERE TRIM(perm) = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, perm.trim());

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPin = rs.getString("pin").trim();
                    System.out.println("stored pin: [" + storedPin + "]");
                    System.out.println("match? " + storedPin.equalsIgnoreCase(hashedPin));

                }
                String sql2 = """
                    SELECT 1 FROM test_students
                    WHERE LOWER(TRIM(perm)) = ? AND LOWER(TRIM(pin)) = ?
                """;
                PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                pstmt2.setString(1, perm.trim().toLowerCase());
                pstmt2.setString(2, hashedPin.toLowerCase());

                return pstmt2.executeQuery().next();

                
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
}
