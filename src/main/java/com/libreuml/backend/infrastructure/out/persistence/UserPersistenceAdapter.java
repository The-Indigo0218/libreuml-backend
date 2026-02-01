package com.libreuml.backend.infrastructure.out.persistence;

import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.infrastructure.out.persistence.entity.UserEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.UserMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {


    private final SpringDataUserRepository springRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return springRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = springRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> getUserById(UUID id) {
        return springRepository.findById(id)
                .map(userMapper::toDomain);
    }
}