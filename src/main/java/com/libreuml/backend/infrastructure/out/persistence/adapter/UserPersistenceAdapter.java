package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.infrastructure.out.persistence.entity.UserEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.UserPersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    @Override
    public User save(User user) {
        UserEntity entity = userPersistenceMapper.toEntity(user);
        UserEntity savedEntity = springDataUserRepository.save(entity);
        return userPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> getUserById(UUID id) {
        return springDataUserRepository.findById(id)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public PagedResult<User> getUserByFullName(String fullName, PaginationCommand command) {
        Pageable pageable = toPageable(command);
        Page<UserEntity> page = springDataUserRepository.findByFullNameContainingIgnoreCase(fullName, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<User> getAllUsers(PaginationCommand command) {
        Pageable pageable = toPageable(command);
        Page<UserEntity> page = springDataUserRepository.findAll(pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<User> getActiveUsers(PaginationCommand command) {
        Pageable pageable = toPageable(command);
        Page<UserEntity> page = springDataUserRepository.findAllActive(pageable);
        return toPagedResult(page);
    }

    @Override
    public int getActiveUsersCount() {
        return (int) springDataUserRepository.countByActiveTrue();
    }

    @Override
    public int getTotalUsersCount() {
        return (int) springDataUserRepository.count();
    }


    private Pageable toPageable(PaginationCommand command) {
        return PageRequest.of(command.page(), command.size());
    }

    private PagedResult<User> toPagedResult(Page<UserEntity> page) {
        return new PagedResult<>(
                page.getContent().stream().map(userPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}