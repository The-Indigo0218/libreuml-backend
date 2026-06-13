package com.libreuml.backend.domain.model;

/**
 * Diagram kinds supported inside a project. This is a superset of {@link DiagramType}
 * (the standalone-diagram enum): it adds PACKAGE, OBJECT, DOMAIN and UNSPECIFIED, which the
 * project-centric modeler frontend emits. Kept as a separate enum so the standalone-diagram
 * contract under {@code /api/v1/diagrams} stays unchanged.
 */
public enum ProjectDiagramType {
    CLASS, USE_CASE, DOMAIN, SEQUENCE, ACTIVITY, STATE,
    COMPONENT, DEPLOYMENT, PACKAGE, OBJECT, ER, UNSPECIFIED
}
