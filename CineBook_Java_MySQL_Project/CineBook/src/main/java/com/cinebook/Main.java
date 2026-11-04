package com.cinebook;

import com.cinebook.config.DBConnection;
import com.cinebook.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static long currentUserId = -1;
    private static String currentUserName = "";

    public static void main(String[] args) {
        System.out.println("\n======================================");
        System.out.println("       CINEBOOK MOVIE TICKETS");
        System.out.println("======================================");
        try (Connection ignored = DBConnection.getConnection()) {
            bootstrapAdmin();
            mainMenu();
        } catch (SQLException e) {
            System.out.println("\nDatabase connection failed: " + e.getMessage());
            System.out.println("Check that MySQL is running and DB_URL, DB_USER and DB_PASSWORD are correct.");
        }
    }

    private static void bootstrapAdmin() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement check = c.prepareStatement("SELECT admin_id FROM admins WHERE email=?")) {
            check.setString(1, "admin@cinebook.local");
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    try (PreparedStatement add = c.prepareStatement(
                            "INSERT INTO admins(email,password_hash) VALUES(?,?)")) {
                        add.setString(1, "admin@cinebook.local");
                        add.setString(2, PasswordUtil.hash("Admin@123"));
                        add.executeUpdate();
                    }
                    System.out.println("Default admin created: admin@cinebook.local / Admin@123");
                    System.out.println("Change this password before using the project beyond a demo.");
                }
            }
        }
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("\n1. Register\n2. User Login\n3. Admin Login\n4. Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> adminLogin();
                case 4 -> { System.out.println("Thank you for using CineBook!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void register() {
        String name = readText("Full name: ");
        String email = readText("Email: ").toLowerCase(Locale.ROOT);
        String phone = readText("Phone (optional): ");
        String password = readText("Password: ");
        if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || password.length() < 8) {
            System.out.println("Enter a name, valid email and password of at least 8 characters.");
            return;
        }
        String sql = "INSERT INTO users(full_name,email,phone,password_hash) VALUES(?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, phone);
            ps.setString(4, PasswordUtil.hash(password)); ps.executeUpdate();
            System.out.println("Registration successful. Please log in.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("That email is already registered.");
        } catch (SQLException e) { System.out.println("Registration failed: " + e.getMessage()); }
    }

    private static void login() {
        String email = readText("Email: ").toLowerCase(Locale.ROOT);
        String password = readText("Password: ");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT user_id,full_name,password_hash FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    currentUserId = rs.getLong("user_id"); currentUserName = rs.getString("full_name");
                    userMenu();
                } else System.out.println("Invalid email or password.");
            }
        } catch (SQLException e) { System.out.println("Login failed: " + e.getMessage()); }
    }

    private static void userMenu() {
        while (currentUserId != -1) {
            System.out.println("\nWelcome, " + currentUserName);
            System.out.println("1. View movies and shows\n2. Search movies\n3. Book tickets");
            System.out.println("4. My bookings\n5. Cancel booking\n6. Logout");
            switch (readInt("Choose: ")) {
                case 1 -> listShows(null);
                case 2 -> listShows(readText("Movie title contains: "));
                case 3 -> bookTickets();
                case 4 -> myBookings();
                case 5 -> cancelBooking();
                case 6 -> { currentUserId = -1; currentUserName = ""; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void listShows(String search) {
        String sql = "SELECT s.show_id,m.title,m.genre,m.language,t.theatre_name,sc.screen_name," +
                "s.show_time,s.ticket_price FROM shows s JOIN movies m ON s.movie_id=m.movie_id " +
                "JOIN screens sc ON s.screen_id=sc.screen_id JOIN theatres t ON sc.theatre_id=t.theatre_id " +
                "WHERE s.status='OPEN' AND s.show_time>NOW() " +
                (search == null ? "" : "AND m.title LIKE ? ") + "ORDER BY s.show_time";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (search != null) ps.setString(1, "%" + search + "%");
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nID | Movie | Genre | Theatre / Screen | Show time | Price");
                while (rs.next()) System.out.printf("%d | %s | %s | %s / %s | %s | Rs.%s%n",
                    rs.getLong("show_id"), rs.getString("title"), rs.getString("genre"),
                    rs.getString("theatre_name"), rs.getString("screen_name"),
                    rs.getTimestamp("show_time"), rs.getBigDecimal("ticket_price"));
            }
        } catch (SQLException e) { System.out.println("Could not load shows: " + e.getMessage()); }
    }

    private static void bookTickets() {
        listShows(null);
        long showId = readLong("Enter show ID: ");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal price;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT ticket_price FROM shows WHERE show_id=? AND status='OPEN' AND show_time>NOW()")) {
                    p.setLong(1, showId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) { System.out.println("Show not found or no longer open."); c.rollback(); return; }
                        price = r.getBigDecimal(1);
                    }
                }
                List<Long> seatIds = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT show_seat_id,seat_label,status FROM show_seats WHERE show_id=? ORDER BY seat_label FOR UPDATE")) {
                    p.setLong(1, showId);
                    try (ResultSet r = p.executeQuery()) {
                        System.out.println("\nAvailable seats are marked AVAILABLE:");
                        while (r.next()) {
                            System.out.printf("%s [%s]  ", r.getString("seat_label"), r.getString("status"));
                            if ("AVAILABLE".equals(r.getString("status"))) seatIds.add(r.getLong("show_seat_id"));
                        }
                        System.out.println();
                    }
                }
                String requested = readText("Enter seat labels separated by commas (example A1,A2): ").toUpperCase(Locale.ROOT);
                List<String> labels = Arrays.stream(requested.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
                if (labels.isEmpty()) { System.out.println("No seats selected."); c.rollback(); return; }
                List<Long> chosen = new ArrayList<>();
                for (String label : labels) {
                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT show_seat_id FROM show_seats WHERE show_id=? AND seat_label=? AND status='AVAILABLE' FOR UPDATE")) {
                        p.setLong(1, showId); p.setString(2, label);
                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next()) { System.out.println("Seat " + label + " is unavailable or invalid."); c.rollback(); return; }
                            chosen.add(r.getLong(1));
                        }
                    }
                }
                BigDecimal total = price.multiply(BigDecimal.valueOf(chosen.size()));
                String code = "CB" + System.currentTimeMillis();
                long bookingId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO bookings(booking_code,user_id,show_id,total_amount) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, code); p.setLong(2, currentUserId); p.setLong(3, showId); p.setBigDecimal(4, total);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) { keys.next(); bookingId = keys.getLong(1); }
                }
                for (long seatId : chosen) {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE show_seats SET status='BOOKED' WHERE show_seat_id=? AND status='AVAILABLE'")) {
                        p.setLong(1, seatId);
                        if (p.executeUpdate() != 1) throw new SQLException("A selected seat was just booked. Please retry.");
                    }
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO booking_details(booking_id,show_seat_id,seat_price) VALUES(?,?,?)")) {
                        p.setLong(1, bookingId); p.setLong(2, seatId); p.setBigDecimal(3, price); p.executeUpdate();
                    }
                }
                c.commit();
                System.out.println("\nBOOKING CONFIRMED");
                System.out.println("Booking code: " + code + "\nSeats: " + String.join(", ", labels) + "\nTotal: Rs." + total);
                System.out.println("Payment is simulated for this college project; no real payment was taken.");
            } catch (Exception e) {
                c.rollback(); System.out.println("Booking failed: " + e.getMessage());
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { System.out.println("Database error: " + e.getMessage()); }
    }

    private static void myBookings() {
        String sql = "SELECT b.booking_code,m.title,s.show_time,b.total_amount,b.booking_status," +
                "GROUP_CONCAT(ss.seat_label ORDER BY ss.seat_label) seats FROM bookings b " +
                "JOIN shows s ON b.show_id=s.show_id JOIN movies m ON s.movie_id=m.movie_id " +
                "JOIN booking_details bd ON b.booking_id=bd.booking_id JOIN show_seats ss ON bd.show_seat_id=ss.show_seat_id " +
                "WHERE b.user_id=? GROUP BY b.booking_id ORDER BY b.booked_at DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, currentUserId);
            try (ResultSet r = p.executeQuery()) {
                System.out.println("\nBooking code | Movie | Show time | Seats | Total | Status");
                while (r.next()) System.out.printf("%s | %s | %s | %s | Rs.%s | %s%n",
                    r.getString("booking_code"), r.getString("title"), r.getTimestamp("show_time"),
                    r.getString("seats"), r.getBigDecimal("total_amount"), r.getString("booking_status"));
            }
        } catch (SQLException e) { System.out.println("Could not load bookings: " + e.getMessage()); }
    }

    private static void cancelBooking() {
        String code = readText("Booking code to cancel: ").toUpperCase(Locale.ROOT);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long bookingId, showId;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT booking_id,show_id FROM bookings WHERE booking_code=? AND user_id=? AND booking_status='CONFIRMED' FOR UPDATE")) {
                    p.setString(1, code); p.setLong(2, currentUserId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) { System.out.println("Confirmed booking not found."); c.rollback(); return; }
                        bookingId = r.getLong(1); showId = r.getLong(2);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE show_seats ss JOIN booking_details bd ON ss.show_seat_id=bd.show_seat_id " +
                        "SET ss.status='AVAILABLE' WHERE bd.booking_id=?")) {
                    p.setLong(1, bookingId); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE bookings SET booking_status='CANCELLED' WHERE booking_id=?")) {
                    p.setLong(1, bookingId); p.executeUpdate();
                }
                c.commit(); System.out.println("Booking " + code + " cancelled. Seats are available again.");
            } catch (Exception e) { c.rollback(); System.out.println("Cancellation failed: " + e.getMessage()); }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { System.out.println("Database error: " + e.getMessage()); }
    }

    private static void adminLogin() {
        String email = readText("Admin email: ");
        String password = readText("Admin password: ");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT password_hash FROM admins WHERE email=?")) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next() && PasswordUtil.verify(password, r.getString(1))) adminMenu();
                else System.out.println("Invalid admin credentials.");
            }
        } catch (SQLException e) { System.out.println("Admin login failed: " + e.getMessage()); }
    }

    private static void adminMenu() {
        while (true) {
            System.out.println("\nADMIN MENU\n1. List movies\n2. Add movie\n3. View bookings\n4. Logout");
            switch (readInt("Choose: ")) {
                case 1 -> {
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement p = c.prepareStatement("SELECT movie_id,title,genre,language,duration_minutes,rating FROM movies WHERE active=TRUE");
                         ResultSet r = p.executeQuery()) {
                        while (r.next()) System.out.printf("%d | %s | %s | %s | %d min | %s%n",
                            r.getLong(1), r.getString(2), r.getString(3), r.getString(4), r.getInt(5), r.getBigDecimal(6));
                    } catch (SQLException e) { System.out.println(e.getMessage()); }
                }
                case 2 -> addMovie();
                case 3 -> {
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement p = c.prepareStatement(
                            "SELECT b.booking_code,u.full_name,m.title,b.total_amount,b.booking_status FROM bookings b " +
                            "JOIN users u ON b.user_id=u.user_id JOIN shows s ON b.show_id=s.show_id " +
                            "JOIN movies m ON s.movie_id=m.movie_id ORDER BY b.booked_at DESC");
                         ResultSet r = p.executeQuery()) {
                        while (r.next()) System.out.printf("%s | %s | %s | Rs.%s | %s%n",
                            r.getString(1), r.getString(2), r.getString(3), r.getBigDecimal(4), r.getString(5));
                    } catch (SQLException e) { System.out.println(e.getMessage()); }
                }
                case 4 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addMovie() {
        String title = readText("Title: "), genre = readText("Genre: "), language = readText("Language: ");
        int duration = readInt("Duration in minutes: ");
        String ratingText = readText("Rating (0-10): ");
        String description = readText("Short description: ");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                "INSERT INTO movies(title,genre,language,duration_minutes,rating,description) VALUES(?,?,?,?,?,?)")) {
            p.setString(1,title); p.setString(2,genre); p.setString(3,language); p.setInt(4,duration);
            p.setBigDecimal(5,new BigDecimal(ratingText)); p.setString(6,description);
            p.executeUpdate(); System.out.println("Movie added.");
        } catch (Exception e) { System.out.println("Could not add movie: " + e.getMessage()); }
    }

    private static String readText(String prompt) { System.out.print(prompt); return in.nextLine().trim(); }
    private static int readInt(String prompt) {
        while (true) { try { return Integer.parseInt(readText(prompt)); }
            catch (NumberFormatException e) { System.out.println("Enter a whole number."); } }
    }
    private static long readLong(String prompt) {
        while (true) { try { return Long.parseLong(readText(prompt)); }
            catch (NumberFormatException e) { System.out.println("Enter a valid ID."); } }
    }
}
