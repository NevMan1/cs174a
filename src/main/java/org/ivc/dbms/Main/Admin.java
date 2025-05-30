package org.ivc.dbms.Main;

import javax.swing.*;
import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;

public class Admin implements Runnable {

    private static Admin ref = null;
    private volatile Queue<AdminCmd> cmdQueue;

    public static Admin Ref() {
        if (ref == null) ref = new Admin();
        return ref;
    }

    private Admin() {
        cmdQueue = new LinkedList<>();
    }

    public void inputCommand(AdminCmd c) {
        cmdQueue.add(c);
    }

    @Override
    public void run() {
        System.out.println("Admin Controller - Running...");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(50);
                while (!cmdQueue.isEmpty()) {
                    System.out.println("Admin Controller - Executing command...");
                    cmdQueue.remove().execute();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Admin Controller - Terminated.");
        }
    }

    public interface AdminCmd {
        void execute() throws SQLException;
    }

    public static class AddStudentToCourse implements AdminCmd {
        private final String perm, enrollmentcode, year, quarter;

        public AddStudentToCourse(String perm, String enrollmentcode, String year, String quarter) {
            this.perm = perm;
            this.enrollmentcode = enrollmentcode;
            this.year = year;
            this.quarter = quarter;
        }

        @Override
        public void execute() throws SQLException {
            if (Database.conn == null || Database.conn.isClosed()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "❌ Database connection error.", "Error", JOptionPane.ERROR_MESSAGE));
                return;
            }

