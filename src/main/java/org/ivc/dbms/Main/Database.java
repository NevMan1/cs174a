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

    public static Connection conn;
    public static Statement stmt;

    public static String loggedInPerm = null;  // Changed to String for CHAR(7) consistency

    private static final String dbUsername = "ADMIN";
    private static final String dbPassword = "";
    private static final String strConn = "jdbc:oracle:thin:@winnev1_tp?TNS_ADMIN=/Users/legitbrunogmail.com/Documents/Wallet_winnev1";

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
            StudentGUI gui = new StudentGUI();
            gui.setVisible(true);
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
        while (true) {
            JTextField permField = new JTextField();
            JPasswordField pinField = new JPasswordField();
            Object[] fields = {
                "Perm Number (7 characters):", permField,
                "PIN (4 digits):", pinField
            };

            int option = JOptionPane.showConfirmDialog(
                null, fields, "Login to Registrar System", JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) {
                return false;
            }

            try {
                String perm = permField.getText().trim();
                String pin = new String(pinField.getPassword()).trim();

                if (validateCredentials(perm, pin)) {
                    loggedInPerm = perm;
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Invalid Perm Number or PIN. Try again.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "❌ Invalid input: PIN must be numbers only.");
            }
        }
    }

    public static boolean validateCredentials(String perm, String pin) {
        try {
            String sql = "SELECT * FROM test_students WHERE TRIM(perm) = ? AND TRIM(pin) = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, perm);
            pstmt.setString(2, pin);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();  // returns true if a row is found
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
