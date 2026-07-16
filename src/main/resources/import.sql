-- Sample data loaded automatically on startup (because generation=drop-and-create).
-- This is how you can demo the API immediately with real rows in MySQL.
INSERT INTO tasks (title, description, status, priority, due_date, created_at) VALUES ('Set up development environment', 'Install JDK 21, Maven and Docker Desktop', 'DONE', 'HIGH', '2026-07-18', '2026-07-15 09:00:00');
INSERT INTO tasks (title, description, status, priority, due_date, created_at) VALUES ('Learn Quarkus basics', 'Understand REST, CDI and Panache', 'IN_PROGRESS', 'HIGH', '2026-07-25', '2026-07-15 09:05:00');
INSERT INTO tasks (title, description, status, priority, due_date, created_at) VALUES ('Design MySQL schema', 'Create the tasks table with proper columns and indexes', 'IN_PROGRESS', 'MEDIUM', '2026-07-28', '2026-07-15 09:10:00');
INSERT INTO tasks (title, description, status, priority, due_date, created_at) VALUES ('Write the cold email', 'Reach out to Team Computers with the project link', 'TODO', 'MEDIUM', '2026-08-05', '2026-07-15 09:15:00');
INSERT INTO tasks (title, description, status, priority, due_date, created_at) VALUES ('Deploy to AWS', 'Push the container and run it on ECS with RDS', 'TODO', 'LOW', '2026-08-15', '2026-07-15 09:20:00');
