CREATE TABLE enrollments (
                             id UUID PRIMARY KEY,

                             student_id UUID NOT NULL,
                             course_id UUID NOT NULL,

                             progress INTEGER DEFAULT 0,
                             active BOOLEAN DEFAULT TRUE,
                             enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,

                             CONSTRAINT uk_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);