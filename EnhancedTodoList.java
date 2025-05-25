

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;    
import java.util.HashMap; 
import java.io.*;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnhancedTodoList extends JFrame {
  
    private DefaultListModel<Task> taskListModel;
    private JList<Task> taskList;
    private JTextField inputField;
    private JButton addButton, removeButton, doneButton, themeButton;
    private JButton removeAllButton, removeCompletedButton, removeSelectedButton;
    private JTextArea historyArea;
    private JScrollPane historyScroll, taskScroll;
    private JComboBox<Priority> priorityCombo;
    private JComboBox<Category> categoryCombo;
    private JComboBox<String> filterCombo;
    private JProgressBar progressBar;
    private JLabel statsLabel, timeLabel;
    private JPopupMenu contextMenu;
    
    
    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private File tasksFile = new File("enhanced_tasks.txt");
    private File historyFile = new File("enhanced_history.txt");
    private Timer clockTimer;
    
    
    private boolean isDarkTheme = false;
    private Color primaryColor = new Color(74, 144, 226);
    private Color secondaryColor = new Color(245, 247, 250);
    private Color accentColor = new Color(16, 185, 129);
    
    
    private Timer animationTimer;
    private int animationStep = 0;

    
    enum Priority {
        LOW(" Low", new Color(34, 197, 94)),
        MEDIUM(" Medium", new Color(251, 191, 36)),
        HIGH(" High", new Color(239, 68, 68));
        
        String display;
        Color color;
        Priority(String display, Color color) {
            this.display = display;
            this.color = color;
        }
        
        @Override
        public String toString() { return display; }
    }
    
    enum Category {
        WORK(" Work", new Color(59, 130, 246)),
        PERSONAL(" Personal", new Color(168, 85, 247)),
        SHOPPING(" Shopping", new Color(34, 197, 94)),
        HEALTH(" Health", new Color(239, 68, 68)),
        STUDY(" Study", new Color(251, 191, 36)),
        OTHER(" Other", new Color(107, 114, 128));
        
        String display;
        Color color;
        Category(String display, Color color) {
            this.display = display;
            this.color = color;
        }
        
        @Override
        public String toString() { return display; }
    }

    
    static class Task implements Serializable {
        String content;
        Priority priority;
        Category category;
        boolean completed;
        String createdAt;
        String completedAt;
        String dueDate;
        
        Task(String content, Priority priority, Category category) {
            this.content = content;
            this.priority = priority;
            this.category = category;
            this.completed = false;
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            this.dueDate = "";
        }
        
        @Override
        public String toString() {
            String status = completed ? "" : "";
            return String.format("%s [%s] %s - %s", 
                status, priority.display.split(" ")[1], category.display, content);
        }
    }

    public EnhancedTodoList() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
        setupContextMenu();
        loadData();
        startClock();
        setVisible(true);
        
       
        startWelcomeAnimation();
    }

    private void initializeComponents() {
        setTitle(" Enhanced Todo List - Productivity Master");
        setSize(950, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        inputField = new JTextField();
        priorityCombo = new JComboBox<>(Priority.values());
        categoryCombo = new JComboBox<>(Category.values());
        filterCombo = new JComboBox<>(new String[]{"All Tasks", "Completed", "Pending", "High Priority", "Work", "Personal"});
        
        addButton = new JButton(" Add Task");
        removeButton = new JButton(" Remove Selected");
        removeAllButton = new JButton(" Clear All");
        removeCompletedButton = new JButton(" Clear Completed");
        removeSelectedButton = new JButton(" Delete Multiple");
        doneButton = new JButton(" Complete");
        themeButton = new JButton(" Dark Mode");
        
        historyArea = new JTextArea();
        progressBar = new JProgressBar();
        statsLabel = new JLabel(" Tasks: 0 | Completed: 0 | Pending: 0");
        timeLabel = new JLabel();
        
       
        styleComponents();
    }

    private void styleComponents() {
        
        getContentPane().setBackground(secondaryColor);
        
        
        taskList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taskList.setBackground(Color.WHITE);
        taskList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        taskList.setSelectionBackground(new Color(219, 234, 254));
        taskList.setCellRenderer(new TaskRenderer());
        
       
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(createRoundedBorder());
        inputField.setPreferredSize(new Dimension(0, 35));
        
        
        priorityCombo.setRenderer(new PriorityRenderer());
        categoryCombo.setRenderer(new CategoryRenderer());
        
        
        styleButton(addButton, accentColor);
        styleButton(doneButton, new Color(34, 197, 94));
        styleButton(removeButton, new Color(239, 68, 68));
        styleButton(removeAllButton, new Color(220, 38, 38));
        styleButton(removeCompletedButton, new Color(251, 146, 60));
        styleButton(removeSelectedButton, new Color(185, 28, 28));
        styleButton(themeButton, primaryColor);
        
        
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        historyArea.setBackground(new Color(249, 250, 251));
        historyArea.setEditable(false);
        historyArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(accentColor);
        progressBar.setBorder(createRoundedBorder());
        
        
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timeLabel.setForeground(primaryColor);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
       
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        
        JPanel leftPanel = createTaskPanel();
        mainPanel.add(leftPanel);
        
        
        JPanel rightPanel = createHistoryPanel();
        mainPanel.add(rightPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        
       
        JPanel bottomPanel = createInputPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(primaryColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
       
        JLabel titleLabel = new JLabel(" Enhanced Todo List");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
      
        JPanel rightHeaderPanel = new JPanel(new GridLayout(2, 1));
        rightHeaderPanel.setOpaque(false);
        rightHeaderPanel.add(timeLabel);
        rightHeaderPanel.add(themeButton);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeaderPanel, BorderLayout.EAST);
        
        return headerPanel;
    }

    private JPanel createTaskPanel() {
        JPanel taskPanel = new JPanel(new BorderLayout(0, 10));
        taskPanel.setBackground(secondaryColor);
        
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(secondaryColor);
        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterCombo);
        
        
        taskScroll = new JScrollPane(taskList);
        taskScroll.setBorder(createRoundedBorder());
        taskScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        customizeScrollBar(taskScroll);
        
        
        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        progressPanel.setBackground(secondaryColor);
        progressPanel.add(progressBar);
        progressPanel.add(statsLabel);
        
        
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonPanel.setBackground(secondaryColor);
        buttonPanel.add(doneButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(removeCompletedButton);
        buttonPanel.add(removeAllButton);
        
        taskPanel.add(filterPanel, BorderLayout.NORTH);
        taskPanel.add(taskScroll, BorderLayout.CENTER);
        
        
        JPanel bottomSection = new JPanel(new BorderLayout(0, 10));
        bottomSection.setBackground(secondaryColor);
        bottomSection.add(progressPanel, BorderLayout.NORTH);
        bottomSection.add(buttonPanel, BorderLayout.CENTER);
        
        taskPanel.add(bottomSection, BorderLayout.SOUTH);
        
        
        taskPanel.setBorder(BorderFactory.createTitledBorder(
            createRoundedBorder(), " Your Tasks", 
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), primaryColor));
        
        return taskPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(secondaryColor);
        
        historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(createRoundedBorder());
        customizeScrollBar(historyScroll);
        
        historyPanel.add(historyScroll, BorderLayout.CENTER);
        
        
        historyPanel.setBorder(BorderFactory.createTitledBorder(
            createRoundedBorder(), " Activity History", 
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), primaryColor));
        
        return historyPanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(secondaryColor);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 15, 10));
        
       
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        controlsPanel.setBackground(secondaryColor);
        controlsPanel.add(new JLabel("Priority:"));
        controlsPanel.add(priorityCombo);
        controlsPanel.add(new JLabel("Category:"));
        controlsPanel.add(categoryCombo);
        
        inputPanel.add(controlsPanel, BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);
        
        return inputPanel;
    }

    private void setupContextMenu() {
        contextMenu = new JPopupMenu();
        
        JMenuItem deleteItem = new JMenuItem(" Delete Task");
        JMenuItem completeItem = new JMenuItem(" Mark Complete");
        JMenuItem editItem = new JMenuItem(" Edit Task");
        JMenuItem duplicateItem = new JMenuItem(" Duplicate Task");
        JMenuItem moveUpItem = new JMenuItem(" Move Up");
        JMenuItem moveDownItem = new JMenuItem(" Move Down");
        
        
        deleteItem.addActionListener(e -> removeSelectedTasks());
        completeItem.addActionListener(e -> markDone());
        editItem.addActionListener(e -> editSelectedTask());
        duplicateItem.addActionListener(e -> duplicateSelectedTask());
        moveUpItem.addActionListener(e -> moveTaskUp());
        moveDownItem.addActionListener(e -> moveTaskDown());
        
        contextMenu.add(completeItem);
        contextMenu.add(editItem);
        contextMenu.addSeparator();
        contextMenu.add(duplicateItem);
        contextMenu.add(moveUpItem);
        contextMenu.add(moveDownItem);
        contextMenu.addSeparator();
        contextMenu.add(deleteItem);
        
        
        taskList.setComponentPopupMenu(contextMenu);
    }

    private void setupEventListeners() {
        
        addButton.addActionListener(e -> addTask());
        removeButton.addActionListener(e -> removeSelectedTasks());
        removeAllButton.addActionListener(e -> removeAllTasks());
        removeCompletedButton.addActionListener(e -> removeCompletedTasks());
        doneButton.addActionListener(e -> markDone());
        themeButton.addActionListener(e -> toggleTheme());
        
        
        filterCombo.addActionListener(e -> applyFilter());
        
        
        inputField.addActionListener(e -> addTask());
        
        
        taskList.addListSelectionListener(e -> updateButtonStates());
        
        
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedTask();
                }
            }
        });
        
        
        setupKeyboardShortcuts();
        
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
                System.exit(0);
            }
        });
        
        
        addHoverEffects();
    }

    private void setupKeyboardShortcuts() {
        
        taskList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedTasks();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    markDone();
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    editSelectedTask();
                }
            }
        });
        
        
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
                    taskList.setSelectionInterval(0, taskListModel.getSize() - 1);
                }
            }
        });
    }

    private void addTask() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            Priority priority = (Priority) priorityCombo.getSelectedItem();
            Category category = (Category) categoryCombo.getSelectedItem();
            
            Task task = new Task(text, priority, category);
            taskListModel.addElement(task);
            
            logHistory(" ADDED", task.toString(), getCurrentTime());
            inputField.setText("");
            updateStats();
            saveData();
            
            
            animateTaskAddition();
        }
    }

    private void removeSelectedTasks() {
        int[] selectedIndices = taskList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select task(s) to remove.", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String message = selectedIndices.length == 1 ? 
                "Are you sure you want to delete this task?" :
                "Are you sure you want to delete " + selectedIndices.length + " tasks?";
        
        int choice = JOptionPane.showConfirmDialog(this, message, 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                Task task = taskListModel.getElementAt(selectedIndices[i]);
                taskListModel.remove(selectedIndices[i]);
                logHistory(" REMOVED", task.toString(), getCurrentTime());
            }
            
            updateStats();
            saveData();
            animateTaskRemoval();
        }
    }

    private void removeAllTasks() {
        if (taskListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tasks to remove.", 
                    "Empty List", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int choice = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove ALL tasks?\nThis action cannot be undone!", 
                "Clear All Tasks", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            int taskCount = taskListModel.getSize();
            taskListModel.clear();
            logHistory("🧹 CLEARED ALL", taskCount + " tasks removed", getCurrentTime());
            updateStats();
            saveData();
            animateTaskRemoval();
        }
    }

    private void removeCompletedTasks() {
        java.util.List<Integer> completedIndices = new ArrayList<>();
        
        for (int i = 0; i < taskListModel.getSize(); i++) {
            if (taskListModel.getElementAt(i).completed) {
                completedIndices.add(i);
            }
        }
        
        if (completedIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No completed tasks to remove.", 
                    "No Completed Tasks", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int choice = JOptionPane.showConfirmDialog(this, 
                "Remove " + completedIndices.size() + " completed task(s)?", 
                "Clear Completed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            
            for (int i = completedIndices.size() - 1; i >= 0; i--) {
                int index = completedIndices.get(i);
                Task task = taskListModel.getElementAt(index);
                taskListModel.remove(index);
                logHistory(" CLEARED COMPLETED", task.toString(), getCurrentTime());
            }
            
            updateStats();
            saveData();
            animateTaskRemoval();
        }
    }

    private void editSelectedTask() {
        int index = taskList.getSelectedIndex();
        if (index != -1) {
            Task task = taskListModel.getElementAt(index);
            String newContent = JOptionPane.showInputDialog(this, 
                    "Edit task:", "Edit Task", JOptionPane.PLAIN_MESSAGE, null, null, task.content)
                    .toString();
            
            if (newContent != null && !newContent.trim().isEmpty()) {
                String oldContent = task.content;
                task.content = newContent.trim();
                taskList.repaint();
                logHistory(" EDITED", "'" + oldContent + "' → '" + task.content + "'", getCurrentTime());
                saveData();
            }
        }
    }

    private void duplicateSelectedTask() {
        int index = taskList.getSelectedIndex();
        if (index != -1) {
            Task originalTask = taskListModel.getElementAt(index);
            Task duplicateTask = new Task("Copy of " + originalTask.content, 
                    originalTask.priority, originalTask.category);
            taskListModel.addElement(duplicateTask);
            logHistory(" DUPLICATED", duplicateTask.toString(), getCurrentTime());
            updateStats();
            saveData();
        }
    }

    private void moveTaskUp() {
        int index = taskList.getSelectedIndex();
        if (index > 0) {
            Task task = taskListModel.remove(index);
            taskListModel.add(index - 1, task);
            taskList.setSelectedIndex(index - 1);
            logHistory(" MOVED UP", task.toString(), getCurrentTime());
            saveData();
        }
    }

    private void moveTaskDown() {
        int index = taskList.getSelectedIndex();
        if (index >= 0 && index < taskListModel.getSize() - 1) {
            Task task = taskListModel.remove(index);
            taskListModel.add(index + 1, task);
            taskList.setSelectedIndex(index + 1);
            logHistory(" MOVED DOWN", task.toString(), getCurrentTime());
            saveData();
        }
    }

    private void markDone() {
        int[] selectedIndices = taskList.getSelectedIndices();
        if (selectedIndices.length == 0) return;
        
        for (int index : selectedIndices) {
            Task task = taskListModel.getElementAt(index);
            if (!task.completed) {
                task.completed = true;
                task.completedAt = getCurrentTime();
                logHistory(" COMPLETED", task.toString(), getCurrentTime());
            }
        }
        
        taskList.repaint();
        updateStats();
        saveData();
        animateTaskCompletion();
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (isDarkTheme) {
            
            primaryColor = new Color(37, 99, 235);
            secondaryColor = new Color(31, 41, 55);
            accentColor = new Color(16, 185, 129);
            
            getContentPane().setBackground(new Color(17, 24, 39));
            taskList.setBackground(new Color(55, 65, 81));
            taskList.setForeground(Color.WHITE);
            historyArea.setBackground(new Color(55, 65, 81));
            historyArea.setForeground(Color.WHITE);
            inputField.setBackground(new Color(55, 65, 81));
            inputField.setForeground(Color.WHITE);
            
            themeButton.setText(" Light Mode");
        } else {
            
            primaryColor = new Color(74, 144, 226);
            secondaryColor = new Color(245, 247, 250);
            accentColor = new Color(16, 185, 129);
            
            getContentPane().setBackground(secondaryColor);
            taskList.setBackground(Color.WHITE);
            taskList.setForeground(Color.BLACK);
            historyArea.setBackground(new Color(249, 250, 251));
            historyArea.setForeground(Color.BLACK);
            inputField.setBackground(Color.WHITE);
            inputField.setForeground(Color.BLACK);
            
            themeButton.setText(" Dark Mode");
        }
        
        
        styleComponents();
        repaint();
    }

    private void applyFilter() {
        String filter = (String) filterCombo.getSelectedItem();
        
        updateStats();
    }

    private void updateStats() {
        int total = taskListModel.getSize();
        int completed = 0;
        
        for (int i = 0; i < total; i++) {
            if (taskListModel.getElementAt(i).completed) {
                completed++;
            }
        }
        
        int pending = total - completed;
        statsLabel.setText(String.format(" Tasks: %d | Completed: %d | Pending: %d", 
                                        total, completed, pending));
        
        
        if (total > 0) {
            int progress = (completed * 100) / total;
            progressBar.setValue(progress);
            progressBar.setString(progress + "% Complete");
        } else {
            progressBar.setValue(0);
            progressBar.setString("No tasks");
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = taskList.getSelectedIndex() != -1;
        boolean hasTasks = !taskListModel.isEmpty();
        boolean hasCompletedTasks = false;
        
        
        for (int i = 0; i < taskListModel.getSize(); i++) {
            if (taskListModel.getElementAt(i).completed) {
                hasCompletedTasks = true;
                break;
            }
        }
        
        removeButton.setEnabled(hasSelection);
        doneButton.setEnabled(hasSelection);
        removeAllButton.setEnabled(hasTasks);
        removeCompletedButton.setEnabled(hasCompletedTasks);
    }

    private void startClock() {
        clockTimer = new Timer(1000, e -> {
            timeLabel.setText(" " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        });
        clockTimer.start();
    }

    private void startWelcomeAnimation() {
        animationTimer = new Timer(50, e -> {
            animationStep++;
            if (animationStep > 20) {
                animationTimer.stop();
                animationStep = 0;
            }
            repaint();
        });
        animationTimer.start();
    }

    private void animateTaskAddition() {
        
        Timer flashTimer = new Timer(100, null);
        flashTimer.addActionListener(new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                taskList.setSelectionBackground(count % 2 == 0 ? 
                    new Color(34, 197, 94) : new Color(219, 234, 254));
                count++;
                if (count > 6) {
                    flashTimer.stop();
                    taskList.setSelectionBackground(new Color(219, 234, 254));
                }
            }
        });
        flashTimer.start();
    }

    private void animateTaskCompletion() {
        
        Timer celebrationTimer = new Timer(200, null);
        celebrationTimer.addActionListener(new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setForeground(count % 2 == 0 ? 
                    new Color(34, 197, 94) : accentColor);
                count++;
                if (count > 4) {
                    celebrationTimer.stop();
                    progressBar.setForeground(accentColor);
                }
            }
        });
        celebrationTimer.start();
    }

    private void animateTaskRemoval() {
        
        Timer fadeTimer = new Timer(50, null);
        fadeTimer.addActionListener(new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
               
                taskList.setBackground(count % 2 == 0 ? 
                    new Color(255, 245, 245) : Color.WHITE);
                count++;
                if (count > 6) {
                    fadeTimer.stop();
                    taskList.setBackground(Color.WHITE);
                }
            }
        });
        fadeTimer.start();
    }

    private void logHistory(String action, String content, String time) {
        String logEntry = String.format("[%s] %s: %s\n", time, action, content);
        historyArea.append(logEntry);
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
        saveHistory();
    }

    private String getCurrentTime() {
        return formatter.format(new Date());
    }

    private void saveData() {
        saveTasks();
        saveHistory();
    }

    private void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tasksFile))) {
            java.util.List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < taskListModel.getSize(); i++) {
                tasks.add(taskListModel.getElementAt(i));
            }
            oos.writeObject(tasks);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving tasks: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveHistory() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(historyFile))) {
            writer.print(historyArea.getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving history: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        loadTasks();
        loadHistory();
        updateStats();
    }

    @SuppressWarnings("unchecked")
    private void loadTasks() {
        if (!tasksFile.exists()) return;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tasksFile))) {
            java.util.List<Task> tasks = (java.util.List<Task>) ois.readObject();
            taskListModel.clear();
            for (Task task : tasks) {
                taskListModel.addElement(task);
            }
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHistory() {
        if (!historyFile.exists()) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            historyArea.setText(content.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading history: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    
    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
       
        button.setBorder(new RoundedBorder(8));
        button.setContentAreaFilled(false);
        button.setOpaque(true);
    }

    private void addHoverEffects() {
        addButton.addMouseListener(new HoverEffect(addButton, accentColor));
        removeButton.addMouseListener(new HoverEffect(removeButton, new Color(239, 68, 68)));
        removeAllButton.addMouseListener(new HoverEffect(removeAllButton, new Color(220, 38, 38)));
        removeCompletedButton.addMouseListener(new HoverEffect(removeCompletedButton, new Color(251, 146, 60)));
        doneButton.addMouseListener(new HoverEffect(doneButton, new Color(34, 197, 94)));
        themeButton.addMouseListener(new HoverEffect(themeButton, primaryColor));
    }

    private Border createRoundedBorder() {
        return new RoundedBorder(8);
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
    }

    
    private class TaskRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Task) {
                Task task = (Task) value;
                setIcon(null);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                
                if (task.completed) {
                    setForeground(Color.GRAY);
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else {
                    setForeground(task.priority.color);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                
                
                if (task.completed) {
                    setText("<html><strike>" + task.toString() + "</strike></html>");
                }
            }
            
            return this;
        }
    }

    private class PriorityRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Priority) {
                Priority priority = (Priority) value;
                setForeground(priority.color);
                setFont(getFont().deriveFont(Font.BOLD));
            }
            
            return this;
        }
    }

    private class CategoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Category) {
                Category category = (Category) value;
                setForeground(category.color);
                setFont(getFont().deriveFont(Font.BOLD));
            }
            
            return this;
        }
    }

    private class RoundedBorder implements Border {
        private int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 200, 200));
            g2.draw(new RoundRectangle2D.Float(x, y, width-1, height-1, radius, radius));
            g2.dispose();
        }
    }

    private class HoverEffect extends MouseAdapter {
        private JButton button;
        private Color originalColor;
        
        HoverEffect(JButton button, Color originalColor) {
            this.button = button;
            this.originalColor = originalColor;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(originalColor.brighter());
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(originalColor);
        }
    }

    private class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(150, 150, 150);
            trackColor = new Color(230, 230, 230);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, 
                           thumbBounds.width - 4, thumbBounds.height - 4, 5, 5);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
               
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
               
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new EnhancedTodoList();
        });
    }
}