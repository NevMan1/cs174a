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
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ? AND e.grade IS NULL
        ORDER BY o.year DESC, 
                 CASE o.quarter 
                     WHEN 'F' THEN 3
                     WHEN 'S' THEN 2
                     WHEN 'W' THEN 1
                 END DESC
    """;
  

    try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
        stmt.setString(1, perm.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            courses.append(rs.getString("course_number"))
                .append(" - ")
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
                    WHEN 'F' THEN 3
                    WHEN 'S' THEN 2
                    WHEN 'W' THEN 1
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
    // Step 0: Check how many *other* courses the student is currently enrolled in
    String countSql = """
        SELECT COUNT(*) AS cnt
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
        WHERE TRIM(e.perm) = ? AND e.grade IS NULL AND TRIM(o.course_number) <> ?
    """;

    try (PreparedStatement countStmt = Database.conn.prepareStatement(countSql)) {
        countStmt.setString(1, perm.trim());
        countStmt.setString(2, courseNumber.trim());
        ResultSet countRs = countStmt.executeQuery();
        if (countRs.next() && countRs.getInt("cnt") == 0) {
            throw new SQLException("❌ You cannot drop your last enrolled course.");
        }
    }

    // Step 1: Find the enrollment entry
    String findSql = """
        SELECT e.enrollment_code, o.year, o.quarter
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
        WHERE TRIM(e.perm) = ? AND o.course_number = ? AND e.grade IS NULL
    """;

    try (PreparedStatement findStmt = Database.conn.prepareStatement(findSql)) {
        findStmt.setString(1, perm.trim());
        findStmt.setString(2, courseNumber.trim());
        ResultSet rs = findStmt.executeQuery();
        if (rs.next()) {
            String enrollmentCode = rs.getString("enrollment_code");
            int year = rs.getInt("year");
            String quarter = rs.getString("quarter");

            // Step 2: Execute the deletion
            String deleteSql = """
                DELETE FROM test_enrollments 
                WHERE enrollment_code = ? AND TRIM(perm) = ? AND year = ? AND quarter = ?
            """;
            try (PreparedStatement deleteStmt = Database.conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, enrollmentCode);
                deleteStmt.setString(2, perm.trim());
                deleteStmt.setInt(3, year);
                deleteStmt.setString(4, quarter);
                deleteStmt.executeUpdate();
            }
        } else {
            throw new SQLException("❌ You are not currently enrolled in " + courseNumber);
        }
    }
}





    public static void enrollInCourse(String perm, String courseNum, int year, String quarter) throws SQLException {
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

    // Check if the student already took or passed this course
    String checkSql = """
        SELECT e.grade
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
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
            if (List.of("C", "C+", "B-", "B", "B+", "A-", "A", "A+").contains(grade.toUpperCase())) {
                throw new SQLException("You already passed this course with a grade of " + grade + ".");
            }
        }
    }



    // ✅ Prerequisite check
    String prereqSql = "SELECT prereq_course FROM test_prerequisites WHERE course_number = ?";
    Set<String> requiredPrereqs = new HashSet<>();
    try (PreparedStatement ps = Database.conn.prepareStatement(prereqSql)) {
        ps.setString(1, courseNum.trim());
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                requiredPrereqs.add(rs.getString(1).trim());
            }
        }
    }

    if (!requiredPrereqs.isEmpty()) {
        Set<String> completedCourses = new HashSet<>();
        String completedSql = """
            SELECT DISTINCT TRIM(c.course_number)
            FROM test_enrollments e
            JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
            JOIN test_courses c ON o.course_number = c.course_number
            WHERE TRIM(e.perm) = ? AND e.grade IS NOT NULL
              AND UPPER(e.grade) IN ('C', 'C+', 'B-', 'B', 'B+', 'A-', 'A', 'A+')
        """;
        try (PreparedStatement ps = Database.conn.prepareStatement(completedSql)) {
            ps.setString(1, perm.trim());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                completedCourses.add(rs.getString(1).trim());
            }
        }

        for (String prereq : requiredPrereqs) {
            if (!completedCourses.contains(prereq)) {
                throw new SQLException("Missing prerequisite: " + prereq);
            }
        }
    }

    // Insert enrollment
    String insertSql = "INSERT INTO test_enrollments (perm, enrollment_code, year, quarter, grade) VALUES (?, ?, ?, ?, NULL)";
    try (PreparedStatement insert = Database.conn.prepareStatement(insertSql)) {
        insert.setString(1, perm.trim());
        insert.setString(2, enrollmentCode.trim());
        insert.setInt(3, year);
        insert.setString(4, quarter.trim());
        insert.executeUpdate();
    }
}







    // Fixed getGradesLastQuarter method for Student class

