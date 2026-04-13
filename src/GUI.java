import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class GUI extends JFrame {

    private static final String PAGE_HOME = "home";
    private static final String PAGE_SETTINGS = "settings";
    private static final String PAGE_HELP = "help";

    private final Sorter sorter = new Sorter();
    private final Object outputCaptureLock = new Object();
    private final Preferences prefs = Preferences.userNodeForPackage(GUI.class);

    private File selectedFolder;
    private boolean busy;

    private Color appBg;
    private Color cardBg;
    private Color panelBg;
    private Color sidebarBg;
    private Color accent;
    private Color accentSoft;
    private Color textPrimary;
    private Color textSoft;
    private Color border;
    private Color success;
    private Color warning;

    private boolean darkTheme = true;

    private JPanel root;
    private JPanel pageContainer;
    private CardLayout pageLayout;
    private JPanel sidebar;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel statusBadge;

    private JTextField folderPathBox;
    private JLabel folderStatsLabel;
    private JLabel rulesStatsLabel;
    private JLabel settingsSummaryLabel;
    private JTextArea outputBox;
    private JComboBox<String> recentFolderCombo;
    private JComboBox<String> modeCombo;
    private JSpinner bigFileSpinner;
    private JCheckBox safeModeCheck;
    private JCheckBox includeHiddenCheck;
    private JProgressBar progressBar;
    private JPanel actionPanel;

    public GUI() {
        configureLookAndFeel();
        applyPalette(true);
        setTitle("SFORA - Professional Organizer");
        setMinimumSize(new Dimension(1160, 760));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rebuildUi();
        setLocationRelativeTo(null);
        appendLog("Welcome to SFORA. Choose a folder and select an operation.");
    }

    private void rebuildUi() {
        setContentPane(buildMainPanel());
        refreshThemeStyles();
        revalidate();
        repaint();
    }

    private JPanel buildMainPanel() {
        root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(appBg);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContentArea(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(sidebarBg);
        sidebar.setBorder(new EmptyBorder(16, 14, 16, 14));

        JLabel brand = new JLabel("SFORA");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 26));
        brand.setForeground(textPrimary);

        JLabel tagline = new JLabel("File workflow studio");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tagline.setForeground(textSoft);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(2));
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(22));

        addNavButton("Home", UIManager.getIcon("FileView.homeFolderIcon"), () -> switchPage(PAGE_HOME));
        addNavButton("Settings", UIManager.getIcon("FileChooser.detailsViewIcon"), () -> switchPage(PAGE_SETTINGS));
        addNavButton("Help / Info", UIManager.getIcon("OptionPane.informationIcon"), () -> switchPage(PAGE_HELP));
        sidebar.add(Box.createVerticalStrut(10));

        addNavButton("Reset / Clear", UIManager.getIcon("OptionPane.warningIcon"), this::resetWorkspace);
        addNavButton("Export / Save", UIManager.getIcon("FileView.floppyDriveIcon"), this::exportOutputLog);

        sidebar.add(Box.createVerticalGlue());

        JLabel build = new JLabel("Build: " + java.time.LocalDate.now());
        build.setForeground(textSoft);
        build.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sidebar.add(build);

        return sidebar;
    }

    private void addNavButton(String text, Icon icon, Runnable action) {
        ModernButton button = createButton(text, icon, accentSoft, textPrimary);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> action.run());
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(8));
    }

    private JPanel buildContentArea() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);
        content.add(buildTopHeader(), BorderLayout.NORTH);

        pageLayout = new CardLayout();
        pageContainer = new JPanel(pageLayout);
        pageContainer.setOpaque(false);

        pageContainer.add(buildHomePage(), PAGE_HOME);
        pageContainer.add(buildSettingsPage(), PAGE_SETTINGS);
        pageContainer.add(buildHelpPage(), PAGE_HELP);

        content.add(pageContainer, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBackground(cardBg);
        header.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(textPrimary);

        subtitleLabel = new JLabel("Organize, preview, and manage file workflows from a single view.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(textSoft);

        textBlock.add(titleLabel);
        textBlock.add(Box.createVerticalStrut(2));
        textBlock.add(subtitleLabel);

        statusBadge = new JLabel("IDLE");
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusBadge.setOpaque(true);
        statusBadge.setBorder(new EmptyBorder(7, 10, 7, 10));

        header.add(textBlock, BorderLayout.WEST);
        header.add(statusBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel buildHomePage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        page.add(buildFolderToolbar(), BorderLayout.NORTH);
        page.add(buildMainTabs(), BorderLayout.CENTER);
        page.add(buildBottomStatusBar(), BorderLayout.SOUTH);

        return page;
    }

    private JPanel buildFolderToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 8));
        toolbar.setBackground(cardBg);
        toolbar.setBorder(new EmptyBorder(12, 12, 12, 12));

        folderPathBox = new JTextField("Select a folder to begin");
        folderPathBox.setEditable(false);
        folderPathBox.setFont(new Font("Consolas", Font.PLAIN, 13));
        styleTextComponent(folderPathBox);

        ModernButton browseBtn = createButton("Browse", UIManager.getIcon("FileView.directoryIcon"), accent, textPrimary);
        browseBtn.setToolTipText("Choose a folder from your file system");
        browseBtn.addActionListener(e -> chooseFolder());

        JPanel centerRow = new JPanel(new BorderLayout(8, 0));
        centerRow.setOpaque(false);
        centerRow.add(folderPathBox, BorderLayout.CENTER);
        centerRow.add(browseBtn, BorderLayout.EAST);

        JPanel shortcuts = new JPanel(new GridLayout(1, 4, 8, 0));
        shortcuts.setOpaque(false);

        ModernButton docsBtn = createButton("Documents", null, accentSoft, textPrimary);
        docsBtn.setToolTipText("Quick-select Documents folder");
        docsBtn.addActionListener(e -> selectKnownFolder("Documents", Paths.get(System.getProperty("user.home"), "Documents")));

        ModernButton downloadsBtn = createButton("Downloads", null, accentSoft, textPrimary);
        downloadsBtn.setToolTipText("Quick-select Downloads folder");
        downloadsBtn.addActionListener(e -> selectKnownFolder("Downloads", Paths.get(System.getProperty("user.home"), "Downloads")));

        ModernButton desktopBtn = createButton("Desktop", null, accentSoft, textPrimary);
        desktopBtn.setToolTipText("Quick-select Desktop folder");
        desktopBtn.addActionListener(e -> selectKnownFolder("Desktop", Paths.get(System.getProperty("user.home"), "Desktop")));

        recentFolderCombo = new JComboBox<>();
        styleComboBox(recentFolderCombo);
        refreshRecentFolderCombo();
        recentFolderCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedPath = (String) e.getItem();
                if (selectedPath != null && !selectedPath.isBlank() && !"Recent folders...".equals(selectedPath)) {
                    File candidate = new File(selectedPath);
                    if (candidate.exists() && candidate.isDirectory()) {
                        setSelectedFolder(candidate, "recent list");
                    }
                }
            }
        });

        shortcuts.add(docsBtn);
        shortcuts.add(downloadsBtn);
        shortcuts.add(desktopBtn);
        shortcuts.add(recentFolderCombo);

        toolbar.add(centerRow, BorderLayout.NORTH);
        toolbar.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        toolbar.add(shortcuts, BorderLayout.SOUTH);

        return toolbar;
    }

    private JTabbedPane buildMainTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(cardBg);
        tabs.setForeground(textPrimary);

        tabs.addTab("Operations", buildOperationsPanel());
        tabs.addTab("Activity", buildOutputPanel());
        tabs.addTab("Rules", buildRulesPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                titleLabel.setText("Dashboard");
                subtitleLabel.setText("Run operations and automation flows safely.");
            } else if (tabs.getSelectedIndex() == 1) {
                titleLabel.setText("Activity Log");
                subtitleLabel.setText("Review run output, warnings, and completion messages.");
            } else {
                titleLabel.setText("Rules");
                subtitleLabel.setText("Manage custom sorting logic and rule summaries.");
            }
        });
        return tabs;
    }

    private JPanel buildOperationsPanel() {
        JPanel shell = new JPanel(new BorderLayout(12, 12));
        shell.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(cardBg);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        controls.add(makeFieldLabel("Organize mode"));
        modeCombo = new JComboBox<>(new String[] { "hybrid", "rules", "default" });
        modeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        modeCombo.setToolTipText("Choose how files should be categorized");
        styleComboBox(modeCombo);
        controls.add(modeCombo);
        controls.add(Box.createVerticalStrut(10));

        controls.add(makeFieldLabel("Safety options"));
        safeModeCheck = new JCheckBox("Safe mode (recommended)", true);
        includeHiddenCheck = new JCheckBox("Include hidden files", false);
        styleCheckBox(safeModeCheck);
        styleCheckBox(includeHiddenCheck);
        includeHiddenCheck.setEnabled(false);

        safeModeCheck.setToolTipText("Protects sensitive and system-like folders from risky moves");
        includeHiddenCheck.setToolTipText("If enabled, hidden files are also scanned and moved");

        safeModeCheck.addActionListener(e -> {
            boolean safeMode = safeModeCheck.isSelected();
            includeHiddenCheck.setEnabled(!safeMode);
            if (safeMode) {
                includeHiddenCheck.setSelected(false);
            }
            updateSettingsSummary();
            appendLog("Safety updated: safe mode=" + safeMode + ", include hidden=" + includeHiddenCheck.isSelected());
            updateFolderInsightsAsync();
        });

        includeHiddenCheck.addActionListener(e -> {
            updateSettingsSummary();
            appendLog("Include hidden files: " + includeHiddenCheck.isSelected());
            updateFolderInsightsAsync();
        });

        controls.add(safeModeCheck);
        controls.add(includeHiddenCheck);
        controls.add(Box.createVerticalStrut(10));

        controls.add(makeFieldLabel("Big file limit (MB)"));
        bigFileSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
        bigFileSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        bigFileSpinner.setToolTipText("Files over this limit can be moved into BigFiles");
        styleSpinner(bigFileSpinner);
        controls.add(bigFileSpinner);
        controls.add(Box.createVerticalStrut(14));

        actionPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        actionPanel.setOpaque(false);

        addActionButton("Organize", UIManager.getIcon("FileView.fileIcon"), accent, textPrimary, "Build and run organize plan", this::confirmOrganizeWithPlan);
        addActionButton("Preview", UIManager.getIcon("FileChooser.newFolderIcon"), accentSoft, textPrimary, "Preview changes without moving files", this::showPreviewPlan);

        addActionButton("Fix Names", null, accentSoft, textPrimary, "Normalize messy file names", () -> runTask("Fix Names", true, () -> {
            applySorterOptions();
            sorter.fixFilenames(selectedFolder);
        }));

        addActionButton("Find Duplicates", null, accentSoft, textPrimary, "Scan for duplicate content using fingerprinting", () -> runTask("Find Duplicates", true, () -> {
            applySorterOptions();
            sorter.findDuplications(selectedFolder);
        }));

        addActionButton("Move Big Files", null, accentSoft, textPrimary, "Move large files into a dedicated folder", () -> runTask("Move Big Files", true, () -> {
            long mb = ((Number) bigFileSpinner.getValue()).longValue();
            applySorterOptions();
            sorter.extractLargeFiles(selectedFolder, mb);
        }));

        addActionButton("Undo Last", null, accentSoft, textPrimary, "Revert the most recent move", () -> runTask("Undo Last", false, sorter::undoLast));

        addActionButton("Undo All", null, warning, textPrimary, "Revert all logged actions", () -> {
            int option = JOptionPane.showConfirmDialog(this, "Undo everything from the action log?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                runTask("Undo All", false, sorter::undoAll);
            }
        });

        addActionButton("Open Report", null, accentSoft, textPrimary, "Open run history report", this::openReportFile);
        addActionButton("Clear Output", null, accentSoft, textPrimary, "Clear the activity panel", () -> outputBox.setText(""));

        controls.add(actionPanel);

        JScrollPane controlsScroll = new JScrollPane(controls);
        controlsScroll.setBorder(BorderFactory.createLineBorder(border));
        controlsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScroll.getVerticalScrollBar().setUnitIncrement(16);
        controlsScroll.getViewport().setBackground(cardBg);
        controlsScroll.setBackground(cardBg);

        folderStatsLabel = new JLabel("No folder selected");
        folderStatsLabel.setForeground(textSoft);
        folderStatsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        rulesStatsLabel = new JLabel(sorter.getRulesSummary());
        rulesStatsLabel.setForeground(textSoft);
        rulesStatsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel meta = new JPanel(new GridLayout(2, 1, 0, 2));
        meta.setOpaque(false);
        meta.add(folderStatsLabel);
        meta.add(rulesStatsLabel);

        card.add(controlsScroll, BorderLayout.CENTER);
        card.add(meta, BorderLayout.SOUTH);

        shell.add(card, BorderLayout.CENTER);
        return shell;
    }

    private JPanel buildOutputPanel() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(cardBg);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        outputBox = new JTextArea();
        outputBox.setEditable(false);
        outputBox.setLineWrap(true);
        outputBox.setWrapStyleWord(true);
        outputBox.setFont(new Font("Consolas", Font.PLAIN, 13));
        styleTextComponent(outputBox);
        outputBox.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(outputBox);
        scroll.setBorder(BorderFactory.createLineBorder(border));
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        ModernButton export = createButton("Export Log", UIManager.getIcon("FileView.floppyDriveIcon"), accentSoft, textPrimary);
        export.addActionListener(e -> exportOutputLog());
        export.setToolTipText("Save current output as a text file");

        ModernButton clear = createButton("Clear", null, accentSoft, textPrimary);
        clear.addActionListener(e -> outputBox.setText(""));
        clear.setToolTipText("Remove current output text");

        footer.add(export);
        footer.add(clear);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildRulesPanel() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(cardBg);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JTextArea quickView = new JTextArea(loadRulesText());
        quickView.setEditable(false);
        quickView.setFont(new Font("Consolas", Font.PLAIN, 13));
        styleTextComponent(quickView);
        quickView.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(quickView);
        scroll.setBorder(BorderFactory.createLineBorder(border));

        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
        actions.setOpaque(false);

        ModernButton openEditor = createButton("Edit Rules", null, accent, textPrimary);
        openEditor.setToolTipText("Open an editable rules dialog");
        openEditor.addActionListener(e -> {
            openRulesEditor();
            quickView.setText(loadRulesText());
        });

        ModernButton openFile = createButton("Open Rules File", null, accentSoft, textPrimary);
        openFile.setToolTipText("Open rules.txt in the default editor");
        openFile.addActionListener(e -> openRulesFile());

        ModernButton reload = createButton("Reload Rules", null, accentSoft, textPrimary);
        reload.setToolTipText("Reload rules and refresh summaries");
        reload.addActionListener(e -> {
            sorter.reloadRules();
            rulesStatsLabel.setText(sorter.getRulesSummary());
            quickView.setText(loadRulesText());
            appendLog("Rules reloaded from rules.txt.");
        });

        actions.add(openEditor);
        actions.add(openFile);
        actions.add(reload);

        card.add(scroll, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildBottomStatusBar() {
        JPanel status = new JPanel(new BorderLayout(8, 0));
        status.setBackground(cardBg);
        status.setBorder(new EmptyBorder(8, 10, 8, 10));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        settingsSummaryLabel = new JLabel();
        settingsSummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        settingsSummaryLabel.setForeground(textSoft);

        updateSettingsSummary();

        status.add(settingsSummaryLabel, BorderLayout.WEST);
        status.add(progressBar, BorderLayout.CENTER);
        return status;
    }

    private JPanel buildSettingsPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(cardBg);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel("Application Settings");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(textPrimary);

        JLabel hint = new JLabel("Adjust visual preferences and workflow defaults.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(textSoft);

        card.add(heading);
        card.add(Box.createVerticalStrut(2));
        card.add(hint);
        card.add(Box.createVerticalStrut(16));

        card.add(makeFieldLabel("Theme"));
        JComboBox<String> themeCombo = new JComboBox<>(new String[] { "Dark", "Light" });
        themeCombo.setMaximumSize(new Dimension(260, 34));
        themeCombo.setSelectedItem(darkTheme ? "Dark" : "Light");
        styleComboBox(themeCombo);
        themeCombo.addActionListener(e -> {
            boolean darkSelected = "Dark".equals(themeCombo.getSelectedItem());
            if (darkSelected != darkTheme) {
                darkTheme = darkSelected;
                SwingUtilities.invokeLater(() -> {
                    applyPalette(darkTheme);
                    rebuildUi();
                    switchPage(PAGE_SETTINGS);
                    appendLog("Theme switched to " + (darkTheme ? "dark" : "light") + " mode.");
                });
            }
        });
        card.add(themeCombo);

        card.add(Box.createVerticalStrut(12));

        ModernButton resetButton = createButton("Reset Workspace State", null, warning, textPrimary);
        resetButton.setMaximumSize(new Dimension(260, 36));
        resetButton.setToolTipText("Clear current selection and activity output");
        resetButton.addActionListener(e -> resetWorkspace());

        card.add(resetButton);
        card.add(Box.createVerticalStrut(10));

        ModernButton exportButton = createButton("Export Activity Log", null, accentSoft, textPrimary);
        exportButton.setMaximumSize(new Dimension(260, 36));
        exportButton.setToolTipText("Save the activity output to a file");
        exportButton.addActionListener(e -> exportOutputLog());
        card.add(exportButton);

        card.add(Box.createVerticalGlue());

        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHelpPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(cardBg);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextArea help = new JTextArea();
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleTextComponent(help);
        help.setBorder(new EmptyBorder(8, 8, 8, 8));
        help.setText(
            "SFORA Help\n\n"
                + "1) Choose a folder from Browse, quick shortcuts, or recent list.\n"
                + "2) Open Operations tab and choose an action.\n"
                + "3) Use Preview before Organize for safer runs.\n"
                + "4) Use Safe Mode for normal desktop folders.\n"
                + "5) Open Rules tab to edit or reload custom rules.\n"
                + "6) Export Activity log for audit or support purposes.\n\n"
                + "Validation and Safety\n"
                + "- The app blocks folder-dependent actions until a folder is selected.\n"
                + "- Safe Mode disables hidden-file scanning to avoid risky moves.\n"
                + "- Undo options are available via action log history.\n"
        );

        JScrollPane scroll = new JScrollPane(help);
        scroll.setBorder(BorderFactory.createLineBorder(border));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        ModernButton openReport = createButton("Open Report", null, accentSoft, textPrimary);
        openReport.addActionListener(e -> openReportFile());

        ModernButton openRules = createButton("Open Rules", null, accentSoft, textPrimary);
        openRules.addActionListener(e -> openRulesFile());

        actions.add(openReport);
        actions.add(openRules);

        card.add(scroll, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private void switchPage(String page) {
        pageLayout.show(pageContainer, page);
        if (PAGE_HOME.equals(page)) {
            titleLabel.setText("Dashboard");
            subtitleLabel.setText("Organize, preview, and manage file workflows from a single view.");
        } else if (PAGE_SETTINGS.equals(page)) {
            titleLabel.setText("Settings");
            subtitleLabel.setText("Configure theme and workspace behavior.");
        } else {
            titleLabel.setText("Help / Info");
            subtitleLabel.setText("Guidance for safe and efficient operations.");
        }
    }

    private void updateSettingsSummary() {
        if (settingsSummaryLabel == null || safeModeCheck == null || includeHiddenCheck == null || bigFileSpinner == null) {
            return;
        }
        settingsSummaryLabel.setText(
            "Mode=" + modeCombo.getSelectedItem()
                + " | Safe=" + safeModeCheck.isSelected()
                + " | Hidden=" + includeHiddenCheck.isSelected()
                + " | BigFileMB=" + bigFileSpinner.getValue()
        );
    }

    private void addActionButton(String title, Icon icon, Color bg, Color fg, String tooltip, Runnable action) {
        ModernButton button = createButton(title, icon, bg, fg);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        actionPanel.add(button);
    }

    private ModernButton createButton(String text, Icon icon, Color bg, Color fg) {
        ModernButton button = new ModernButton(text, icon);
        button.setBackgroundColor(bg);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JLabel makeFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(textSoft);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void applyPalette(boolean dark) {
        if (dark) {
            appBg = new Color(15, 18, 24);
            cardBg = new Color(27, 32, 42);
            panelBg = new Color(21, 25, 33);
            sidebarBg = new Color(20, 23, 31);
            accent = new Color(52, 115, 255);
            accentSoft = new Color(44, 54, 74);
            textPrimary = new Color(243, 247, 255);
            textSoft = new Color(157, 173, 199);
            border = new Color(58, 68, 91);
            success = new Color(58, 180, 120);
            warning = new Color(178, 77, 71);
        } else {
            appBg = new Color(237, 242, 248);
            cardBg = new Color(255, 255, 255);
            panelBg = new Color(246, 248, 252);
            sidebarBg = new Color(229, 236, 245);
            accent = new Color(33, 102, 230);
            accentSoft = new Color(218, 227, 240);
            textPrimary = new Color(34, 44, 62);
            textSoft = new Color(91, 103, 127);
            border = new Color(191, 203, 223);
            success = new Color(44, 153, 102);
            warning = new Color(178, 77, 71);
        }
    }

    private void refreshThemeStyles() {
        if (root != null) {
            root.setBackground(appBg);
        }
        if (statusBadge != null) {
            statusBadge.setBackground(new Color(accentSoft.getRGB()));
            statusBadge.setForeground(textPrimary);
        }
    }

    private void chooseFolder() {
        JFileChooser picker = new JFileChooser();
        if (selectedFolder != null && selectedFolder.exists()) {
            picker.setCurrentDirectory(selectedFolder);
        }
        picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (picker.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setSelectedFolder(picker.getSelectedFile(), "browse dialog");
        }
    }

    private void selectKnownFolder(String label, Path folderPath) {
        File knownFolder = folderPath.toFile();
        if (knownFolder.exists() && knownFolder.isDirectory()) {
            setSelectedFolder(knownFolder, label);
        } else {
            appendLog(label + " folder not found on this machine.");
        }
    }

    private void setSelectedFolder(File folder, String source) {
        selectedFolder = folder;
        folderPathBox.setText(folder.getAbsolutePath());
        rememberRecentFolder(folder.getAbsolutePath());
        refreshRecentFolderCombo();
        appendLog("Selected folder from " + source + ": " + folder.getAbsolutePath());
        updateFolderInsightsAsync();
    }

    private void applySorterOptions() {
        boolean safeMode = safeModeCheck.isSelected();
        boolean includeHidden = includeHiddenCheck.isSelected();
        sorter.configureScanOptions(safeMode, includeHidden);
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(panelBg);
        comboBox.setForeground(textPrimary);
        comboBox.setBorder(BorderFactory.createLineBorder(border));
        comboBox.setFocusable(false);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(panelBg);
        spinner.setForeground(textPrimary);
        spinner.setBorder(BorderFactory.createLineBorder(border));
        Component editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(panelBg);
            defaultEditor.getTextField().setForeground(textPrimary);
            defaultEditor.getTextField().setCaretColor(textPrimary);
            defaultEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        }
    }

    private void styleTextComponent(JComponent component) {
        component.setBackground(panelBg);
        component.setForeground(textPrimary);
        component.setBorder(BorderFactory.createLineBorder(border));
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(textPrimary);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkBox.setFocusPainted(false);
    }

    private void confirmOrganizeWithPlan() {
        if (busy) {
            appendLog("Another action is running. Please wait.");
            return;
        }

        if (!validateFolderSelection()) {
            return;
        }

        applySorterOptions();
        String mode = normalizeMode();
        OrganizePlan plan = sorter.buildOrganizePlan(selectedFolder, mode);
        if (plan.isEmpty()) {
            appendLog("Nothing to organize for the selected folder.");
            return;
        }

        if (!showPlanDialog("Confirm Organize", plan, true)) {
            appendLog("Organize action cancelled.");
            return;
        }

        runTask("Organize", true, () -> sorter.organizeFiles(selectedFolder, mode));
    }

    private void showPreviewPlan() {
        if (busy) {
            appendLog("Another action is running. Please wait.");
            return;
        }

        if (!validateFolderSelection()) {
            return;
        }

        applySorterOptions();
        OrganizePlan plan = sorter.buildOrganizePlan(selectedFolder, normalizeMode());
        if (plan.isEmpty()) {
            appendLog("Nothing to preview for the selected folder.");
            return;
        }

        showPlanDialog("Preview Diff", plan, false);
        runTask("Preview", true, () -> sorter.previewChanges(selectedFolder, normalizeMode()));
    }

    private boolean validateFolderSelection() {
        if (selectedFolder == null || !selectedFolder.exists() || !selectedFolder.isDirectory()) {
            JOptionPane.showMessageDialog(
                this,
                "Please choose a valid folder before running this action.",
                "Folder Required",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        return true;
    }

    private String normalizeMode() {
        String mode = (String) modeCombo.getSelectedItem();
        if ("default".equals(mode)) {
            return "standard";
        }
        return mode;
    }

    private boolean showPlanDialog(String title, OrganizePlan plan, boolean confirmable) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(this);
        final boolean[] confirmed = new boolean[] { false };

        JPanel shell = new JPanel(new BorderLayout(10, 10));
        shell.setBorder(new EmptyBorder(12, 12, 12, 12));
        shell.setBackground(appBg);

        JLabel summary = new JLabel("Mode: " + plan.getMode() + " | Planned moves: " + plan.getMoveCount());
        summary.setForeground(textPrimary);
        summary.setFont(new Font("Segoe UI", Font.BOLD, 13));
        shell.add(summary, BorderLayout.NORTH);

        JTextArea diff = new JTextArea();
        diff.setEditable(false);
        diff.setFont(new Font("Consolas", Font.PLAIN, 13));
        diff.setBackground(panelBg);
        diff.setForeground(textPrimary);
        diff.setCaretColor(textPrimary);
        diff.setBorder(new EmptyBorder(10, 10, 10, 10));

        StringBuilder builder = new StringBuilder();
        builder.append(plan.describe()).append('\n');
        int shown = 0;
        for (FileMoveCandidate move : plan.getMoves()) {
            if (shown >= 110) {
                builder.append("... and ").append(plan.getMoveCount() - shown).append(" more files.\n");
                break;
            }
            builder.append("- ")
                .append(move.getSource().getName())
                .append(" -> ")
                .append(move.getRelativeTarget())
                .append('\n');
            shown++;
        }
        diff.setText(builder.toString());
        diff.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(diff);
        scroll.setBorder(BorderFactory.createLineBorder(border));
        shell.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, confirmable ? 2 : 1, 8, 0));
        actions.setOpaque(false);

        if (confirmable) {
            ModernButton confirmBtn = createButton("Proceed", null, accent, textPrimary);
            confirmBtn.addActionListener(e -> {
                confirmed[0] = true;
                dialog.dispose();
            });
            actions.add(confirmBtn);
        }

        ModernButton closeBtn = createButton(confirmable ? "Cancel" : "Close", null, accentSoft, textPrimary);
        closeBtn.addActionListener(e -> dialog.dispose());
        actions.add(closeBtn);

        shell.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(shell);
        dialog.setVisible(true);
        return confirmed[0];
    }

    private void openRulesFile() {
        File rulesFile = new File("rules.txt");
        if (!rulesFile.exists()) {
            appendLog("rules.txt was not found.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            appendLog("Desktop open is not supported on this machine.");
            return;
        }

        try {
            Desktop.getDesktop().open(rulesFile);
            appendLog("Opened rules.txt.");
        } catch (IOException e) {
            appendLog("Could not open rules.txt.");
        }
    }

    private void openRulesEditor() {
        JDialog dialog = new JDialog(this, "Rules Editor", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(840, 560);
        dialog.setLocationRelativeTo(this);

        JPanel shell = new JPanel(new BorderLayout(10, 10));
        shell.setBorder(new EmptyBorder(12, 12, 12, 12));
        shell.setBackground(appBg);

        JLabel help = new JLabel("Format: KEYWORD=value,FOLDER=Target or EXTENSION=value,FOLDER=Target");
        help.setForeground(textSoft);
        help.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shell.add(help, BorderLayout.NORTH);

        JTextArea editor = new JTextArea(loadRulesText());
        editor.setFont(new Font("Consolas", Font.PLAIN, 13));
        editor.setBackground(panelBg);
        editor.setForeground(textPrimary);
        editor.setCaretColor(textPrimary);
        editor.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(BorderFactory.createLineBorder(border));
        shell.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
        actions.setOpaque(false);

        ModernButton saveBtn = createButton("Save", null, accent, textPrimary);
        saveBtn.addActionListener(e -> {
            try {
                Files.writeString(new File("rules.txt").toPath(), editor.getText(), StandardCharsets.UTF_8);
                sorter.reloadRules();
                if (rulesStatsLabel != null) {
                    rulesStatsLabel.setText(sorter.getRulesSummary());
                }
                appendLog("Rules saved and reloaded.");
                dialog.dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not save rules.txt", "Save Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        ModernButton reloadBtn = createButton("Reload", null, accentSoft, textPrimary);
        reloadBtn.addActionListener(e -> {
            editor.setText(loadRulesText());
            sorter.reloadRules();
            if (rulesStatsLabel != null) {
                rulesStatsLabel.setText(sorter.getRulesSummary());
            }
            appendLog("Rules reloaded into editor.");
        });

        ModernButton closeBtn = createButton("Close", null, accentSoft, textPrimary);
        closeBtn.addActionListener(e -> dialog.dispose());

        actions.add(saveBtn);
        actions.add(reloadBtn);
        actions.add(closeBtn);
        shell.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(shell);
        dialog.setVisible(true);
    }

    private String loadRulesText() {
        File rulesFile = new File("rules.txt");
        if (!rulesFile.exists()) {
            return "# rules.txt\n# KEYWORD=assignment,FOLDER=University/Assignments\n# EXTENSION=pdf,FOLDER=Documents/PDFs\n";
        }

        try {
            return Files.readString(rulesFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# Could not read rules.txt\n";
        }
    }

    private void runTask(String title, boolean needsFolder, Runnable job) {
        if (busy) {
            appendLog("Another action is running. Please wait.");
            return;
        }

        if (needsFolder && !validateFolderSelection()) {
            return;
        }

        if (needsFolder && !safeModeCheck.isSelected()) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Safe mode is OFF. This may reorganize developer folders and hidden files. Continue?",
                "Safety Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                appendLog("Action cancelled by user.");
                return;
            }
        }

        setBusy(true, title + " in progress...");
        appendLog("--- " + title + " started ---");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                synchronized (outputCaptureLock) {
                    PrintStream originalOut = System.out;
                    PrintStream originalErr = System.err;
                    PrintStream uiStream = new PrintStream(new OutputStream() {
                        @Override
                        public void write(int b) {
                            appendRaw(Character.toString((char) b));
                        }
                    }, true);

                    try {
                        System.setOut(uiStream);
                        System.setErr(uiStream);
                        job.run();
                    } finally {
                        System.setOut(originalOut);
                        System.setErr(originalErr);
                        uiStream.close();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Idle");
                appendLog("--- " + title + " finished ---\n");
                updateFolderInsightsAsync();
            }
        };
        worker.execute();
    }

    private void setBusy(boolean running, String statusText) {
        busy = running;
        if (progressBar != null) {
            progressBar.setVisible(running);
        }

        statusBadge.setText(running ? "RUNNING" : "IDLE");
        statusBadge.setBackground(running ? success : accentSoft);
        statusBadge.setForeground(textPrimary);

        if (actionPanel != null) {
            for (Component component : actionPanel.getComponents()) {
                component.setEnabled(!running);
            }
        }

        if (modeCombo != null) {
            modeCombo.setEnabled(!running);
        }
        if (bigFileSpinner != null) {
            bigFileSpinner.setEnabled(!running);
        }
        if (safeModeCheck != null) {
            safeModeCheck.setEnabled(!running);
        }
        if (includeHiddenCheck != null) {
            includeHiddenCheck.setEnabled(!running && !safeModeCheck.isSelected());
        }
        if (recentFolderCombo != null) {
            recentFolderCombo.setEnabled(!running);
        }

        appendLog("Status: " + statusText);
    }

    private void openReportFile() {
        File report = new File("organize_report.txt");
        if (!report.exists()) {
            appendLog("No report file yet. Run organize or large-file action first.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            appendLog("Desktop open is not supported on this machine.");
            return;
        }

        try {
            Desktop.getDesktop().open(report);
            appendLog("Opened report file.");
        } catch (IOException e) {
            appendLog("Could not open report file.");
        }
    }

    private void resetWorkspace() {
        if (busy) {
            appendLog("Wait for the current task to finish before resetting.");
            return;
        }

        selectedFolder = null;
        if (folderPathBox != null) {
            folderPathBox.setText("Select a folder to begin");
        }
        if (modeCombo != null) {
            modeCombo.setSelectedItem("hybrid");
        }
        if (safeModeCheck != null) {
            safeModeCheck.setSelected(true);
        }
        if (includeHiddenCheck != null) {
            includeHiddenCheck.setSelected(false);
            includeHiddenCheck.setEnabled(false);
        }
        if (bigFileSpinner != null) {
            bigFileSpinner.setValue(50);
        }
        if (folderStatsLabel != null) {
            folderStatsLabel.setText("No folder selected");
        }
        if (outputBox != null) {
            outputBox.setText("");
        }
        updateSettingsSummary();
        appendLog("Workspace state was reset.");
    }

    private void exportOutputLog() {
        if (outputBox == null || outputBox.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "No output to export yet.", "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Activity Log");
        chooser.setSelectedFile(new File("sfora-activity-log.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destination = chooser.getSelectedFile();
            try {
                Files.writeString(destination.toPath(), outputBox.getText(), StandardCharsets.UTF_8);
                appendLog("Activity log exported to: " + destination.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Could not export activity log.", "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void appendLog(String text) {
        if (outputBox == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            outputBox.append(text);
            if (!text.endsWith("\n")) {
                outputBox.append("\n");
            }
            outputBox.setCaretPosition(outputBox.getDocument().getLength());
        });
    }

    private void appendRaw(String text) {
        if (outputBox == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            outputBox.append(text);
            outputBox.setCaretPosition(outputBox.getDocument().getLength());
        });
    }

    private void updateFolderInsightsAsync() {
        if (folderStatsLabel == null) {
            return;
        }
        if (selectedFolder == null) {
            folderStatsLabel.setText("No folder selected");
            return;
        }

        folderStatsLabel.setText("Analyzing folder...");
        boolean includeHidden = includeHiddenCheck != null && includeHiddenCheck.isSelected();
        SwingWorker<FolderInsight, Void> insightWorker = new SwingWorker<>() {
            @Override
            protected FolderInsight doInBackground() {
                return analyzeFolder(selectedFolder, includeHidden);
            }

            @Override
            protected void done() {
                try {
                    FolderInsight insight = get();
                    folderStatsLabel.setText(
                        "Files: " + insight.fileCount
                            + " | Folders: " + insight.folderCount
                            + " | Size: " + readableSize(insight.totalBytes)
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    folderStatsLabel.setText("Folder analysis interrupted");
                } catch (ExecutionException e) {
                    folderStatsLabel.setText("Could not analyze selected folder");
                }
            }
        };
        insightWorker.execute();
    }

    private FolderInsight analyzeFolder(File rootFolder, boolean includeHidden) {
        long files = 0;
        long folders = 0;
        long bytes = 0;

        Deque<File> stack = new ArrayDeque<>();
        stack.push(rootFolder);

        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }

            for (File child : children) {
                if (!includeHidden) {
                    try {
                        if (child.isHidden()) {
                            continue;
                        }
                    } catch (SecurityException ignored) {
                        continue;
                    }
                }

                if (child.isDirectory()) {
                    folders++;
                    stack.push(child);
                } else {
                    files++;
                    bytes += child.length();
                }
            }
        }

        return new FolderInsight(files, folders, bytes);
    }

    private String readableSize(long bytes) {
        double value = bytes;
        String[] units = { "B", "KB", "MB", "GB", "TB" };
        int index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index++;
        }
        return String.format("%.1f %s", value, units[index]);
    }

    private java.util.List<String> loadRecentFolders() {
        java.util.List<String> recent = new ArrayList<>();
        String packed = prefs.get("recentFolders", "");
        if (packed.isBlank()) {
            return recent;
        }

        String[] items = packed.split("\\|\\|");
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                recent.add(trimmed);
            }
        }
        return recent;
    }

    private void rememberRecentFolder(String path) {
        java.util.List<String> recent = loadRecentFolders();
        recent.remove(path);
        recent.add(0, path);

        while (recent.size() > 8) {
            recent.remove(recent.size() - 1);
        }

        prefs.put("recentFolders", String.join("||", recent));
    }

    private void refreshRecentFolderCombo() {
        if (recentFolderCombo == null) {
            return;
        }

        recentFolderCombo.removeAllItems();
        recentFolderCombo.addItem("Recent folders...");
        for (String path : loadRecentFolders()) {
            recentFolderCombo.addItem(path);
        }
        recentFolderCombo.setSelectedIndex(0);
    }

    private static class FolderInsight {
        private final long fileCount;
        private final long folderCount;
        private final long totalBytes;

        private FolderInsight(long fileCount, long folderCount, long totalBytes) {
            this.fileCount = fileCount;
            this.folderCount = folderCount;
            this.totalBytes = totalBytes;
        }
    }

    private static class ModernButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color pressColor;
        private int arc = 12;

        ModernButton(String text, Icon icon) {
            super(text, icon);
            setBorder(new EmptyBorder(9, 12, 9, 12));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (isEnabled()) {
                        animateFlash();
                    }
                }
            });
        }

        void setBackgroundColor(Color color) {
            this.baseColor = color;
            this.hoverColor = mix(color, new Color(255, 255, 255), 0.08f);
            this.pressColor = mix(color, new Color(0, 0, 0), 0.15f);
            repaint();
        }

        private void animateFlash() {
            Color original = hoverColor;
            hoverColor = mix(baseColor, Color.WHITE, 0.16f);
            repaint();
            Timer timer = new Timer(110, e -> {
                hoverColor = original;
                repaint();
            });
            timer.setRepeats(false);
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = baseColor;
            if (!isEnabled()) {
                fill = mix(baseColor, Color.GRAY, 0.35f);
            } else if (getModel().isPressed()) {
                fill = pressColor;
            } else if (getModel().isRollover()) {
                fill = hoverColor;
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();

            super.paintComponent(g);
        }

        private static Color mix(Color a, Color b, float ratio) {
            float inverse = 1f - ratio;
            int r = Math.round(a.getRed() * inverse + b.getRed() * ratio);
            int g = Math.round(a.getGreen() * inverse + b.getGreen() * ratio);
            int bl = Math.round(a.getBlue() * inverse + b.getBlue() * ratio);
            return new Color(clamp(r), clamp(g), clamp(bl));
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }
}