            // Step 1: Check if already enrolled
            String checkSql = """
                SELECT 1 FROM TEST_ENROLLMENTS
                WHERE TRIM(PERM) = ? AND TRIM(ENROLLMENT_CODE) = ? AND TRIM(QUARTER) = ? AND TRIM(YEAR) = ?
            """;
            PreparedStatement checkStmt = Database.conn.prepareStatement(checkSql);
            checkStmt.setString(1, perm.trim());
            checkStmt.setString(2, enrollmentcode.trim());
            checkStmt.setString(3, quarter.trim());
            checkStmt.setString(4, year.trim());

            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "⚠️ Student is already enrolled in this course.", "Duplicate Enrollment", JOptionPane.WARNING_MESSAGE));
                return;
            }

            // Step 2: Check max capacity
            String capacitySql = """
                SELECT COUNT(*) AS CURRENT_COUNT
                FROM TEST_ENROLLMENTS
                WHERE ENROLLMENT_CODE = ? AND QUARTER = ? AND YEAR = ?
            """;
            PreparedStatement capStmt = Database.conn.prepareStatement(capacitySql);
            capStmt.setString(1, enrollmentcode);
            capStmt.setString(2, quarter);
            capStmt.setString(3, year);

            ResultSet capRs = capStmt.executeQuery();
            int current = capRs.next() ? capRs.getInt("CURRENT_COUNT") : 0;

            String maxSql = "SELECT MAX_ENROLLMENT FROM TEST_COURSE_OFFERINGS WHERE ENROLLMENT_CODE = ?";
            PreparedStatement maxStmt = Database.conn.prepareStatement(maxSql);
            maxStmt.setString(1, enrollmentcode);
            ResultSet maxRs = maxStmt.executeQuery();
            int max = maxRs.next() ? maxRs.getInt(1) : -1;

            if (max != -1 && current >= max) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "❌ Course is full (" + current + "/" + max + ")", "Enrollment Full", JOptionPane.WARNING_MESSAGE));
                return;
            }

            // Step 3: Insert enrollment
            String insertSql = """
                INSERT INTO TEST_ENROLLMENTS (PERM, ENROLLMENT_CODE, GRADE, QUARTER, YEAR)
                VALUES (?, ?, NULL, ?, ?)
            """;
            PreparedStatement ps = Database.conn.prepareStatement(insertSql);
            ps.setString(1, perm);
            ps.setString(2, enrollmentcode);
            ps.setString(3, quarter);
            ps.setString(4, year);
            ps.executeUpdate();

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "✅ Student " + perm + " successfully added to course.", "Enrollment Success", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    public static class DropStudentFromCourse implements AdminCmd {
        private final String perm, enrollmentcode, year, quarter;

        public DropStudentFromCourse(String perm, String enrollmentcode, String year, String quarter) {
            this.perm = perm;
            this.enrollmentcode = enrollmentcode;
            this.year = year;
            this.quarter = quarter;
        }

        @Override
        public void execute() throws SQLException {
            if (Database.conn == null || Database.conn.isClosed()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "❌ Database connection error.", "Error", JOptionPane.ERROR_MESSAGE));
                return;
            }

            String dropSql = """
                DELETE FROM TEST_ENROLLMENTS
                WHERE TRIM(PERM) = ? AND TRIM(ENROLLMENT_CODE) = ? AND TRIM(QUARTER) = ? AND TRIM(YEAR) = ?
            """;
            PreparedStatement ps = Database.conn.prepareStatement(dropSql);
            ps.setString(1, perm.trim());
            ps.setString(2, enrollmentcode.trim());
            ps.setString(3, quarter.trim());
            ps.setString(4, year.trim());
            int rows = ps.executeUpdate();

            String msg = (rows > 0)
                ? "✅ Student " + perm + " successfully dropped from course."
                : "⚠️ No matching enrollment found.";

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg, "Drop Status",
                rows > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE));
        }
    }

    public static class ListCoursesTaken implements AdminCmd {
        private final String perm;
        public ListCoursesTaken(String perm) {
            this.perm = perm;
        }

        public void execute() throws SQLException {
            StringBuilder sb = new StringBuilder();
            String sql = """
                SELECT c.course_number, c.title, o.quarter, o.year, e.grade
                FROM test_enrollments e
                JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
                                            AND e.year = o.year AND e.quarter = o.quarter
                JOIN test_courses c ON o.course_number = c.course_number
                WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
                ORDER BY o.year DESC,
                        CASE o.quarter 
                            WHEN 'F' THEN 3 
                            WHEN 'S' THEN 2 
                            WHEN 'W' THEN 1 
                            ELSE 0
                        END DESC
            """;

            try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
                stmt.setString(1, perm.trim());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    sb.append(rs.getString("course_number"))
                    .append(" - ")
                    .append(rs.getString("title"))
                    .append(" (")
                    .append(rs.getString("quarter"))
                    .append(" ")
                    .append(rs.getInt("year"))
                    .append("): ")
                    .append(rs.getString("grade"))
                    .append("\n");
                }
            }

            if (sb.length() == 0) {
                JOptionPane.showMessageDialog(null, "No graded courses found for " + perm + ".", "Courses Taken", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, sb.toString(), "Courses Taken for " + perm, JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public static class ClassList implements AdminCmd {
        private final String courseNumber, year, quarter;

        public ClassList(String courseNumber, String year, String quarter) {
            this.courseNumber = courseNumber;
            this.year = year;
            this.quarter = quarter;
        }

        @Override
        public void execute() throws SQLException {
            String sql = """
                SELECT DISTINCT S.PERM, S.NAME
                FROM TEST_ENROLLMENTS E
                JOIN TEST_COURSE_OFFERINGS O ON E.ENROLLMENT_CODE = O.ENROLLMENT_CODE
                JOIN TEST_STUDENTS S ON E.PERM = S.PERM
                WHERE O.COURSE_NUMBER = ? AND E.YEAR = ? AND E.QUARTER = ?
            """;
            PreparedStatement ps = Database.conn.prepareStatement(sql);
            ps.setString(1, courseNumber);
            ps.setString(2, year);
            ps.setString(3, quarter);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("Class list for ").append(courseNumber).append(":\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("PERM")).append(": ").append(rs.getString("NAME")).append("\n");
            }
            JOptionPane.showMessageDialog(null, sb.toString(), "Class List", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static class ListGradesLastQuarter implements AdminCmd {
        private final String perm;

        public ListGradesLastQuarter(String perm) {
            this.perm = perm;
        }

        @Override
        public void execute() throws SQLException {
            int latestYear = -1;
            String latestQuarter = null;

            // Step 1: Get latest graded quarter for this student
            String personalSql = """
                SELECT o.year, o.quarter
                FROM test_enrollments e
                JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
                                            AND e.year = o.year AND e.quarter = o.quarter
                WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
                ORDER BY o.year DESC,
                        CASE o.quarter 
                            WHEN 'F' THEN 3 
                            WHEN 'S' THEN 2 
                            WHEN 'W' THEN 1 
                            ELSE 0
                        END DESC
                FETCH FIRST 1 ROWS ONLY
            """;

            try (PreparedStatement stmt = Database.conn.prepareStatement(personalSql)) {
                stmt.setString(1, perm.trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    latestYear = rs.getInt("year");
                    latestQuarter = rs.getString("quarter");
                }
            }

            // Step 2: Fallback to system-wide latest graded quarter
            if (latestQuarter == null) {
                String globalSql = """
                    SELECT o.year, o.quarter
                    FROM test_enrollments e
                    JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
                                                AND e.year = o.year AND e.quarter = o.quarter
                    WHERE e.grade IS NOT NULL
                    ORDER BY o.year DESC,
                            CASE o.quarter 
                                WHEN 'F' THEN 3 
                                WHEN 'S' THEN 2 
                                WHEN 'W' THEN 1 
                                ELSE 0
                            END DESC
                    FETCH FIRST 1 ROWS ONLY
                """;
                try (PreparedStatement stmt = Database.conn.prepareStatement(globalSql);
                    ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        latestYear = rs.getInt("year");
                        latestQuarter = rs.getString("quarter");
                    } else {
                        JOptionPane.showMessageDialog(null, "No grades found.", "Grades", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
            }

            // Step 3: Fetch grades for that quarter
            String sql = """
                SELECT c.course_number, c.title, o.year, o.quarter, e.grade
                FROM test_enrollments e
                JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
                                            AND e.year = o.year AND e.quarter = o.quarter
                JOIN test_courses c ON o.course_number = c.course_number
                WHERE TRIM(e.perm) = ? AND o.year = ? AND o.quarter = ? AND e.grade IS NOT NULL
            """;

            StringBuilder sb = new StringBuilder("📋 Grades for " + perm + " in " + latestQuarter + " " + latestYear + ":\n");
            boolean found = false;
            try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
                stmt.setString(1, perm.trim());
                stmt.setInt(2, latestYear);
                stmt.setString(3, latestQuarter);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    found = true;
                    sb.append("- ")
                    .append(rs.getString("course_number"))
                    .append(" (")
                    .append(rs.getString("title"))
                    .append("): ")
                    .append(rs.getString("grade"))
                    .append("\n");
                }
            }

            if (!found) sb.append("No grades for this quarter.");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, sb.toString(), "Last Quarter Grades", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    public static class PrintTranscript implements AdminCmd {
        private final String perm;

        public PrintTranscript(String perm) {
            this.perm = perm;
        }

        public void execute() throws SQLException {
            String sql = """
                SELECT c.course_number, c.title, o.quarter, o.year, e.grade
                FROM test_enrollments e
                JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND o.year = e.year AND o.quarter = e.quarter
                JOIN test_courses c ON o.course_number = c.course_number
                WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
                ORDER BY o.year DESC,
                        CASE o.quarter 
                            WHEN 'F' THEN 3 
                            WHEN 'S' THEN 2 
                            WHEN 'W' THEN 1 
                            ELSE 0
                        END DESC
            """;

            StringBuilder sb = new StringBuilder("Transcript for " + perm + ":\n");

            try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
                stmt.setString(1, perm.trim());
                ResultSet rs = stmt.executeQuery();

                boolean hasResults = false;
                while (rs.next()) {
                    hasResults = true;
                    sb.append(rs.getString("course_number"))
                    .append(" - ")
                    .append(rs.getString("title"))
                    .append(" (")
                    .append(rs.getString("quarter"))
                    .append(" ")
                    .append(rs.getInt("year"))
                    .append("): ")
                    .append(rs.getString("grade"))
                    .append("\n");
                }

                if (!hasResults) {
                    sb.append("No graded courses found.");
                }

                System.out.println(sb.toString()); // print to console
            }
        }
    }

    public static class GradeMailerAllStudents implements AdminCmd {
        private final String quarter;
        private final String year;

        public GradeMailerAllStudents(String quarter, String year) {
            this.quarter = quarter;
            this.year = year;
        }

        @Override
        public void execute() throws SQLException {
            String sql = """
                SELECT S.NAME, S.PERM, O.COURSE_NUMBER, E.GRADE
                FROM TEST_ENROLLMENTS E
                JOIN TEST_COURSE_OFFERINGS O ON E.ENROLLMENT_CODE = O.ENROLLMENT_CODE
                JOIN TEST_STUDENTS S ON E.PERM = S.PERM
                WHERE E.QUARTER = ? AND E.YEAR = ? AND E.GRADE IS NOT NULL
                ORDER BY S.PERM, O.COURSE_NUMBER
            """;

            PreparedStatement ps = Database.conn.prepareStatement(sql);
            ps.setString(1, quarter);
            ps.setString(2, year);
            ResultSet rs = ps.executeQuery();

            StringBuilder result = new StringBuilder();
            String currentPerm = "";
            String currentName = "";
            StringBuilder grades = new StringBuilder();

            while (rs.next()) {
                String perm = rs.getString("PERM");
                String name = rs.getString("NAME");
                String course = rs.getString("COURSE_NUMBER");
                String grade = rs.getString("GRADE");

                if (!perm.equals(currentPerm)) {
                    if (!currentPerm.isEmpty()) {
                        result.append(currentName).append(", your grades are: ")
                            .append(grades.toString(), 0, grades.length() - 2)  // remove trailing comma+space
                            .append(".\n\n");
                        grades.setLength(0);
                    }
                    currentPerm = perm;
                    currentName = name;
                }

                grades.append(course).append(" is ").append(grade).append(", ");
            }

            if (grades.length() > 0) {
                result.append(currentName).append(", your grades are: ")
                    .append(grades.toString(), 0, grades.length() - 2)
                    .append(".");
            }

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                result.length() > 0 ? result.toString() : "No graded enrollments found.",
                "Grade Mailer",
                JOptionPane.INFORMATION_MESSAGE
            ));
        }
    }

    public static class EnterGradesFromFile implements AdminCmd {
        private final String filename;

        public EnterGradesFromFile(String filename) {
            this.filename = filename;
        }

        @Override
        public void execute() throws SQLException {
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                String courseCode = null, quarter = null;
                int year = -1;

                // Step 1: Find course code
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("course code:")) {
                        // Skip blank lines to find the actual course code
                        while ((line = reader.readLine()) != null && line.trim().isEmpty());
                        if (line != null) courseCode = line.trim();
                    }
                    if (courseCode != null) break;
                }

                // Step 2: Find course quarter
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("course quarter:")) {
                        while ((line = reader.readLine()) != null && line.trim().isEmpty());
                        if (line != null) {
                            String q = line.trim().toUpperCase(); // e.g., W25, F24
                            if (q.length() != 3)
                                throw new IllegalArgumentException("Invalid quarter format: " + q);
                            switch (q.charAt(0)) {
                                case 'W' -> quarter = "W";
                                case 'S' -> quarter = "S";
                                case 'F' -> quarter = "F";
                                default -> throw new IllegalArgumentException("Invalid quarter code: " + q.charAt(0));
                            }
                            year = 2000 + Integer.parseInt(q.substring(1));
                        }
                    }
                    if (quarter != null && year != -1) break;
                }

                // Step 3: Skip lines until we reach the PERM GRADE header
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (line.startsWith("perm")) break;
                }

                if (courseCode == null || quarter == null || year == -1) {
                    throw new IllegalArgumentException("Missing course information in file.");
                }

                // Step 4: Process grades
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        String perm = parts[0].trim();
                        String grade = parts[1].trim();

                        String sql = """
                            UPDATE TEST_ENROLLMENTS
                            SET GRADE = ?
                            WHERE PERM = ? AND ENROLLMENT_CODE IN (
                                SELECT ENROLLMENT_CODE FROM TEST_COURSE_OFFERINGS
                                WHERE COURSE_NUMBER = ? AND QUARTER = ? AND YEAR = ?
                            )
                        """;

                        PreparedStatement ps = Database.conn.prepareStatement(sql);
                        ps.setString(1, grade);
                        ps.setString(2, perm);
                        ps.setString(3, courseCode);
                        ps.setString(4, quarter);
                        ps.setInt(5, year);
                        ps.executeUpdate();
                    }
                }

                String msg = "📤 Grades successfully entered from " + filename;
                System.out.println(msg);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null, msg, "Grades Uploaded", JOptionPane.INFORMATION_MESSAGE
                ));

            } catch (IOException | SQLException | IllegalArgumentException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null, "❌ Error processing file: " + e.getMessage(),
                    "Upload Failed", JOptionPane.ERROR_MESSAGE
                ));
            }
        }
    }
}