public static String getGradesLastQuarter(String perm) throws SQLException {
    StringBuilder sb = new StringBuilder();

    // Step 1: Try to get the latest graded quarter for this student
    int latestYear = -1;
    String latestQuarter = null;
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

    // Step 2: If student has no grades, fallback to latest graded quarter in the system
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
                return "No grades found.";
            }
        }
    }

    // Step 3: Show all this student's grades in that quarter
    String sql = """
        SELECT c.course_number, c.title, o.year, o.quarter, e.grade
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code
                                      AND e.year = o.year AND e.quarter = o.quarter
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

    return sb.length() == 0 ? "No grades for you in " + latestQuarter + " " + latestYear + "." : sb.toString();
}





    public static String getRequirementCheck(String perm) throws SQLException {
    String major = getMajorByPerm(perm);
    if (major == null) return "❌ No major found for student.";

    // Step 1: Get all completed courses (grade D or higher)
    Set<String> completed = new HashSet<>();
    String sql = """
        SELECT DISTINCT c.course_number
        FROM test_enrollments e
        JOIN (
            SELECT DISTINCT enrollment_code, course_number FROM test_course_offerings
        ) o ON e.enrollment_code = o.enrollment_code
        JOIN test_courses c ON o.course_number = c.course_number
        WHERE TRIM(e.perm) = ?
          AND e.grade IS NOT NULL
          AND UPPER(e.grade) IN ('D', 'D+', 'C', 'C+', 'B-', 'B', 'B+', 'A-', 'A', 'A+')
    """;

    try (PreparedStatement stmt = Database.conn.prepareStatement(sql)) {
        stmt.setString(1, perm.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            completed.add(rs.getString("course_number").trim());
        }
    }

    // Step 2: Load required and elective courses
    Map<String, String> required = new LinkedHashMap<>();
    Map<String, String> electives = new LinkedHashMap<>();

    String reqSql = """
        SELECT r.course_number, c.title, r.is_elective
        FROM test_major_requirements r
        JOIN test_courses c ON r.course_number = c.course_number
        WHERE r.major_name = ?
        ORDER BY r.is_elective, r.course_number
    """;

    try (PreparedStatement stmt = Database.conn.prepareStatement(reqSql)) {
        stmt.setString(1, major.trim());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String course = rs.getString("course_number").trim();
            String title = rs.getString("title");
            String isElective = rs.getString("is_elective");

            if ("Y".equalsIgnoreCase(isElective)) {
                electives.put(course, title);
            } else {
                required.put(course, title);
            }
        }
    }

    // Step 3: Get number of electives required
    int numElectivesRequired = 0;
    String numElectivesSql = "SELECT num_electives_required FROM test_majors WHERE major_name = ?";
    try (PreparedStatement stmt = Database.conn.prepareStatement(numElectivesSql)) {
        stmt.setString(1, major.trim());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            numElectivesRequired = rs.getInt(1);
        }
    }

    // Step 4: Build the display string
    StringBuilder sb = new StringBuilder();
    sb.append("Required Courses:\n");
    for (var entry : required.entrySet()) {
        String status = completed.contains(entry.getKey()) ? "✅" : "❌";
        sb.append(status).append(" ").append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
    }

    sb.append("\nElective Courses (need ").append(numElectivesRequired).append("):\n");
    for (var entry : electives.entrySet()) {
        String status = completed.contains(entry.getKey()) ? "✅" : "❌";
        sb.append(status).append(" ").append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
    }

    return sb.toString();
}






    public static int getAvailableSpots(String courseNum, int year, String quarter) throws SQLException {
    String sql = """
        SELECT o.max_enrollment, COUNT(e.perm) AS enrolled
        FROM test_course_offerings o
        LEFT JOIN test_enrollments e ON o.enrollment_code = e.enrollment_code AND o.year = e.year AND o.quarter = e.quarter
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

    // Step 1: Get major
    String major = getMajorByPerm(perm);

    // Step 2: Separate required and elective courses
    Set<String> requiredCourses = new HashSet<>();
    Set<String> electiveCourses = new HashSet<>();

    String reqSql = "SELECT course_number, is_elective FROM test_major_requirements WHERE TRIM(major_name) = ?";
    try (PreparedStatement ps = Database.conn.prepareStatement(reqSql)) {
        ps.setString(1, major.trim());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String course = rs.getString("course_number").trim();
            String isElective = rs.getString("is_elective");
            if ("Y".equalsIgnoreCase(isElective)) {
                electiveCourses.add(course);
            } else {
                requiredCourses.add(course);
            }
        }
    }

    // Step 3: Get number of electives required
    int numElectivesRequired = 0;
    String electiveCountSql = "SELECT num_electives_required FROM test_majors WHERE TRIM(major_name) = ?";
    try (PreparedStatement ps = Database.conn.prepareStatement(electiveCountSql)) {
        ps.setString(1, major.trim());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            numElectivesRequired = rs.getInt(1);
        }
    }

    // Step 4: Get completed courses
    Set<String> completed = new HashSet<>();
    String completedSql = """
        SELECT DISTINCT TRIM(c.course_number)
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
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

    // Step 5: Get in-progress courses
    Set<String> inProgress = new HashSet<>();
    String inProgressSql = """
        SELECT DISTINCT TRIM(c.course_number)
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
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

    // Step 6: Determine remaining required and elective courses
    Set<String> toTake = new HashSet<>();
    for (String course : requiredCourses) {
        if (!completed.contains(course) && !inProgress.contains(course)) {
            toTake.add(course);
        }
    }

    List<String> completedElectives = electiveCourses.stream().filter(completed::contains).toList();
    List<String> inProgressElectives = electiveCourses.stream().filter(inProgress::contains).toList();
    int electivesStillNeeded = numElectivesRequired - completedElectives.size() - inProgressElectives.size();

    for (String course : electiveCourses) {
        if (electivesStillNeeded <= 0) break;
        if (!completed.contains(course) && !inProgress.contains(course)) {
            toTake.add(course);
            electivesStillNeeded--;
        }
    }

    // Step 7: Load prerequisites
    Map<String, Set<String>> prereqMap = new HashMap<>();
    String prereqSql = "SELECT TRIM(course_number), TRIM(prereq_course) FROM test_prerequisites";
    try (PreparedStatement ps = Database.conn.prepareStatement(prereqSql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String course = rs.getString(1);
            String prereq = rs.getString(2);
            prereqMap.computeIfAbsent(course, k -> new HashSet<>()).add(prereq);
        }
    }

    // Step 8: Determine the current/next quarter
    QuarterInfo nextQuarter = getNextQuarterFromAllGrades();
    String currentRank = getQuarterRank(nextQuarter.year, nextQuarter.quarter);
    int currentValue = getQuarterValue(currentRank);

    // Step 9: Get future quarters only
    List<String> futureQuarters = new ArrayList<>();
    String quarterSql = """
        SELECT year, quarter FROM test_quarters
        ORDER BY year, CASE quarter WHEN 'W' THEN 1 WHEN 'S' THEN 2 WHEN 'F' THEN 3 END
    """;
    try (PreparedStatement ps = Database.conn.prepareStatement(quarterSql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            int year = rs.getInt("year");
            String quarter = rs.getString("quarter").trim();
            String rank = getQuarterRank(year, quarter);
            int rankValue = getQuarterValue(rank);
            if (rankValue > currentValue) {
                futureQuarters.add(rank);
            }
        }
    }

    // Step 10: Get offerings by quarter
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

    // Step 11: Schedule courses into future quarters
    Set<String> scheduled = new HashSet<>(completed);
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

