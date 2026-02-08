CREATE TABLE courses (
                         id UUID PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         slug VARCHAR(255) NOT NULL UNIQUE,
                         description TEXT,
                         cover_url VARCHAR(500),
                         visibility VARCHAR(50) NOT NULL,
                         active BOOLEAN DEFAULT TRUE,
                         tags JSONB,
                         code VARCHAR(50) NOT NULL UNIQUE,
                         creator_id UUID NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_courses_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_courses_slug ON courses(slug);
CREATE INDEX idx_courses_tags ON courses USING GIN (tags);

CREATE TABLE resources (
                           id UUID PRIMARY KEY,
                           title VARCHAR(255) NOT NULL,
                           type VARCHAR(50) NOT NULL,
                           content TEXT NOT NULL,
                           active BOOLEAN DEFAULT TRUE,

                           creator_id UUID NOT NULL,

                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_resources_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE course_resources (
                                  id UUID PRIMARY KEY,
                                  course_id UUID NOT NULL,
                                  resource_id UUID NOT NULL,

                                  position INTEGER NOT NULL,
                                  visible BOOLEAN DEFAULT TRUE,
                                  added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_cr_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_cr_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE RESTRICT,
                                  CONSTRAINT uk_course_resource UNIQUE (course_id, resource_id)
);