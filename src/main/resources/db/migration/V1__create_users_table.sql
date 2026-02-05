-- V1__create_users_table.sql

CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       user_type VARCHAR(31) NOT NULL,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255),
                       role VARCHAR(50),
                       active BOOLEAN DEFAULT TRUE,

                       joined_at DATE,
                       last_login DATE,
                       avatar_url VARCHAR(255),

                       academic_degrees JSONB,
                       organization JSONB,
                       stacks JSONB,

                       github_url VARCHAR(255),
                       instagram_url VARCHAR(255),
                       x_url VARCHAR(255),
                       linkedin_url VARCHAR(255),
                       website_url VARCHAR(255),

                       teacher_code VARCHAR(255),
                       student_count INTEGER
);

