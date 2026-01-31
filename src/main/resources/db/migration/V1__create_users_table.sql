CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255),
                       role VARCHAR(50),
                       active BOOLEAN,
                       joined_at DATE,
                       last_login DATE,
                       avatar_url VARCHAR(255),

                       social_profile_id UUID,

                       user_type VARCHAR(31),

                       teacher_code VARCHAR(255),
                       student_count INTEGER
);

CREATE TABLE social_profiles (
                                 id UUID PRIMARY KEY,
                                 github_url VARCHAR(255),
                                 instagram_url VARCHAR(255),
                                 x_url VARCHAR(255),
                                 linkedin_url VARCHAR(255),
                                 website_url VARCHAR(255)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_social_profile
        FOREIGN KEY (social_profile_id) REFERENCES social_profiles(id);


CREATE TABLE user_academic_degrees (
                                       user_id UUID NOT NULL,
                                       degree VARCHAR(255),
                                       FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_organizations (
                                    user_id UUID NOT NULL,
                                    organization VARCHAR(255),
                                    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_stacks (
                             user_id UUID NOT NULL,
                             stack_technology VARCHAR(255),
                             FOREIGN KEY (user_id) REFERENCES users(id)
);