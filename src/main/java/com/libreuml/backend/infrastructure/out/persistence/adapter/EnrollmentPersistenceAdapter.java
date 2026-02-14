package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.enrollment.port.out.EnrollmentRepository;
import com.libreuml.backend.domain.model.Enrollment;
import com.libreuml.backend.infrastructure.out.persistence.entity.EnrollmentEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.EnrollmentPersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataCourseRepository;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataEnrollmentRepository;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EnrollmentPersistenceAdapter implements EnrollmentRepository {

    private final SpringDataEnrollmentRepository enrollmentRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataCourseRepository courseRepository;
    private final EnrollmentPersistenceMapper enrollmentMapper;

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentEntity entity = enrollmentMapper.toEntity(enrollment);
        if (enrollment.getStudentId() != null) {
            entity.setStudent(userRepository.getReferenceById(enrollment.getStudentId()));
        }
        if (enrollment.getCourseId() != null) {
            entity.setCourse(courseRepository.getReferenceById(enrollment.getCourseId()));
        }

        EnrollmentEntity savedEntity = enrollmentRepository.save(entity);
        return enrollmentMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    public Optional<Enrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(enrollmentMapper::toDomain);
    }

    @Override
    public PagedResult<Enrollment> findAllByCourseId(UUID courseId, PaginationCommand pagination) {
        Pageable pageable = PageRequest.of(pagination.page(), pagination.size());
        Page<EnrollmentEntity> page = enrollmentRepository.findAllByCourseId(courseId, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Enrollment> findAllByStudentId(UUID studentId, PaginationCommand pagination) {
        Pageable pageable = PageRequest.of(pagination.page(), pagination.size());
        Page<EnrollmentEntity> page = enrollmentRepository.findAllByStudentId(studentId, pageable);
        return toPagedResult(page);
    }

    private PagedResult<Enrollment> toPagedResult(Page<EnrollmentEntity> page) {
        List<Enrollment> content = page.getContent().stream()
                .map(enrollmentMapper::toDomain)
                .toList();

        return new PagedResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}