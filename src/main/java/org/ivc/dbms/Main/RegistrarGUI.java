package org.ivc.dbms.Main;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegistrarGUI extends JFrame {
    public RegistrarGUI() {
        setTitle("Registrar Admin Console");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);

        JButton addStudentBtn = new JButton("Add Student to Course");
        addStudentBtn.setBounds(50, 20, 300, 30);
        add(addStudentBtn);
        addStudentBtn.addActionListener(e -> {
            JTextField permField = new JTextField();
            JTextField codeField = new JTextField();
            JTextField yearField = new JTextField();
            JTextField quarterField = new JTextField();
            Object[] fields = {
                "Perm:", permField,
                "Enrollment Code:", codeField,
                "Year:", yearField,
                "Quarter:", quarterField
            };
            int res = JOptionPane.showConfirmDialog(null, fields, "Add Student", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                Admin.Ref().inputCommand(new Admin.AddStudentToCourse(
                    permField.getText().trim(),
                    codeField.getText().trim(),
                    yearField.getText().trim(),
                    quarterField.getText().trim()
                ));
            }
        });

        JButton dropStudentBtn = new JButton("Drop Student from Course");
        dropStudentBtn.setBounds(50, 60, 300, 30);
        add(dropStudentBtn);
        dropStudentBtn.addActionListener(e -> {
            JTextField permField = new JTextField();
            JTextField codeField = new JTextField();
            JTextField yearField = new JTextField();
            JTextField quarterField = new JTextField();
            Object[] fields = {
                "Perm:", permField,
                "Enrollment Code:", codeField,
                "Year:", yearField,
                "Quarter:", quarterField
            };
            int res = JOptionPane.showConfirmDialog(null, fields, "Drop Student", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                Admin.Ref().inputCommand(new Admin.DropStudentFromCourse(
                    permField.getText().trim(),
                    codeField.getText().trim(),
                    yearField.getText().trim(),
                    quarterField.getText().trim()
                ));
            }
        });

        JButton listCoursesBtn = new JButton("List Courses Taken");
        listCoursesBtn.setBounds(50, 100, 300, 30);
        add(listCoursesBtn);
        listCoursesBtn.addActionListener(e -> {
            String perm = JOptionPane.showInputDialog(this, "Enter Student Perm:");
            if (perm != null && !perm.isBlank()) {
                Admin.Ref().inputCommand(new Admin.ListCoursesTaken(perm.trim()));
            }
        });

        JButton listGradesBtn = new JButton("List Grades Last Quarter");
        listGradesBtn.setBounds(50, 140, 300, 30);
        add(listGradesBtn);
        listGradesBtn.addActionListener(e -> {
            String perm = JOptionPane.showInputDialog(this, "Enter Student Perm:");
            if (perm != null && !perm.isBlank()) {
                Admin.Ref().inputCommand(new Admin.ListGradesLastQuarter(perm.trim()));
            }
        });

        JButton classListBtn = new JButton("Generate Class List");
        classListBtn.setBounds(50, 180, 300, 30);
        add(classListBtn);
        classListBtn.addActionListener(e -> {
            JTextField courseField = new JTextField();
            JTextField yearField = new JTextField();
            JTextField quarterField = new JTextField();
            Object[] fields = {
                "Course Number:", courseField,
                "Year:", yearField,
                "Quarter:", quarterField
            };
            int res = JOptionPane.showConfirmDialog(null, fields, "Class List", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                Admin.Ref().inputCommand(new Admin.ClassList(
                    courseField.getText().trim(),
                    yearField.getText().trim(),
                    quarterField.getText().trim()
                ));
            }
        });

        JButton enterGradesBtn = new JButton("Enter Grades from File");
        enterGradesBtn.setBounds(50, 220, 300, 30);
        add(enterGradesBtn);
        enterGradesBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                Admin.Ref().inputCommand(new Admin.EnterGradesFromFile(path));
            }
        });
        

        JButton transcriptBtn = new JButton("Print Transcript");
        transcriptBtn.setBounds(50, 260, 300, 30);
        add(transcriptBtn);
        transcriptBtn.addActionListener(e -> {
            String perm = JOptionPane.showInputDialog(this, "Enter Student Perm:");
            if (perm != null && !perm.isBlank()) {
                Admin.Ref().inputCommand(new Admin.PrintTranscript(perm.trim()));
            }
        });

        JButton gradeMailerBtn = new JButton("Send Grade Mailer");
        gradeMailerBtn.setBounds(50, 300, 300, 30); // FIXED: not overlapping transcriptBtn
        add(gradeMailerBtn);
        gradeMailerBtn.addActionListener(e -> {
            JTextField quarterField = new JTextField();
            JTextField yearField = new JTextField();
            Object[] fields = {
                "Quarter (W/S/F):", quarterField,
                "Year (e.g., 2025):", yearField
            };
            int res = JOptionPane.showConfirmDialog(null, fields, "Grade Mailer", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                Admin.Ref().inputCommand(new Admin.GradeMailerAllStudents(
                    quarterField.getText().trim().toUpperCase(),
                    yearField.getText().trim()
                ));
            }
        });
    }
}
