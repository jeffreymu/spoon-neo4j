-- Test SQL Schema for Oracle
-- This file contains sample DDL statements for Oracle

-- Create sequence for users
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;

-- Create users table
CREATE TABLE users (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(50) NOT NULL,
    email VARCHAR2(100) NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP,
    status VARCHAR2(20) DEFAULT 'active',
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- Create trigger for auto-increment
CREATE OR REPLACE TRIGGER users_trg
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := users_seq.NEXTVAL;
    END IF;
END;
/

-- Create roles table
CREATE TABLE roles (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(50) NOT NULL,
    description VARCHAR2(255)
);

-- Create user_roles junction table
CREATE TABLE user_roles (
    user_id NUMBER NOT NULL,
    role_id NUMBER NOT NULL,
    assigned_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create departments table
CREATE TABLE departments (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    location VARCHAR2(200),
    manager_id NUMBER,
    CONSTRAINT fk_dept_manager FOREIGN KEY (manager_id) REFERENCES employees(id)
);

-- Create employees table
CREATE TABLE employees (
    id NUMBER PRIMARY KEY,
    first_name VARCHAR2(50) NOT NULL,
    last_name VARCHAR2(50) NOT NULL,
    email VARCHAR2(100) NOT NULL,
    phone VARCHAR2(20),
    hire_date DATE DEFAULT SYSDATE,
    salary NUMBER(10, 2),
    department_id NUMBER,
    manager_id NUMBER,
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_emp_manager FOREIGN KEY (manager_id) REFERENCES employees(id),
    CONSTRAINT uk_emp_email UNIQUE (email)
);

-- Create index on employees
CREATE INDEX idx_emp_dept ON employees(department_id);
CREATE INDEX idx_emp_manager ON employees(manager_id);
CREATE INDEX idx_emp_name ON employees(last_name, first_name);

-- Create view for employee details
CREATE OR REPLACE VIEW employee_details AS
SELECT 
    e.id,
    e.first_name || ' ' || e.last_name AS full_name,
    e.email,
    e.salary,
    d.name AS department_name,
    m.first_name || ' ' || m.last_name AS manager_name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
LEFT JOIN employees m ON e.manager_id = m.id;

-- Create stored procedure
CREATE OR REPLACE PROCEDURE get_employee_by_dept(
    p_dept_id IN NUMBER,
    p_cursor OUT SYS_REFCURSOR
)
AS
BEGIN
    OPEN p_cursor FOR
    SELECT * FROM employees WHERE department_id = p_dept_id;
END;
/
