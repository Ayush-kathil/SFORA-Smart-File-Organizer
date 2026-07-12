import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

public class CustomFolderBrowser extends JDialog {
    private File selectedFolder = null;
    private File currentDirectory;
    private DefaultListModel<File> listModel;
    private JList<File> fileList;
    private JLabel pathLabel;

    public CustomFolderBrowser(JFrame parent, File startDir, Color bg, Color cardBg, Color accent, Color textPrimary, Color textSoft) {
        super(parent, "Select Directory", true);
        setSize(600, 450);
        setLocationRelativeTo(parent);
        
        currentDirectory = (startDir != null && startDir.exists()) ? startDir : new File(System.getProperty("user.home"));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top bar
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(bg);
        
        JButton upBtn = new JButton("UP");
        upBtn.setFocusPainted(false);
        upBtn.setBackground(cardBg);
        upBtn.setForeground(textPrimary);
        upBtn.addActionListener(e -> navigateUp());

        pathLabel = new JLabel(currentDirectory.getAbsolutePath());
        pathLabel.setForeground(textSoft);
        pathLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        topPanel.add(upBtn, BorderLayout.WEST);
        topPanel.add(pathLabel, BorderLayout.CENTER);

        // Center List
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setBackground(cardBg);
        fileList.setForeground(textPrimary);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileList.setCellRenderer(new CustomListCellRenderer(textPrimary, accent, textSoft));
        
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && fileList.getSelectedValue() != null) {
                File sel = fileList.getSelectedValue();
                if (sel.isDirectory()) {
                    pathLabel.setText(sel.getAbsolutePath());
                }
            }
        });

        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    File sel = fileList.getSelectedValue();
                    if (sel != null && sel.isDirectory()) {
                        loadDirectory(sel);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(BorderFactory.createLineBorder(bg.darker(), 1));

        // Bottom Bar
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBackground(bg);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(cardBg);
        cancelBtn.setForeground(textPrimary);
        cancelBtn.addActionListener(e -> dispose());

        JButton selectBtn = new JButton("Select Folder");
        selectBtn.setBackground(accent);
        selectBtn.setForeground(textPrimary);
        selectBtn.addActionListener(e -> {
            File sel = fileList.getSelectedValue();
            if (sel != null && sel.isDirectory()) {
                selectedFolder = sel;
            } else {
                selectedFolder = currentDirectory;
            }
            dispose();
        });

        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(cancelBtn);
        bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        bottomPanel.add(selectBtn);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);
        loadDirectory(currentDirectory);
    }

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            loadDirectory(parent);
        }
    }

    private void loadDirectory(File dir) {
        currentDirectory = dir;
        pathLabel.setText(dir.getAbsolutePath());
        listModel.clear();
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : files) {
                listModel.addElement(f);
            }
        }
    }

    public File getSelectedFolder() {
        return selectedFolder;
    }

    class CustomListCellRenderer extends JLabel implements ListCellRenderer<File> {
        private Color textPrimary, accent, textSoft;

        public CustomListCellRenderer(Color textPrimary, Color accent, Color textSoft) {
            this.textPrimary = textPrimary;
            this.accent = accent;
            this.textSoft = textSoft;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getName() + (value.isDirectory() ? " (Folder)" : ""));
            if (isSelected) {
                setBackground(accent);
                setForeground(Color.WHITE);
            } else {
                setBackground(list.getBackground());
                setForeground(value.isDirectory() ? textPrimary : textSoft);
            }
            return this;
        }
    }
}
