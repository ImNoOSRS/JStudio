package com.tonic.ui.editor;

import com.tonic.parser.MethodEntry;
import com.tonic.ui.MainFrame;
import com.tonic.ui.model.ClassEntryModel;
import com.tonic.ui.model.MethodEntryModel;
import com.tonic.ui.model.ProjectModel;
import com.tonic.ui.theme.Icons;
import com.tonic.ui.theme.JStudioTheme;
import com.tonic.ui.theme.Theme;
import com.tonic.ui.theme.ThemeManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Welcome tab showing project info and quick links to main() methods.
 */
public class WelcomeTab extends JPanel implements ThemeManager.ThemeChangeListener {

    private final MainFrame mainFrame;
    private ProjectModel projectModel;

    private JPanel contentPanel;
    private JPanel mainMethodsPanel;
    private JLabel projectNameLabel;
    private JLabel classCountLabel;
    private JLabel methodCountLabel;
    private JScrollPane scrollPane;

    public WelcomeTab(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setBackground(JStudioTheme.getBgTertiary());

        // Create scrollable content
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(JStudioTheme.getBgTertiary());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // Header section
        contentPanel.add(createHeaderSection());
        contentPanel.add(Box.createVerticalStrut(30));

        // Stats section
        contentPanel.add(createStatsSection());
        contentPanel.add(Box.createVerticalStrut(30));

        // Main methods section
        contentPanel.add(createMainMethodsSection());
        contentPanel.add(Box.createVerticalStrut(30));

        // Quick actions section
        contentPanel.add(createQuickActionsSection());

        // Add glue to push content to top
        contentPanel.add(Box.createVerticalGlue());

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(JStudioTheme.getBgTertiary());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        ThemeManager.getInstance().addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        SwingUtilities.invokeLater(this::applyTheme);
    }

    private void applyTheme() {
        setBackground(JStudioTheme.getBgTertiary());
        contentPanel.setBackground(JStudioTheme.getBgTertiary());
        mainMethodsPanel.setBackground(JStudioTheme.getBgTertiary());
        scrollPane.getViewport().setBackground(JStudioTheme.getBgTertiary());
        repaint();
    }

