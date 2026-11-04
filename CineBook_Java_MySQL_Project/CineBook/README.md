# CineBook - Java + MySQL Console Movie Ticket Booking System

A beginner-friendly college project built with Java 17, Maven, JDBC and MySQL 8.

## Features in this starter version
- User registration and login (passwords hashed with PBKDF2)
- List and search upcoming movie shows
- View show seat availability and book multiple seats
- Database transaction for booking; selected seats cannot be double-booked
- Booking history and cancellation
- Admin login, movie listing/addition and booking report
- Payment is simulated; no real payment gateway is connected

## Requirements
- JDK 17+
- MySQL Server 8+
- Maven 3.8+ (or use Maven support in your IDE)

## 1. Create the database
Open MySQL Workbench, connect to your local MySQL server, then run these files in order:
1. `database/schema.sql`
2. `database/sample_data.sql`

The sample data script adds five movies, two theatres, three screens, five upcoming shows and seats for each show.

## 2. Configure database connection
The app reads these environment variables:
- `DB_URL` (default: `jdbc:mysql://localhost:3306/cinebook_db?useSSL=false&serverTimezone=UTC`)
- `DB_USER` (default: `root`)
- `DB_PASSWORD` (default: empty)

Set `DB_PASSWORD` to the password you created for your MySQL account. Do not commit credentials to GitHub.

## 3. Run
From the project root:

```bash
mvn clean compile
mvn exec:java
```

Or open the folder in VS Code / IntelliJ and run `com.cinebook.Main`.

## Default admin
On first successful application start, the app creates:
- Email: `admin@cinebook.local`
- Password: `Admin@123`

Change this before any real deployment. This is a classroom demonstration project, not production ticketing software.

## Suggested demo
1. Register a user.
2. Log in.
3. View shows.
4. Book seats such as A1,A2.
5. View booking history.
6. Cancel the booking and show that seats become available again.
7. Log in as admin and add a movie.

## Project structure
```text
CineBook/
├── database/
│   ├── schema.sql
│   └── sample_data.sql
├── src/main/java/com/cinebook/
│   ├── Main.java
│   ├── config/DBConnection.java
│   └── util/PasswordUtil.java
├── pom.xml
└── README.md
```

## Limitations / next improvements
- Payment is simulated only.
- Admin show/theatre management is currently seeded through SQL; add full CRUD in the next iteration.
- Add automated tests, seat-map formatting, booking cancellation policy, and admin password-change flow.
- Grok AI is intentionally not integrated in Version 1. Add it later behind a separate recommendation service; never hardcode the API key.
