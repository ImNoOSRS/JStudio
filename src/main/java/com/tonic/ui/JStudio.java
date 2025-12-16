package com.tonic.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.tonic.ui.theme.JStudioTheme;
import com.tonic.ui.theme.ThemeManager;
import com.tonic.ui.util.KeyboardShortcuts;
import com.tonic.ui.util.Settings;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.EventQueue;

/**
 * JStudio - Java Reverse Engineering Suite
 *
 * A professional reverse engineering and analysis tool for Java bytecode,
 * featuring decompilation, SSA IR visualization, call graph analysis,
 * and bytecode transformation capabilities.
 */
public class JStudio {

    public static final String APP_NAME = "JStudio";
    public static final String APP_VERSION = "1.0.0";

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    FlatDarkLaf.setup();

                    String savedTheme = Settings.getInstance().getTheme();
                    ThemeManager.getInstance().setTheme(savedTheme);

                    MainFrame frame = new MainFrame();

                    KeyboardShortcuts.register(frame);

                    frame.setVisible(true);

                    if (args.length > 0) {
                        frame.openFile(args[0]);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed to initialize JStudio: " + e.getMessage());
                    System.exit(1);
                }
            }
        });
    }
}