private static int getQuarterValue(String rank) {
    int year = Integer.parseInt(rank.substring(1));
    char q = rank.charAt(0);
    int qValue = switch (q) {
        case 'W' -> 1;
        case 'S' -> 2;
        case 'F' -> 3;
        default -> 0;
    };
    return year * 10 + qValue;
}






private static String getQuarterRank(int year, String quarter) {
    return quarter.toUpperCase().charAt(0) + String.valueOf(year).substring(2);
}


    public static class QuarterInfo {
        public int year;
        public String quarter;
    }

    public static QuarterInfo getNextQuarterFromAllGrades() throws SQLException {
    String sql = """
        SELECT o.year, o.quarter
        FROM test_enrollments e
        JOIN test_course_offerings o ON e.enrollment_code = o.enrollment_code AND e.year = o.year AND e.quarter = o.quarter
        WHERE e.grade IS NOT NULL
        ORDER BY o.year DESC,
                 CASE o.quarter
                     WHEN 'F' THEN 3
                     WHEN 'W' THEN 2
                     WHEN 'S' THEN 1
                     ELSE 0
                 END DESC
        FETCH FIRST 1 ROWS ONLY
    """;

    try (PreparedStatement ps = Database.conn.prepareStatement(sql)) {
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int latestYear = rs.getInt("year");
            String latestQuarter = rs.getString("quarter");

            QuarterInfo next = new QuarterInfo();
            switch (latestQuarter) {
                case "F" -> {
                    next.year = latestYear + 1;
                    next.quarter = "W";
                }
                case "W" -> {
                    next.year = latestYear;
                    next.quarter = "S";
                }
                case "S" -> {
                    next.year = latestYear;
                    next.quarter = "F";
                }
                default -> throw new SQLException("Invalid quarter in DB: " + latestQuarter);
            }

            System.out.println("📋 [DEBUG] Most recent graded quarter = " + latestQuarter + " " + latestYear);
            System.out.println("➡️ [DEBUG] Next quarter = " + next.quarter + " " + next.year);
            return next;
        }
    }

    // Fallback if no grades are in the system
    System.out.println("⚠️ [DEBUG] No graded enrollments found. Defaulting to Fall this year.");
    QuarterInfo fallback = new QuarterInfo();
    fallback.quarter = "F";
    fallback.year = java.time.LocalDate.now().getYear();
    return fallback;
}













}