    private JPanel createHeaderSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(JStudioTheme.getBgTertiary());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Logo/Title
        JLabel titleLabel = new JLabel("JStudio");
        titleLabel.setFont(JStudioTheme.getUIFont(28).deriveFont(Font.BOLD));
        titleLabel.setForeground(JStudioTheme.getAccent());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(8));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Java Reverse Engineering Suite");
        subtitleLabel.setFont(JStudioTheme.getUIFont(14));
        subtitleLabel.setForeground(JStudioTheme.getTextSecondary());
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel createStatsSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JStudioTheme.getBgSecondary());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JStudioTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(600, 120));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Project name
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel projectLabel = new JLabel("Project:");
        projectLabel.setForeground(JStudioTheme.getTextSecondary());
        panel.add(projectLabel, gbc);

        gbc.gridx = 1;
        projectNameLabel = new JLabel("No project loaded");
        projectNameLabel.setForeground(JStudioTheme.getTextPrimary());
        projectNameLabel.setFont(JStudioTheme.getUIFont(12).deriveFont(Font.BOLD));
        panel.add(projectNameLabel, gbc);

        // Class count
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel classesLabel = new JLabel("Classes:");
        classesLabel.setForeground(JStudioTheme.getTextSecondary());
        panel.add(classesLabel, gbc);

        gbc.gridx = 1;
        classCountLabel = new JLabel("0");
        classCountLabel.setForeground(JStudioTheme.getTextPrimary());
        panel.add(classCountLabel, gbc);

        // Method count
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel methodsLabel = new JLabel("Methods:");
        methodsLabel.setForeground(JStudioTheme.getTextSecondary());
        panel.add(methodsLabel, gbc);

        gbc.gridx = 1;
        methodCountLabel = new JLabel("0");
        methodCountLabel.setForeground(JStudioTheme.getTextPrimary());
        panel.add(methodCountLabel, gbc);

        return panel;
    }

    private JPanel createMainMethodsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(JStudioTheme.getBgTertiary());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section header
        JLabel headerLabel = new JLabel("Entry Points (main methods)");
        headerLabel.setFont(JStudioTheme.getUIFont(14).deriveFont(Font.BOLD));
        headerLabel.setForeground(JStudioTheme.getTextPrimary());
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(headerLabel);

        panel.add(Box.createVerticalStrut(12));

        // Container for main method links
        mainMethodsPanel = new JPanel();
        mainMethodsPanel.setLayout(new BoxLayout(mainMethodsPanel, BoxLayout.Y_AXIS));
        mainMethodsPanel.setBackground(JStudioTheme.getBgTertiary());
        mainMethodsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noMainLabel = new JLabel("No project loaded");
        noMainLabel.setForeground(JStudioTheme.getTextSecondary());
        noMainLabel.setFont(JStudioTheme.getUIFont(12));
        mainMethodsPanel.add(noMainLabel);

        panel.add(mainMethodsPanel);

        return panel;
    }

    private JPanel createQuickActionsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(JStudioTheme.getBgTertiary());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section header
        JLabel headerLabel = new JLabel("Quick Actions");
        headerLabel.setFont(JStudioTheme.getUIFont(14).deriveFont(Font.BOLD));
        headerLabel.setForeground(JStudioTheme.getTextPrimary());
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(headerLabel);

        panel.add(Box.createVerticalStrut(12));

        // Action buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonsPanel.setBackground(JStudioTheme.getBgTertiary());
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        buttonsPanel.add(createActionButton("Open File", Icons.getIcon("folder"), () -> mainFrame.showOpenDialog()));
        buttonsPanel.add(createActionButton("Search", Icons.getIcon("search"), () -> mainFrame.showFindInProjectDialog()));
        buttonsPanel.add(createActionButton("Transforms", Icons.getIcon("settings"), () -> mainFrame.showTransformDialog()));

        panel.add(buttonsPanel);

        return panel;
    }

    private JButton createActionButton(String text, javax.swing.Icon icon, Runnable action) {
        JButton button = new JButton(text, icon);
        button.setBackground(JStudioTheme.getBgSecondary());
        button.setForeground(JStudioTheme.getTextPrimary());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JStudioTheme.getBorder()),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(JStudioTheme.getHover());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(JStudioTheme.getBgSecondary());
            }
        });

        return button;
    }

    /**
     * Set the project model and refresh the display.
     */
    public void setProjectModel(ProjectModel project) {
        this.projectModel = project;
        refresh();
    }

    /**
     * Refresh the welcome tab with current project info.
     */
    public void refresh() {
        if (projectModel == null) {
            projectNameLabel.setText("No project loaded");
            classCountLabel.setText("0");
            methodCountLabel.setText("0");
            updateMainMethodsPanel(new ArrayList<>());
            return;
        }

        // Update stats
        String projectName = projectModel.getSourceFile() != null
                ? projectModel.getSourceFile().getName()
                : "Unknown";
        projectNameLabel.setText(projectName);

        List<ClassEntryModel> allClasses = projectModel.getAllClasses();
        classCountLabel.setText(String.valueOf(allClasses.size()));

        int methodCount = 0;
        for (ClassEntryModel cls : allClasses) {
            methodCount += cls.getMethods().size();
        }
        methodCountLabel.setText(String.valueOf(methodCount));

        // Find main methods
        List<MainMethodInfo> mainMethods = findMainMethods();
        updateMainMethodsPanel(mainMethods);
    }

    private List<MainMethodInfo> findMainMethods() {
        List<MainMethodInfo> result = new ArrayList<>();
        if (projectModel == null) return result;

        for (ClassEntryModel classEntry : projectModel.getAllClasses()) {
            for (MethodEntryModel methodModel : classEntry.getMethods()) {
                MethodEntry method = methodModel.getMethodEntry();
                // Check for public static void main(String[])
                if ("main".equals(method.getName())
                        && "([Ljava/lang/String;)V".equals(method.getDesc())
                        && (method.getAccess() & 0x0009) == 0x0009) { // public static
                    result.add(new MainMethodInfo(classEntry, methodModel));
                }
            }
        }

        return result;
    }

    private void updateMainMethodsPanel(List<MainMethodInfo> mainMethods) {
        mainMethodsPanel.removeAll();

        if (mainMethods.isEmpty()) {
            JLabel noMainLabel = new JLabel(projectModel == null
                    ? "No project loaded"
                    : "No main() methods found");
            noMainLabel.setForeground(JStudioTheme.getTextSecondary());
            noMainLabel.setFont(JStudioTheme.getUIFont(12));
            mainMethodsPanel.add(noMainLabel);
        } else {
            for (MainMethodInfo info : mainMethods) {
                mainMethodsPanel.add(createMainMethodLink(info));
                mainMethodsPanel.add(Box.createVerticalStrut(4));
            }
        }

        mainMethodsPanel.revalidate();
        mainMethodsPanel.repaint();
    }

    private JPanel createMainMethodLink(MainMethodInfo info) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        panel.setBackground(JStudioTheme.getBgTertiary());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Class icon
        JLabel iconLabel = new JLabel(info.classEntry.getIcon());
        panel.add(iconLabel);

        // Clickable class name
        JLabel classLink = new JLabel(info.classEntry.getClassName());
        classLink.setForeground(JStudioTheme.getAccent());
        classLink.setFont(JStudioTheme.getCodeFont(12));
        classLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        classLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.openClassInEditor(info.classEntry);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                classLink.setText("<html><u>" + info.classEntry.getClassName() + "</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                classLink.setText(info.classEntry.getClassName());
            }
        });
        panel.add(classLink);

        // main() indicator
        JLabel mainLabel = new JLabel(".main(String[])");
        mainLabel.setForeground(JStudioTheme.getTextSecondary());
        mainLabel.setFont(JStudioTheme.getCodeFont(12));
        panel.add(mainLabel);

        return panel;
    }

    /**
     * Helper class to hold main method info.
     */
    private static class MainMethodInfo {
        final ClassEntryModel classEntry;
        final MethodEntryModel methodEntry;

        MainMethodInfo(ClassEntryModel classEntry, MethodEntryModel methodEntry) {
            this.classEntry = classEntry;
            this.methodEntry = methodEntry;
        }
    }
}
