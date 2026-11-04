USE cinebook_db;

INSERT INTO movies (title,genre,language,duration_minutes,rating,description) VALUES
('Interstellar','Sci-Fi','English',169,8.7,'A team travels beyond this galaxy to save humanity.'),
('Vikram','Action','Tamil',175,8.3,'A special agent investigates a series of crimes.'),
('The Conjuring','Horror','English',112,7.5,'Paranormal investigators help a family.'),
('Jailer','Action','Tamil',168,7.8,'A retired jailer takes on a dangerous gang.'),
('Inside Out 2','Animation','English',96,7.7,'A new set of emotions arrives during teenage years.')
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO theatres (theatre_name,city,address) VALUES
('CineBook Central','Chennai','Anna Salai, Chennai'),
('CineBook Marina','Chennai','Triplicane, Chennai');

INSERT INTO screens (theatre_id,screen_name,total_seats) VALUES
(1,'Screen 1',30),(1,'Screen 2',30),(2,'Screen 1',30);

INSERT INTO shows (movie_id,screen_id,show_time,ticket_price) VALUES
(1,1,DATE_ADD(NOW(), INTERVAL 1 DAY),180.00),
(2,2,DATE_ADD(NOW(), INTERVAL 1 DAY),160.00),
(3,3,DATE_ADD(NOW(), INTERVAL 2 DAY),150.00),
(4,1,DATE_ADD(NOW(), INTERVAL 2 DAY),170.00),
(5,2,DATE_ADD(NOW(), INTERVAL 3 DAY),140.00);

-- Create 30 seats (A1-A10, B1-B10, C1-C10) for each sample show.
INSERT IGNORE INTO show_seats (show_id,seat_label)
SELECT s.show_id, CONCAT(r.row_letter,n.seat_no)
FROM shows s
CROSS JOIN (SELECT 'A' AS row_letter UNION ALL SELECT 'B' UNION ALL SELECT 'C') r
CROSS JOIN (SELECT 1 seat_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) n;
