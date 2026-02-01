package com.libreuml.backend.application.question.port.out;

import com.libreuml.backend.domain.model.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Optional<Question> findById(UUID id);
    Question save(Question question);
    List<Question> findAllActiveQuestions();
    List<Question> findAllQuestionsByCreatorId(UUID creatorId);
    List<Question> findAllByTitleContainingIgnoreCase(String title);
    List<Question> findAllByTag(String tag);
}
