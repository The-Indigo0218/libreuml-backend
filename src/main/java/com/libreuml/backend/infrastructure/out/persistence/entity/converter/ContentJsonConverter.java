package com.libreuml.backend.infrastructure.out.persistence.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that bridges Jackson's {@link ObjectNode} and the
 * PostgreSQL {@code jsonb} column type.
 *
 * <h3>Why a custom converter instead of Hibernate's native JSON support?</h3>
 * <p>Hibernate 6 can map {@code @JdbcTypeCode(SqlTypes.JSON)} to arbitrary types, but it
 * relies on its own internal Jackson usage and does not give us control over serialization
 * options.  An explicit {@code AttributeConverter} makes the serialization contract
 * visible, testable in isolation, and decoupled from Hibernate internals.
 *
 * <h3>JSONB vs VARCHAR at the JDBC level</h3>
 * <p>The converter produces and consumes a {@code String}.  PostgreSQL's JDBC driver accepts
 * a plain {@code VARCHAR} parameter value for a {@code jsonb} column — the server parses and
 * validates the JSON text, then stores it in its optimised binary representation.  No special
 * JDBC type code is required on the Java side.
 */
@Converter(autoApply = false)
public class ContentJsonConverter implements AttributeConverter<ObjectNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ObjectNode attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize diagram content to JSONB", e);
        }
    }

    @Override
    public ObjectNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return (ObjectNode) MAPPER.readTree(dbData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize diagram content from JSONB", e);
        }
    }
}
