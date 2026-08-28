DROP TABLE IF EXISTS students;

CREATE TABLE students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL,
    gender VARCHAR(6) NOT NULL,
    age INT NOT NULL,
    major VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(80),
    city VARCHAR(40),
    score DECIMAL(5, 2),
    enrollment_date DATE
);
