package com.tonic.ui.dialog.filechooser;

import com.tonic.ui.theme.JStudioTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileSystemView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quick access sidebar with shortcuts to common locations and drives.
 */
public class QuickAccessPanel extends JPanel {

    /**
     * Listener for location selection events.
     */
    public interface LocationListener {
        void onLocationSelected(File location);
    }

    private final LocationListener listener;
    private final List<File> recentLocations = new ArrayList<>();
    private final DefaultListModel<QuickAccessItem> quickAccessModel;
    private final DefaultListModel<QuickAccessItem> drivesModel;
    private JList<QuickAccessItem> quickAccessList;
    private JList<QuickAccessItem> drivesList;

    public QuickAccessPanel(LocationListener listener) {
        this.listener = listener;

        setLayout(new BorderLayout());
        setBackground(JStudioTheme.getBgSecondary());
        setPreferredSize(new Dimension(160, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JStudioTheme.getBorder()));

        quickAccessModel = new DefaultListModel<>();
        drivesModel = new DefaultListModel<>();

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(JStudioTheme.getBgSecondary());

        // Quick Access section
        contentPanel.add(createSectionHeader("Quick Access"));
        quickAccessList = createList(quickAccessModel);
        contentPanel.add(quickAccessList);

        contentPanel.add(Box.createVerticalStrut(16));

        // Drives section
        contentPanel.add(createSectionHeader("This PC"));
        drivesList = createList(drivesModel);
        contentPanel.add(drivesList);

        contentPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(JStudioTheme.getBgSecondary());
        add(scrollPane, BorderLayout.CENTER);

        // Populate items
        populateQuickAccess();
        populateDrives();
    }

    private JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setForeground(JStudioTheme.getTextSecondary());
        header.setFont(JStudioTheme.getUIFont(11).deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 8));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JList<QuickAccessItem> createList(DefaultListModel<QuickAccessItem> model) {
        JList<QuickAccessItem> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(JStudioTheme.getBgSecondary());
        list.setForeground(JStudioTheme.getTextPrimary());
        list.setFont(JStudioTheme.getUIFont(12));
        list.setFixedCellHeight(28);
        list.setCellRenderer(new QuickAccessRenderer());
        list.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Single click to navigate
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    QuickAccessItem item = model.getElementAt(index);
                    if (item != null && item.file != null && listener != null) {
                        listener.onLocationSelected(item.file);
                        // Clear selection in other list
                        if (list == quickAccessList) {
                            drivesList.clearSelection();
                        } else {
                            quickAccessList.clearSelection();
                        }
                    }
                }
            }
        });

        return list;
    }

    private void populateQuickAccess() {
        quickAccessModel.clear();

        Map<String, File> folders = FileSystemWorker.getSpecialFolders();

        // Add special folders in order
        addIfExists(folders, "Desktop", quickAccessModel);
        addIfExists(folders, "Documents", quickAccessModel);
        addIfExists(folders, "Downloads", quickAccessModel);
        addIfExists(folders, "Home", quickAccessModel);

        // Add recent locations
        for (File recent : recentLocations) {
            if (recent.exists() && recent.isDirectory()) {
                quickAccessModel.addElement(new QuickAccessItem(
                        recent.getName(),
                        recent,
                        QuickAccessItemType.RECENT
                ));
            }
        }
    }

    private void addIfExists(Map<String, File> folders, String key,
                             DefaultListModel<QuickAccessItem> model) {
        File file = folders.get(key);
        if (file != null && file.exists()) {
            QuickAccessItemType type;
            switch (key) {
                case "Desktop":
                    type = QuickAccessItemType.DESKTOP;
                    break;
                case "Documents":
                    type = QuickAccessItemType.DOCUMENTS;
                    break;
                case "Downloads":
                    type = QuickAccessItemType.DOWNLOADS;
                    break;
                default:
                    type = QuickAccessItemType.FOLDER;
            }
            model.addElement(new QuickAccessItem(key, file, type));
        }
    }

    private void populateDrives() {
        drivesModel.clear();

        for (File root : FileSystemWorker.getRoots()) {
            String name = FileSystemWorker.getDisplayName(root);
            if (name == null || name.isEmpty()) {
                name = root.getAbsolutePath();
            }
            drivesModel.addElement(new QuickAccessItem(name, root, QuickAccessItemType.DRIVE));
        }
    }

    /**
     * Add a location to recent list.
     */
    public void addRecentLocation(File location) {
        if (location == null || !location.isDirectory()) {
            return;
        }

        // Remove if already in list
        recentLocations.remove(location);

        // Add to front
        recentLocations.add(0, location);

        // Limit size
        while (recentLocations.size() > 5) {
            recentLocations.remove(recentLocations.size() - 1);
        }

        // Refresh
        populateQuickAccess();
    }

    /**
     * Clear selection in all lists.
     */
    public void clearSelection() {
        quickAccessList.clearSelection();
        drivesList.clearSelection();
    }

    /**
     * Item types for different icons.
     */
    private enum QuickAccessItemType {
        DESKTOP, DOCUMENTS, DOWNLOADS, FOLDER, RECENT, DRIVE
    }

    /**
     * Quick access list item.
     */
    private static class QuickAccessItem {
        final String name;
        final File file;
        final QuickAccessItemType type;

        QuickAccessItem(String name, File file, QuickAccessItemType type) {
            this.name = name;
            this.file = file;
            this.type = type;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Custom renderer for quick access items.
     */
    private class QuickAccessRenderer implements ListCellRenderer<QuickAccessItem> {
        private final JPanel panel;
        private final JLabel iconLabel;
        private final JLabel textLabel;

        QuickAccessRenderer() {
            panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 8));

            iconLabel = new JLabel();
            textLabel = new JLabel();
            textLabel.setFont(JStudioTheme.getUIFont(12));

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(textLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends QuickAccessItem> list,
                                                      QuickAccessItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                panel.setBackground(JStudioTheme.getSelection());
                textLabel.setForeground(JStudioTheme.getTextPrimary());
            } else {
                panel.setBackground(JStudioTheme.getBgSecondary());
                textLabel.setForeground(JStudioTheme.getTextPrimary());
            }

            textLabel.setText(value.name);

            // Get system icon
            if (value.file != null) {
                javax.swing.Icon icon = FileSystemWorker.getSystemIcon(value.file);
                iconLabel.setIcon(icon);
            } else {
                iconLabel.setIcon(null);
            }

            return panel;
        }
    }
}
