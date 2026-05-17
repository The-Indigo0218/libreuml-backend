ALTER TABLE diagrams DROP CONSTRAINT IF EXISTS diagrams_diagram_type_check;

ALTER TABLE diagrams
    ADD CONSTRAINT diagrams_diagram_type_check
        CHECK (diagram_type IN (
            'CLASS', 'USE_CASE', 'SEQUENCE', 'ACTIVITY',
            'STATE', 'COMPONENT', 'DEPLOYMENT', 'PACKAGE',
            'OBJECT', 'DOMAIN', 'UNSPECIFIED'
        ));
