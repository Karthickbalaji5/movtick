CREATE DATABASE IF NOT EXISTS cinebook_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinebook_db;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  phone VARCHAR(20),
  password_hash VARCHAR(300) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admins (
  admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(300) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movies (
  movie_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  genre VARCHAR(60) NOT NULL,
  language VARCHAR(50) NOT NULL,
  duration_minutes INT NOT NULL,
  rating DECIMAL(3,1) DEFAULT 0.0,
  description VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS theatres (
  theatre_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  theatre_name VARCHAR(150) NOT NULL,
  city VARCHAR(100) NOT NULL,
  address VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS screens (
  screen_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  theatre_id BIGINT NOT NULL,
  screen_name VARCHAR(80) NOT NULL,
  total_seats INT NOT NULL,
  CONSTRAINT fk_screen_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(theatre_id),
  UNIQUE (theatre_id, screen_name)
);

CREATE TABLE IF NOT EXISTS shows (
  show_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  movie_id BIGINT NOT NULL,
  screen_id BIGINT NOT NULL,
  show_time DATETIME NOT NULL,
  ticket_price DECIMAL(10,2) NOT NULL,
  status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  CONSTRAINT fk_show_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id),
  CONSTRAINT fk_show_screen FOREIGN KEY (screen_id) REFERENCES screens(screen_id)
);

CREATE TABLE IF NOT EXISTS show_seats (
  show_seat_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  show_id BIGINT NOT NULL,
  seat_label VARCHAR(8) NOT NULL,
  status ENUM('AVAILABLE','BOOKED') NOT NULL DEFAULT 'AVAILABLE',
  CONSTRAINT fk_seat_show FOREIGN KEY (show_id) REFERENCES shows(show_id) ON DELETE CASCADE,
  UNIQUE (show_id, seat_label)
);

CREATE TABLE IF NOT EXISTS bookings (
  booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_code VARCHAR(30) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  booking_status ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
  booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_booking_show FOREIGN KEY (show_id) REFERENCES shows(show_id)
);

CREATE TABLE IF NOT EXISTS booking_details (
  booking_detail_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  show_seat_id BIGINT NOT NULL,
  seat_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detail_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
  CONSTRAINT fk_detail_seat FOREIGN KEY (show_seat_id) REFERENCES show_seats(show_seat_id),
  UNIQUE (booking_id, show_seat_id)
);
