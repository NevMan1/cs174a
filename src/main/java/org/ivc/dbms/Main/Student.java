package org.ivc.dbms.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Student {
    public static class CourseOfferingInfo {
        public String courseNumber;
        public String title;
        public int maxEnrollment;
        public int enrolledCount;
        public String instructorName;
        public String timeSlot;

        public int availableSpots() {
            return maxEnrollment - enrolledCount;
        }
    }

    public static CourseOfferingInfo getOfferingInfo(String courseNumber, int year, String quarter) throws SQLException {
    String sql = """
            SELECT c.course_number, c.title,
                o.professor_first, o.professor_last, o.time_slot,
                o.max_enrollment, COUNT(e.perm) AS enrolled
            FROM test_course_offerings o
            JOIN test_courses c ON o.course_number = c.course_number
            LEFT JOIN test_enrollments e ON o.enrollment_code = e.enrollment_code
            WHERE o.course_number = ? AND o.year = ? AND o.quarter = ?
            GROUP BY c.course_number, c.title, o.professor_first, o.professor_last, o.time_slot, o.max_enrollment
            FETCH FIRST 1 ROWS ONLY
        """;


    try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
        stmt.setString(1, courseNumber);
        stmt.setInt(2, year);
        stmt.setString(3, quarter);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            CourseOfferingInfo info = new CourseOfferingInfo();
            info.courseNumber = rs.getString("course_number");
            info.title = rs.getString("title");

            String first = rs.getString("professor_first");
            String last = rs.getString("professor_last");
            info.instructorName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();

            info.timeSlot = rs.getString("time_slot");
            info.maxEnrollment = rs.getInt("max_enrollment");
            info.enrolledCount = rs.getInt("enrolled");
            return info;
        } else {
            return null;
        }
    }
}


    public static String getMajorByPerm(String perm) throws SQLException {
        String sql = "SELECT TRIM(major_name) AS major_name FROM test_students WHERE TRIM(perm) = ?";
        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, perm.trim());  // Also trim the input just in case
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("major_name").trim();  // Defensive: trim result
            } else {
                return null;
            }
        }
    }


    public static String getCoursesForMajor(String major) throws SQLException {
        String sql = """
            SELECT c.course_number, c.title
            FROM test_major_requirements r
            JOIN test_courses c ON r.course_number = c.course_number
            WHERE r.major_name = ?
        """;

        StringBuilder courses = new StringBuilder();
        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, major);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.append(rs.getString("course_number"))
                       .append(" - ")
                       .append(rs.getString("title"))
                       .append("\n");
            }
        }
        return courses.toString();
    }

    public static Map<String, String> getCoursesForMajorMap(String major) throws SQLException {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = """
            SELECT c.course_number, c.title
            FROM test_major_requirements r
            JOIN test_courses c ON r.course_number = c.course_number
            WHERE r.major_name = ?
        """;

        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, major);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("course_number"), rs.getString("title"));
            }
        }
        return map;
    }   
        public static boolean verifyCurrentPin(String perm, String currentPin) throws SQLException {
            String hashedPin = HashUtil.sha256(currentPin.trim()).toLowerCase();

            String sql = """
                SELECT 1 FROM test_students
                WHERE LOWER(TRIM(perm)) = ? AND LOWER(TRIM(pin)) = ?
            """;

            try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
                stmt.setString(1, perm.trim().toLowerCase());
                stmt.setString(2, hashedPin);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }




        public static boolean isPinAlreadyUsed(String newPin, String perm) throws SQLException {
        String hashedPin = HashUtil.sha256(newPin.trim()).toLowerCase();
        String sql = "SELECT COUNT(*) AS count FROM test_students WHERE TRIM(pin) = ? AND TRIM(perm) <> ?";
        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPin);
            stmt.setString(2, perm.trim());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("count") > 0;
        }
    }



    public static void updatePin(String perm, String newPin) throws SQLException {
        String hashedPin = HashUtil.sha256(newPin.trim());
        String sql = "UPDATE test_students SET pin = ? WHERE TRIM(perm) = ?";
        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPin);
            stmt.setString(2, perm.trim());
            stmt.executeUpdate();
        }
    }



    public static String getCurrentCourses(String perm) throws SQLException {
    StringBuilder courses = new StringBuilder();
    String sql = """
        SELECT c.course_number, c.title, o.year, o.quarter
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NULL
        ORDER BY o.year DESC, 
                 CASE o.quarter 
                     WHEN 'Fall' THEN 3 
                     WHEN 'Spring' THEN 2 
                     WHEN 'Winter' THEN 1 
                 END DESC
    """;

    try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
        stmt.setString(1, perm.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            courses.append(rs.getString("course_number"))
                .append(" - ")
                .append(rs.getString("title"))
                .append(" (")
                .append(rs.getString("quarter"))
                .append(" ")
                .append(rs.getInt("year"))
                .append(")\n");
        }
    }
    return courses.toString();
}

