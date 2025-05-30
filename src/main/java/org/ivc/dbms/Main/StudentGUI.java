package org.ivc.dbms.Main;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class StudentGUI extends JFrame {
    private JButton addButton, dropButton, listCoursesButton, listGradesButton,
            enterGradesButton, transcriptButton, mailerButton;

    public StudentGUI() {
        setTitle("Gold Interface - IV College");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Student Actions"));

        addButton = new JButton("Add a Course");
        dropButton = new JButton("Drop a Course");
        listCoursesButton = new JButton("List My Courses");
        listGradesButton = new JButton("List Grades Last Quarter");
        enterGradesButton = new JButton("Requirement Check");
        transcriptButton = new JButton("Make a Plan");
        mailerButton = new JButton("Change PIN");

        buttonPanel.add(addButton);
        buttonPanel.add(dropButton);
        buttonPanel.add(listCoursesButton);
        buttonPanel.add(listGradesButton);
        buttonPanel.add(enterGradesButton);
        buttonPanel.add(transcriptButton);
        buttonPanel.add(mailerButton);

        add(buttonPanel, BorderLayout.CENTER);

        // ✅ Add Course
        addButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("Not logged in.");
                return;
            }

            try {
                String major = Student.getMajorByPerm(perm);
                if (major == null) {
                    showInfo("Student not found.");
                    return;
                }

                Map<String, String> courseMap = Student.getCoursesForMajorMap(major);
                System.out.println("📋 [DEBUG] Courses for major " + major + ": " + courseMap);

                if (courseMap.isEmpty()) {
                    showInfo("No courses found for major " + major + ".");
                    return;
                }

                JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
                Student.QuarterInfo qInfo = Student.getNextQuarterFromAllGrades();
                if (qInfo == null) {
                    showInfo("No recent quarter found.");
                    return;
                }
                int year = qInfo.year;
                String quarter = qInfo.quarter;


                for (var entry : courseMap.entrySet()) {
                    String courseNum = entry.getKey();
                    Student.CourseOfferingInfo info;
                    try {
                        info = Student.getOfferingInfo(courseNum, year, quarter);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        continue;
                    }

                    if (info == null) continue;

                    String display = String.format(
                        "%s – %s<br>🧑‍🏫 Instructor: %s<br>🕒 Time: %s<br>👥 Max: %d | Available: %d",
                        info.courseNumber, info.title,
                        info.instructorName != null ? info.instructorName : "(Unknown)",
                        info.timeSlot != null ? info.timeSlot : "(TBD)",
                        info.maxEnrollment, info.availableSpots()
                    );

                    JPanel row = new JPanel(new BorderLayout());
                    row.add(new JLabel("<html>" + display + "</html>"), BorderLayout.CENTER);

                    JButton addCourseBtn = new JButton("Add");
                    if (info.availableSpots() <= 0) {
                        addCourseBtn.setEnabled(false);
                        addCourseBtn.setToolTipText("Class is full");
                    }

                    addCourseBtn.addActionListener(ev -> {
                        try {
                            Student.enrollInCourse(perm, courseNum, year, quarter);
                            showInfo("✅ Enrolled in " + courseNum + " (" + quarter + " " + year + ")");
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showInfo("❌ Failed to enroll: " + ex.getMessage());
                        }
                    });

                    row.add(addCourseBtn, BorderLayout.EAST);
                    panel.add(row);

                    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                    separator.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                    panel.add(separator);
                }

                JScrollPane scrollPane = new JScrollPane(panel);
                JDialog dialog = new JDialog(this, "Add Course from Major: " + major, true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.getContentPane().add(scrollPane);
                dialog.setSize(600, 400);
                dialog.setResizable(true);
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error fetching courses.");
            }
        });

        // ✅ Drop Course
        dropButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("❌ Not logged in.");
                return;
            }

            try {
                Map<String, String> enrolledCourses = Student.getCurrentCoursesMap(perm);
                if (enrolledCourses.isEmpty()) {
                    showInfo("You're not currently enrolled in any courses.");
                    return;
                }

                JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
                for (var entry : enrolledCourses.entrySet()) {
                    String courseNum = entry.getKey();
                    String title = entry.getValue();

                    JPanel row = new JPanel(new BorderLayout());
                    row.add(new JLabel(courseNum + " - " + title), BorderLayout.CENTER);

                    JButton dropCourseBtn = new JButton("Drop");
                    row.add(dropCourseBtn, BorderLayout.EAST);

                    dropCourseBtn.addActionListener(ev -> {
                        try {
                            Student.dropCourse(perm, courseNum);
                            showInfo("✅ Dropped course " + courseNum);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showInfo("❌ Failed to drop course " + courseNum);
                        }
                    });

                    panel.add(row);
                    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                    separator.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                    panel.add(separator);
                }

                JScrollPane scrollPane = new JScrollPane(panel);
                JDialog dialog = new JDialog(this, "Drop a Course", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.getContentPane().add(scrollPane);
                dialog.setSize(500, 300);
                dialog.setResizable(true);
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error fetching current courses.");
            }
        });

        // ✅ List My Courses
        listCoursesButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("❌ Not logged in.");
                return;
            }

            try {
                String courseList = Student.getCurrentCourses(perm);
                if (courseList.isEmpty()) {
                    showInfo("You're not currently enrolled in any courses.");
                } else {
                    showInfo("📚 Current Enrollments:\n\n" + courseList);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error fetching current courses.");
            }
        });

        // ✅ List Grades Last Quarter
        listGradesButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("❌ Not logged in.");
                return;
            }

            try {
                String gradeList = Student.getGradesLastQuarter(perm);
                System.out.println("📋 [GUI DEBUG] Grades received:\n" + gradeList);

                if (gradeList.isEmpty()) {
                    showInfo("📭 No grades found for the last quarter.");
                } else {
                    showInfo("📄 Grades from Last Quarter:\n\n" + gradeList);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error fetching grades.");
            }
        });

        // ✅ Requirement Check
        enterGradesButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("❌ Not logged in.");
                return;
            }

            try {
                String result = Student.getRequirementCheck(perm);
                showInfo("📋 Requirement Check:\n\n" + result);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error checking requirements.");
            }
        });

        // ✅ PIN Change
        mailerButton.addActionListener(e -> {
            JTextField currentPinField = new JPasswordField();
            JTextField newPinField = new JPasswordField();
            JTextField confirmPinField = new JPasswordField();

            Object[] pinFields = {
                "Current PIN:", currentPinField,
                "New PIN:", newPinField,
                "Confirm New PIN:", confirmPinField
            };

            int option = JOptionPane.showConfirmDialog(
                this, pinFields, "Change PIN", JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) return;

            try {
                String perm = Database.loggedInPerm;
                if (perm == null) {
                    showInfo("❌ No logged-in student.");
                    return;
                }

                String currentPin = currentPinField.getText().trim();
                String newPin = newPinField.getText().trim();
                String confirmPin = confirmPinField.getText().trim();

                if (newPin == null || !newPin.equals(confirmPin)) {
                    showInfo("❌ New PIN and confirmation do not match.");
                    return;
                }

                if (!newPin.equals(confirmPin)) {
 
                    showInfo("❌ New PIN and confirmation do not match.");
                    return;
                }

                if (!Student.verifyCurrentPin(perm, currentPin)) {
                    showInfo("❌ Current PIN is incorrect.");
                    return;
                }


                if (Student.isPinAlreadyUsed(newPin, perm)) {
                    showInfo("❌ That PIN is already in use. Choose a different one.");
                    return;
                }

                Student.updatePin(perm, newPin);
                showInfo("✅ PIN changed successfully.");
            } catch (NumberFormatException ex) {
                showInfo("❌ PIN must be a number.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error updating PIN.");
            }
        });

        // ✅ Placeholder for transcript planning
        transcriptButton.addActionListener(e -> {
            String perm = Database.loggedInPerm;
            if (perm == null) {
                showInfo("❌ Not logged in.");
                return;
            }

            try {
                Map<String, List<String>> plan = Student.generateOptimalPlan(perm);
                if (plan.isEmpty()) {
                    showInfo("🎓 All requirements already completed!");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("🗓️ Optimal Plan:\n\n");
                for (var entry : plan.entrySet()) {
                    sb.append(entry.getKey()).append(":\n");
                    for (String course : entry.getValue()) {
                        sb.append("  - ").append(course).append("\n");
                    }
                    sb.append("\n");
                }

                showInfo(sb.toString());
            } catch (SQLException ex) {
                ex.printStackTrace();
                showInfo("❌ Error generating plan.");
            }
        });


    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }
}
