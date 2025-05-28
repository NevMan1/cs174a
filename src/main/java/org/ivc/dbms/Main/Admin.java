package org.ivc.dbms.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class Admin implements Runnable {

    private static Admin ref = null;

    private volatile Queue<AdminCmd> cmdQueue;

    /**
     * Singleton reference accessor
     */
    public static Admin Ref() {
        if (ref == null) ref = new Admin();
        return ref;
    }

    private Admin() {
        cmdQueue = new LinkedList<>();
    }

    /**
     * Add a command to the queue
     */
    public void inputCommand(AdminCmd c) {
        cmdQueue.add(c);
    }

    /**
     * Run thread loop that executes commands from the queue
     */
    @Override
    public void run() {
        System.out.println("Admin Controller - Running...");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(50);
                    while (!cmdQueue.isEmpty()) {
                        System.out.println("Admin Controller - Executing command...");
                        cmdQueue.remove().execute();
                    }
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Admin Controller - Terminated.");
        }
    }

    // =====================================================================
    // Command Interface
    // =====================================================================
    public interface AdminCmd {
        void execute() throws SQLException;
    }

    // =====================================================================
    // Command Implementations
    // =====================================================================

    /**
     * Add a student to a course
     */
    public static class AddAdminToCourse implements AdminCmd {
        private final String perm;
        private final String course;
        private final String year;
        private final String quarter;

        public AddAdminToCourse(String perm, String course, String year, String quarter) {
            this.perm = perm;
            this.course = course;
            this.year = year;
            this.quarter = quarter;
        }

        @Override
        public void execute() throws SQLException {
            String sql = "INSERT INTO COURSE (ENROLLMENT_CODE, COURSE_NUMBER, YEAR, QUARTER, TITLE, " +
                     "FIRST_NAME_PROFESSOR, LAST_NAME_PROF, LOCATION_BUILDING, LOCATION_ROOM, MAX_ENROLL) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            pstmt.setString(1, "CS174");
            pstmt.setString(2, "12345");
            pstmt.setInt(3, 25);
            pstmt.setString(4, "Spring");
            pstmt.setString(5, "CHem123");
            pstmt.setString(6, "NONON");
            pstmt.setString(7, "dsfnakjds");
            pstmt.setString(8, "fdsf");
            pstmt.setInt(9, 10);
            pstmt.setInt(10, 10);
            pstmt.executeUpdate();
            System.out.println("✅ Added student " + perm + " to course " + course);
        }
    }



    /**
     * Drop a student from a course
     */
    public static class DropAdminFromCourse implements AdminCmd {
        private final String perm;
        private final String course;

        public DropAdminFromCourse(String perm, String course) {
            this.perm = perm;
            this.course = course;
        }

        @Override
        public void execute() throws SQLException {
            String sql = "DELETE FROM Enrollment WHERE perm = ? AND course = ?";
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            pstmt.setString(1, perm);
            pstmt.setString(2, course);
            pstmt.executeUpdate();
            System.out.println("🗑️ Dropped student " + perm + " from course " + course);
        }
    }

    /**
     * List all courses (not filtered by perm) and return the result to the GUI
     */
    public static class ListStudentCourses implements AdminCmd {
        private final Consumer<String> callback;

        public ListStudentCourses(Consumer<String> callback) {
            this.callback = callback;
        }

        @Override
        public void execute() throws SQLException {
            System.out.println("🔍 ListStudentCourses command is running...");
            String sql = "SELECT * FROM COURSE";
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            StringBuilder result = new StringBuilder("📋 All Courses:\n");
            boolean found = false;

            while (rs.next()) {
                found = true;
                result.append("- ")
                      .append(rs.getString("Title"))   // adjust to match your table schema
                      .append(" (")
                      .append(rs.getString("Quarter"))
                      .append(" ")
                      .append(rs.getString("Year"))
                      .append(")\n");
            }

            if (!found) {
                result.append("No courses found.");
            }

            callback.accept(result.toString());
        }
    }
}
