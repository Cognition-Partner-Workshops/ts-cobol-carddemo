-- Default users for CardDemo application
-- Password is 'password' encoded with BCrypt
INSERT INTO users (user_id, first_name, last_name, password, user_type, active, created_at, updated_at)
VALUES ('ADMIN001', 'System', 'Administrator', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gPvjWWn.5NqLDu', 'A', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (user_id, first_name, last_name, password, user_type, active, created_at, updated_at)
VALUES ('USER0001', 'John', 'Doe', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gPvjWWn.5NqLDu', 'U', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (user_id, first_name, last_name, password, user_type, active, created_at, updated_at)
VALUES ('USER0002', 'Jane', 'Smith', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gPvjWWn.5NqLDu', 'U', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