public static Map<String, String> getCurrentCoursesMap(String perm) throws SQLException {
    Map<String, String> courseMap = new HashMap<>();
    String sql = """
        SELECT c.course_number, c.title
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NULL
        ORDER BY o.year DESC,
                 CASE o.quarter
                     WHEN 'Fall' THEN 3
                     WHEN 'Spring' THEN 2
                     WHEN 'Winter' THEN 1
                 END DESC
    """;

    try (PreparedStatement ps = Database.conn.prepareStatement(sql)) {
        ps.setString(1, perm.trim());
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courseMap.put(rs.getString("course_number"), rs.getString("title"));
            }
        }
    }
    return courseMap;
}


    // Drop a course for the student (drops the most recent enrollment for that course)
    public static void dropCourse(String perm, String courseNumber) throws SQLException {
        // Step 1: Check how many classes the student is enrolled in (grade IS NULL)
        String countSql = """
            SELECT COUNT(*) AS course_count
            FROM test_enrollments e
            JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
            WHERE TRIM(e.perm) = ? AND e.grade IS NULL
        """;

        try (PreparedStatement countStmt = Database.conn.prepareStatement(countSql)) {
            countStmt.setString(1, perm.trim());
            ResultSet rs = countStmt.executeQuery();
            if (rs.next() && rs.getInt("course_count") <= 1) {
                throw new SQLException("Cannot drop your only enrolled class.");
            }
        }

        // Step 2: Get the enrollment_code for the course
        String findSql = """
            SELECT e.enrollment_code
            FROM test_enrollments e
            JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
            WHERE TRIM(e.perm) = ? AND o.course_number = ? AND e.grade IS NULL
        """;

        String enrollmentCode = null;
        try (PreparedStatement findStmt = Database.conn.prepareStatement(findSql)) {
            findStmt.setString(1, perm.trim());
            findStmt.setString(2, courseNumber);
            ResultSet rs = findStmt.executeQuery();
            if (rs.next()) {
                enrollmentCode = rs.getString("enrollment_code");
            } else {
                throw new SQLException("You are not currently enrolled in " + courseNumber);
            }
        }

        // Step 3: Delete the enrollment
        String deleteSql = "DELETE FROM test_enrollments WHERE enrollment_code = ? AND TRIM(perm) = ?";
        try (PreparedStatement deleteStmt = Database.conn.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, enrollmentCode);
            deleteStmt.setString(2, perm.trim());
            deleteStmt.executeUpdate();
        }
    }


    public static void enrollInCourse(String perm, String courseNum, int year, String quarter) throws SQLException {
    // Step 1: Get the enrollment code for this course in the given quarter/year
    String enrollmentCode = null;
    String getCodeSql = "SELECT enrollment_code FROM test_course_offerings WHERE course_number = ? AND year = ? AND quarter = ?";
    try (PreparedStatement stmt = Database.conn.prepareStatement(getCodeSql)) {
        stmt.setString(1, courseNum.trim());
        stmt.setInt(2, year);
        stmt.setString(3, quarter.trim());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            enrollmentCode = rs.getString(1);
        } else {
            throw new SQLException("Course offering not found.");
        }
    }

    // Step 2: Check if the student is already enrolled or has passed the course before
    String checkSql = """
        SELECT e.grade
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        WHERE TRIM(e.perm) = ? AND TRIM(o.course_number) = ?
    """;
    try (PreparedStatement check = Database.conn.prepareStatement(checkSql)) {
        check.setString(1, perm.trim());
        check.setString(2, courseNum.trim());
        ResultSet rs = check.executeQuery();
        while (rs.next()) {
            String grade = rs.getString("grade");
            if (grade == null) {
                throw new SQLException("You're already enrolled in this course.");
            }

            List<String> passingGrades = List.of("C", "C+", "B", "B+", "A", "A+", "A-");
            if (passingGrades.contains(grade.toUpperCase())) {
                throw new SQLException("You already passed this course with a grade of " + grade + ".");
            }
        }
    }

    // Step 3: Insert the enrollment
    String insertSql = "INSERT INTO test_enrollments (perm, enrollment_code, grade) VALUES (?, ?, NULL)";
    try (PreparedStatement insert = Database.conn.prepareStatement(insertSql)) {
        insert.setString(1, perm.trim());
        insert.setString(2, enrollmentCode.trim());
        insert.executeUpdate();
    }
}





    // Fixed getGradesLastQuarter method for Student class

