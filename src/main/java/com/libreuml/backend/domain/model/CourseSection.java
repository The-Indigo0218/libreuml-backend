package com.libreuml.backend.domain.model;

import com.libreuml.backend.domain.model.Resource;

import java.util.UUID;

public class CourseSection {
    private UUID courseId;
    private Resource resource;
    private Integer order;
    private Boolean visible;
}