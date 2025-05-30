package org.ivc.dbms.Main;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class RegistrarGUI extends JFrame {
    private JTextField permField, courseField, yearField, quarterField;
    private JButton addButton, dropButton, listCoursesButton, listGradesButton, 
                    enterGradesButton, transcriptButton, mailerButton;

    public RegistrarGUI() {
        setTitle("Registrar Interface - IV College");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Student/Course Info"));

        permField = new JTextField();
        courseField = new JTextField();
        yearField = new JTextField();
        quarterField = new JTextField();

        inputPanel.add(new JLabel("Student Perm:"));
        inputPanel.add(permField);
        inputPanel.add(new JLabel("Course Number:"));
        inputPanel.add(courseField);
        inputPanel.add(new JLabel("Year:"));
        inputPanel.add(yearField);
        inputPanel.add(new JLabel("Quarter:"));
        inputPanel.add(quarterField);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Registrar Actions"));

        addButton = new JButton("Add Student to Course");
        dropButton = new JButton("Drop Student from Course");
        listCoursesButton = new JButton("List Student's Courses");
        listGradesButton = new JButton("List Grades Last Quarter");
        enterGradesButton = new JButton("Enter Course Grades (from file)");
        transcriptButton = new JButton("Generate Transcript");
        mailerButton = new JButton("Generate Grade Mailer");

        buttonPanel.add(addButton);
        buttonPanel.add(dropButton);
        buttonPanel.add(listCoursesButton);
        buttonPanel.add(listGradesButton);
        buttonPanel.add(enterGradesButton);
        buttonPanel.add(transcriptButton);
        buttonPanel.add(mailerButton);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        // Dummy action bindings (to be connected to JDBC functions)
        addButton.addActionListener(e -> showInfo("Add course for: " + permField.getText()));
        dropButton.addActionListener(e -> showInfo("Drop course for: " + permField.getText()));
        listCoursesButton.addActionListener(e -> showInfo("List courses for: " + permField.getText()));
        listGradesButton.addActionListener(e -> showInfo("List grades for: " + permField.getText()));
        enterGradesButton.addActionListener(e -> showInfo("Upload grades from file"));
        transcriptButton.addActionListener(e -> showInfo("Generate transcript for: " + permField.getText()));
        mailerButton.addActionListener(e -> showInfo("Generate grade mailers"));
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegistrarGUI().setVisible(true));
    }
}