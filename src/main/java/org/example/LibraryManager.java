package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File; // Import the File class
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// --- DATA CLASSES (No changes) ---
class User {
    private final int id;
    private final String name;
    private final String email;

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    public int getId() { return id; }
    public String getName() { return name; }
}

class Book {
    private final int id;
    private final String title;
    private final String author;
    private final String imagePath;
    private final int borrowedByUserId;

    public Book(int id, String title, String author, String imagePath, int borrowedByUserId) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.imagePath = imagePath;
        this.borrowedByUserId = borrowedByUserId;
    }
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getImagePath() { return imagePath; }
    public boolean isAvailable() { return borrowedByUserId == 0; }
    public int getBorrowedByUserId() { return borrowedByUserId; }
}

// --- DATABASE LOGIC ---
class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:library_v2.db";

    public void initializeDatabase() {
        // --- NEW CODE TO PRINT DATABASE PATH ---
        File dbFile = new File("library_v2.db");
        System.out.println("---------------------------------------------------------");
        System.out.println("Attempting to connect to database at absolute path:");
        System.out.println(dbFile.getAbsolutePath());
        System.out.println("---------------------------------------------------------");

        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL);";
        String createBookTableSql = "CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL, image_path TEXT, borrowed_by_user_id INTEGER DEFAULT 0);";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUserTableSql);
            stmt.execute(createBookTableSql);
        } catch (SQLException e) {
            handleError(e);
        }
    }

    // ... All other DatabaseManager methods remain the same ...
    public User registerUser(String name, String email, String password) {
        String sql = "INSERT INTO users(name, email, password) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return new User(generatedKeys.getInt(1), name, email);
                    }
                }
            }
        } catch (SQLException e) {
            handleError(e);
        }
        return null;
    }

    public User loginUser(String email, String password) {
        String sql = "SELECT id, name, email FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
            }
        } catch (SQLException e) {
            handleError(e);
        }
        return null;
    }

    public String getBorrowerName(int userId) {
        if (userId == 0) return "Available";
        String sql = "SELECT name FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            handleError(e);
        }
        return "Unknown User";
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getString("image_path"), rs.getInt("borrowed_by_user_id")));
            }
        } catch (SQLException e) {
            handleError(e);
        }
        return books;
    }

    public Book getBookById(int bookId) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getString("image_path"), rs.getInt("borrowed_by_user_id"));
            }
        } catch (SQLException e) {
            handleError(e);
        }
        return null;
    }

    public void addBook(String title, String author, String imagePath) {
        String sql = "INSERT INTO books(title, author, image_path) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setString(3, imagePath);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleError(e);
        }
    }

    public void updateBookBorrowStatus(int bookId, int userId) {
        String sql = "UPDATE books SET borrowed_by_user_id = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleError(e);
        }
    }

    public void deleteBook(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void handleError(SQLException e) {
        JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}

// --- UI (VIEW) CLASSES (No changes) ---
class DashboardFrame extends JFrame {
    private final DatabaseManager dbManager;
    private final User currentUser;
    private final JTable bookTable;
    private final DefaultTableModel tableModel;
    private final JLabel imageLabel;
    private static final int BOOK_ID_OFFSET = 10000;

    public DashboardFrame(User user) {
        this.currentUser = user;
        this.dbManager = new DatabaseManager();

        setTitle("Digital Library Dashboard - Welcome, " + currentUser.getName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columnNames = {"S.No.", "Book ID", "Title", "Author", "Status (Borrowed By)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        bookTable = new JTable(tableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateBookDetails();
            }
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        bookTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(bookTable);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Book Details"));
        imageLabel = new JLabel("Select a book to see its cover");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageLabel.setPreferredSize(new Dimension(200, 300));
        imageLabel.setBorder(BorderFactory.createEtchedBorder());
        detailsPanel.add(imageLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton borrowButton = new JButton("Borrow Selected");
        JButton returnButton = new JButton("Return Selected");
        JButton deleteButton = new JButton("Delete Selected");
        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);
        buttonPanel.add(deleteButton);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Book"));
        JTextField titleField = new JTextField(15);
        JTextField authorField = new JTextField(15);
        JTextField imageField = new JTextField(15);
        JButton addButton = new JButton("Add to Library");
        addPanel.add(new JLabel("Title:"));
        addPanel.add(titleField);
        addPanel.add(new JLabel("Author:"));
        addPanel.add(authorField);
        addPanel.add(new JLabel("Image URL:"));
        addPanel.add(imageField);
        addPanel.add(addButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(detailsPanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(addPanel, BorderLayout.NORTH);

        add(mainPanel);

        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            if (title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and Author cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dbManager.addBook(title, author, imageField.getText().trim());
            refreshTable();
            titleField.setText("");
            authorField.setText("");
            imageField.setText("");
        });

        deleteButton.addActionListener(e -> deleteSelectedBook());
        borrowButton.addActionListener(e -> borrowSelectedBook());
        returnButton.addActionListener(e -> returnSelectedBook());

        refreshTable();
    }

    private int getSelectedBookId() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = bookTable.convertRowIndexToModel(selectedRow);
            int displayedId = (int) tableModel.getValueAt(modelRow, 1);
            return displayedId - BOOK_ID_OFFSET;
        }
        return -1;
    }

    private void borrowSelectedBook() {
        int bookId = getSelectedBookId();
        if (bookId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to borrow.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Book book = dbManager.getBookById(bookId);
        if (book != null && !book.isAvailable()) {
            JOptionPane.showMessageDialog(this, "This book is already borrowed.", "Action Error", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        dbManager.updateBookBorrowStatus(bookId, currentUser.getId());
        refreshTable();
    }

    private void returnSelectedBook() {
        int bookId = getSelectedBookId();
        if (bookId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to return.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Book book = dbManager.getBookById(bookId);
        if (book != null && book.isAvailable()) {
            JOptionPane.showMessageDialog(this, "This book is already in the library.", "Action Error", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        dbManager.updateBookBorrowStatus(bookId, 0);
        refreshTable();
    }

    private void deleteSelectedBook() {
        int bookId = getSelectedBookId();
        if (bookId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete this book?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dbManager.deleteBook(bookId);
            refreshTable();
        }
    }

    private void updateBookDetails() {
        int bookId = getSelectedBookId();
        if (bookId == -1) {
            imageLabel.setIcon(null);
            imageLabel.setText("Select a book to see its cover");
            return;
        }

        Book selectedBook = dbManager.getBookById(bookId);

        imageLabel.setIcon(null);
        imageLabel.setText("Loading image...");

        if (selectedBook != null && selectedBook.getImagePath() != null && !selectedBook.getImagePath().isEmpty()) {
            SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    URL imageUrl = new URL(selectedBook.getImagePath());
                    ImageIcon originalIcon = new ImageIcon(imageUrl);
                    Image image = originalIcon.getImage().getScaledInstance(200, 300, Image.SCALE_SMOOTH);
                    return new ImageIcon(image);
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon imageIcon = get();
                        imageLabel.setIcon(imageIcon);
                        imageLabel.setText(null);
                    } catch (Exception e) {
                        imageLabel.setIcon(null);
                        imageLabel.setText("Image not found");
                        System.err.println("Failed to load image: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("No Image Available");
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Book> books = dbManager.getAllBooks();
        int serialNumber = 1;
        for (Book book : books) {
            Object[] row = {
                    serialNumber++,
                    book.getId() + BOOK_ID_OFFSET,
                    book.getTitle(),
                    book.getAuthor(),
                    dbManager.getBorrowerName(book.getBorrowedByUserId())
            };
            tableModel.addRow(row);
        }
    }
}

class LoginFrame extends JFrame {
    private final DatabaseManager dbManager;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final JTextField loginEmailField = new JTextField(20);
    private final JPasswordField loginPasswordField = new JPasswordField(20);

    private final JTextField registerNameField = new JTextField(20);
    private final JTextField registerEmailField = new JTextField(20);
    private final JPasswordField registerPasswordField = new JPasswordField(20);

    public LoginFrame() {
        this.dbManager = new DatabaseManager();
        dbManager.initializeDatabase();

        setTitle("Library Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel loginPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        loginPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        loginPanel.add(new JLabel("Email:"));
        loginPanel.add(loginEmailField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(loginPasswordField);

        JPanel loginButtonPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton showRegisterButton = new JButton("Register");
        loginButtonPanel.add(loginButton);
        loginButtonPanel.add(showRegisterButton);

        JPanel loginContainer = new JPanel(new BorderLayout());
        loginContainer.add(loginPanel, BorderLayout.CENTER);
        loginContainer.add(loginButtonPanel, BorderLayout.SOUTH);

        JPanel registerPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        registerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        registerPanel.add(new JLabel("Name:"));
        registerPanel.add(registerNameField);
        registerPanel.add(new JLabel("Email:"));
        registerPanel.add(registerEmailField);
        registerPanel.add(new JLabel("Password:"));
        registerPanel.add(registerPasswordField);

        JPanel registerButtonPanel = new JPanel();
        JButton registerButton = new JButton("Create Account");
        JButton showLoginButton = new JButton("Back to Login");
        registerButtonPanel.add(registerButton);
        registerButtonPanel.add(showLoginButton);

        JPanel registerContainer = new JPanel(new BorderLayout());
        registerContainer.add(registerPanel, BorderLayout.CENTER);
        registerContainer.add(registerButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(loginContainer, "LOGIN");
        mainPanel.add(registerContainer, "REGISTER");
        add(mainPanel);

        showRegisterButton.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        showLoginButton.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());
    }

    private void handleLogin() {
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email and password cannot be empty.", "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        User user = dbManager.loginUser(email, password);
        if (user != null) {
            this.dispose();
            SwingUtilities.invokeLater(() -> new DashboardFrame(user).setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Invalid email or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String name = registerNameField.getText().trim();
        String email = registerEmailField.getText().trim();
        String password = new String(registerPasswordField.getPassword());
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        User newUser = dbManager.registerUser(name, email, password);
        if (newUser != null) {
            JOptionPane.showMessageDialog(this, "Registration successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "LOGIN");
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed. Email might already be in use.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}


/**
 * The main entry point of the application.
 */
public class LibraryManager {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

