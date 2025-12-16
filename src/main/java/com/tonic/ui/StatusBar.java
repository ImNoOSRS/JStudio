package com.tonic.ui;

import com.tonic.ui.event.EventBus;
import com.tonic.ui.event.events.StatusMessageEvent;
import com.tonic.ui.theme.JStudioTheme;
import com.tonic.ui.theme.Theme;
import com.tonic.ui.theme.ThemeManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.SwingUtilities;

/**
 * Status bar component showing messages, progress, and memory usage.
 */
public class StatusBar extends JPanel implements ThemeManager.ThemeChangeListener {

    private final JLabel messageLabel;
    private final JLabel positionLabel;
    private final JLabel modeLabel;
    private final JLabel memoryLabel;
    private final JProgressBar progressBar;

    private Timer memoryTimer;
    private Timer clearMessageTimer;

    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 24));
        setBackground(JStudioTheme.getBgSecondary());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JStudioTheme.getBorder()));

        // Left panel - message and position
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        leftPanel.setOpaque(false);

        messageLabel = createLabel("");
        positionLabel = createLabel("Ready");
        modeLabel = createLabel("");

        leftPanel.add(messageLabel);
        leftPanel.add(createSeparator());
        leftPanel.add(positionLabel);

        // Center - progress bar (hidden by default)
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(150, 12));
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setForeground(JStudioTheme.getAccent());
        progressBar.setBackground(JStudioTheme.getBgTertiary());
        progressBar.setBorderPainted(false);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        centerPanel.setOpaque(false);
        centerPanel.add(progressBar);

        // Right panel - mode and memory
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        rightPanel.setOpaque(false);

        memoryLabel = createLabel("");
        rightPanel.add(modeLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(memoryLabel);

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Update memory usage periodically
        memoryTimer = new Timer(2000, e -> updateMemory());
        memoryTimer.start();
        updateMemory();

        // Register for status message events
        EventBus.getInstance().register(StatusMessageEvent.class, this::handleStatusMessage);

        // Timer to clear temporary messages
        clearMessageTimer = new Timer(5000, e -> {
            messageLabel.setText("");
            clearMessageTimer.stop();
        });
        clearMessageTimer.setRepeats(false);

        ThemeManager.getInstance().addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        SwingUtilities.invokeLater(this::applyTheme);
    }

    private void applyTheme() {
        setBackground(JStudioTheme.getBgSecondary());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JStudioTheme.getBorder()));

        messageLabel.setForeground(JStudioTheme.getTextSecondary());
        messageLabel.setFont(JStudioTheme.getUIFont(11));

        positionLabel.setForeground(JStudioTheme.getTextSecondary());
        positionLabel.setFont(JStudioTheme.getUIFont(11));

        modeLabel.setForeground(JStudioTheme.getTextSecondary());
        modeLabel.setFont(JStudioTheme.getUIFont(11));

        memoryLabel.setFont(JStudioTheme.getUIFont(11));

        progressBar.setForeground(JStudioTheme.getAccent());
        progressBar.setBackground(JStudioTheme.getBgTertiary());

        repaint();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(JStudioTheme.getTextSecondary());
        label.setFont(JStudioTheme.getUIFont(11));
        return label;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 16));
        sep.setForeground(JStudioTheme.getBorder());
        return sep;
    }

    private void handleStatusMessage(StatusMessageEvent event) {
        setMessage(event.getMessage(), event.getType());
    }

    /**
     * Set the status message.
     */
    public void setMessage(String message) {
        setMessage(message, StatusMessageEvent.MessageType.INFO);
    }

    /**
     * Set the status message with type.
     */
    public void setMessage(String message, StatusMessageEvent.MessageType type) {
        messageLabel.setText(message);

        switch (type) {
            case WARNING:
                messageLabel.setForeground(JStudioTheme.getWarning());
                break;
            case ERROR:
                messageLabel.setForeground(JStudioTheme.getError());
                break;
            default:
                messageLabel.setForeground(JStudioTheme.getTextSecondary());
                break;
        }

        // Auto-clear after 5 seconds
        clearMessageTimer.restart();
    }

    /**
     * Set the position label (e.g., "Line 42, Col 10").
     */
    public void setPosition(String position) {
        positionLabel.setText(position);
    }

    /**
     * Set the mode label (e.g., "Source", "Bytecode", "IR").
     */
    public void setMode(String mode) {
        modeLabel.setText(mode);
    }

    /**
     * Show the progress bar with indeterminate state.
     */
    public void showProgress(String message) {
        progressBar.setIndeterminate(true);
        progressBar.setString(message);
        progressBar.setVisible(true);
    }

    /**
     * Show the progress bar with determinate state.
     */
    public void showProgress(int current, int total, String message) {
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(total);
        progressBar.setValue(current);
        progressBar.setString(message + " (" + current + "/" + total + ")");
        progressBar.setVisible(true);
    }

    /**
     * Hide the progress bar.
     */
    public void hideProgress() {
        progressBar.setVisible(false);
    }

    private void updateMemory() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        memoryLabel.setText(used + " / " + max + " MB");

        // Change color based on usage
        double usage = (double) used / max;
        if (usage > 0.85) {
            memoryLabel.setForeground(JStudioTheme.getError());
        } else if (usage > 0.70) {
            memoryLabel.setForeground(JStudioTheme.getWarning());
        } else {
            memoryLabel.setForeground(JStudioTheme.getTextSecondary());
        }
    }

    /**
     * Clean up timers when the status bar is no longer needed.
     */
    public void dispose() {
        if (memoryTimer != null) {
            memoryTimer.stop();
        }
        if (clearMessageTimer != null) {
            clearMessageTimer.stop();
        }
    }
}