public static String getGradesLastQuarter(String perm) throws SQLException {
    StringBuilder sb = new StringBuilder();

    // Step 1: Get the most recent (year, quarter) Cindy has a non-null grade in
    String latestQuarterSql = """
        SELECT o.year, o.quarter
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
        ORDER BY o.year DESC,
                 CASE o.quarter
                     WHEN 'Winter' THEN 4
                     WHEN 'Fall' THEN 3
                     WHEN 'Summer' THEN 2
                     WHEN 'Spring' THEN 1
                     ELSE 0
                 END DESC
        FETCH FIRST 1 ROWS ONLY
    """;

    int latestYear = -1;
    String latestQuarter = null;

    try (PreparedStatement stmt = Database.conn.prepareStatement(latestQuarterSql)) {
        stmt.setString(1, perm.trim());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            latestYear = rs.getInt("year");
            latestQuarter = rs.getString("quarter");
        } else {
            return "No grades found.";
        }
    }

    // Step 2: Retrieve all courses from that quarter
    String sql = """
        SELECT c.course_number, c.title, o.year, o.quarter, e.grade
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND o.year = ? AND o.quarter = ? AND e.grade IS NOT NULL
    """;

    try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
        stmt.setString(1, perm.trim());
        stmt.setInt(2, latestYear);
        stmt.setString(3, latestQuarter);
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

    return sb.length() == 0 ? "No grades found." : sb.toString();
}



    public static String getRequirementCheck(String perm) throws SQLException {
    String major = getMajorByPerm(perm);
    if (major == null) return "❌ No major found.";

    // Get completed courses
    Set<String> completed = new HashSet<>();
    String sqlCompleted = """
        SELECT TRIM(c.course_number)
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
    """;
    try (PreparedStatement stmt = Database.conn.prepareStatement(sqlCompleted)) {
        stmt.setString(1, perm.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            completed.add(rs.getString(1).trim());
        }
    }

    // Get required and elective courses with titles
    Map<String, String> required = new LinkedHashMap<>();
    Map<String, String> electives = new LinkedHashMap<>();
    String sqlReqs = """
        SELECT TRIM(r.course_number), c.title, r.is_elective
        FROM test_major_requirements r
        JOIN test_courses c ON r.course_number = c.course_number
        WHERE TRIM(r.major_name) = ?
        ORDER BY r.is_elective, r.course_number
    """;
    try (PreparedStatement stmt = Database.conn.prepareStatement(sqlReqs)) {
        stmt.setString(1, major.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String course = rs.getString(1).trim();
            String title = rs.getString("title");
            String isElective = rs.getString("is_elective");
            if ("Y".equals(isElective)) {
                electives.put(course, title);
            } else {
                required.put(course, title);
            }
        }
    }

    // Get how many electives are required
    int numElectivesRequired = 0;
    try (PreparedStatement stmt = Database.conn.prepareStatement(
            "SELECT num_electives_required FROM test_majors WHERE TRIM(major_name) = ?")) {
        stmt.setString(1, major.trim());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            numElectivesRequired = rs.getInt(1);
        }
    }

    // Build display
    StringBuilder sb = new StringBuilder();
    sb.append("Required Courses:\n");
    for (var entry : required.entrySet()) {
        String course = entry.getKey().trim();
        String status = completed.contains(course) ? "✅" : "❌";
        sb.append(status).append(" ").append(course).append(" - ").append(entry.getValue()).append("\n");
    }

    sb.append("\nElective Courses (need ").append(numElectivesRequired).append("):\n");
    for (var entry : electives.entrySet()) {
        String course = entry.getKey().trim();
        String status = completed.contains(course) ? "✅" : "❌";
        sb.append(status).append(" ").append(course).append(" - ").append(entry.getValue()).append("\n");
    }

    return sb.toString();
}


    public static int getAvailableSpots(String courseNum, int year, String quarter) throws SQLException {
        String sql = """
            SELECT o.max_enrollment, COUNT(e.perm) AS enrolled
            FROM test_course_offerings o
            LEFT JOIN test_enrollments e ON o.enrollment_code = e.enrollment_code
            WHERE o.course_number = ? AND o.year = ? AND o.quarter = ?
            GROUP BY o.max_enrollment
        """;

        try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
            stmt.setString(1, courseNum);
            stmt.setInt(2, year);
            stmt.setString(3, quarter);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int max = rs.getInt("max_enrollment");
                int enrolled = rs.getInt("enrolled");
                return max - enrolled;
            } else {
                return -1; // unknown
            }
        }
    }

    public static Map<String, List<String>> generateOptimalPlan(String perm) throws SQLException {
    Map<String, List<String>> plan = new LinkedHashMap<>();

    // Step 1: Get all required courses
    String major = getMajorByPerm(perm);
    Set<String> requiredCourses = new HashSet<>();
    String requiredSql = "SELECT TRIM(course_number) FROM test_major_requirements WHERE TRIM(major_name) = ?";
    try (PreparedStatement ps = Database.conn.prepareStatement(requiredSql)) {
        ps.setString(1, major.trim());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            requiredCourses.add(rs.getString(1).trim());
        }
    }

    // Step 2: Get all completed courses
    Set<String> completed = new HashSet<>();
    String completedSql = """
        SELECT DISTINCT TRIM(c.course_number)
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
    """;
    try (PreparedStatement ps = Database.conn.prepareStatement(completedSql)) {
        ps.setString(1, perm.trim());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            completed.add(rs.getString(1).trim());
        }
    }

    // Step 3: Get currently enrolled courses
    Set<String> inProgress = new HashSet<>();
    String inProgressSql = """
        SELECT DISTINCT TRIM(c.course_number)
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NULL
    """;
    try (PreparedStatement ps = Database.conn.prepareStatement(inProgressSql)) {
        ps.setString(1, perm.trim());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            inProgress.add(rs.getString(1).trim());
        }
    }

    // Step 4: Get prerequisites map
    Map<String, Set<String>> prereqMap = new HashMap<>();
    String prereqSql = "SELECT TRIM(course_number), TRIM(prereq_course) FROM test_prerequisites";
    try (PreparedStatement ps = Database.conn.prepareStatement(prereqSql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String course = rs.getString(1).trim();
            String prereq = rs.getString(2).trim();
            prereqMap.computeIfAbsent(course, k -> new HashSet<>()).add(prereq);
        }
    }

    // Step 5: Get future quarters
    List<String> futureQuarters = new ArrayList<>();
    String quarterSql = "SELECT quarter_rank FROM test_quarters ORDER BY year, CASE quarter WHEN 'Winter' THEN 1 WHEN 'Spring' THEN 2 WHEN 'Fall' THEN 3 END";
    try (PreparedStatement ps = Database.conn.prepareStatement(quarterSql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            futureQuarters.add(rs.getString(1).trim());
        }
    }

    // Step 6: Get offerings
    Map<String, Set<String>> offerings = new HashMap<>();
    String offeringSql = "SELECT TRIM(course_number), year, quarter FROM test_course_offerings";
    try (PreparedStatement ps = Database.conn.prepareStatement(offeringSql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String course = rs.getString(1).trim();
            int year = rs.getInt("year");
            String quarter = rs.getString("quarter");
            String quarterRank = getQuarterRank(year, quarter);
            offerings.computeIfAbsent(quarterRank, k -> new HashSet<>()).add(course);
        }
    }

    // Step 7: Build plan
    Set<String> toTake = new HashSet<>(requiredCourses);
    toTake.removeAll(completed);
    toTake.removeAll(inProgress);
    Set<String> scheduled = new HashSet<>();
    scheduled.addAll(completed);
    scheduled.addAll(inProgress);

    for (String quarter : futureQuarters) {
        List<String> thisQuarter = new ArrayList<>();
        Set<String> offered = offerings.getOrDefault(quarter, new HashSet<>());

        for (String course : new ArrayList<>(toTake)) {
            if (thisQuarter.size() >= 5) break;
            if (!offered.contains(course)) continue;

            Set<String> prereqs = prereqMap.getOrDefault(course, Set.of());
            if (!scheduled.containsAll(prereqs)) continue;

            thisQuarter.add(course);
        }

        if (!thisQuarter.isEmpty()) {
            plan.put(quarter, new ArrayList<>(thisQuarter));
            scheduled.addAll(thisQuarter);
            toTake.removeAll(thisQuarter);
        }

        if (toTake.isEmpty()) break;
    }

    return plan;
}


private static String getQuarterRank(int year, String quarter) {
    char q = switch (quarter) {
        case "Fall" -> 'F';
        case "Winter" -> 'W';
        case "Spring" -> 'S';
        default -> '?';
    };
    return "" + q + String.valueOf(year).substring(2);
}
    public static class QuarterInfo {
        public int year;
        public String quarter;
    }

    public static QuarterInfo getMostRecentQuarter() throws SQLException {
        String sql = """
            SELECT year, quarter
            FROM test_quarters
            ORDER BY year DESC,
                CASE quarter
                    WHEN 'Fall' THEN 3
                    WHEN 'Spring' THEN 2
                    WHEN 'Winter' THEN 1
                    ELSE 0
                END DESC
            FETCH FIRST 1 ROWS ONLY
        """;

        try (PreparedStatement stmt = Database.conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                QuarterInfo q = new QuarterInfo();
                q.year = rs.getInt("year");
                q.quarter = rs.getString("quarter");
                return q;
            } else {
                return null;
            }
        }
    }





}
