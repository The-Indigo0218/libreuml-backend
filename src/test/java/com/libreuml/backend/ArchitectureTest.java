package com.libreuml.backend;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.libreuml.backend")
public class ArchitectureTest {

    // RULE 1: Domain isolation
    @ArchTest
    static final ArchRule domain_should_be_isolated_from_outer_layers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure..",
                            "..application.."
                    )
                    .because("The Domain layer must remain pure and oblivious to infrastructure or application logic.");


    // RULE 2: Web layer should not access persistence layer directly
    @ArchTest
    static final ArchRule web_layer_should_not_access_persistence_layer_directly =
            noClasses()
                    .that().resideInAPackage("..infrastructure.in.web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.out.persistence..")
                    .because("Web controllers must delegate to application use cases (ports) instead of calling repositories directly.");

    // RULE 3: Application layer isolation
    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("The Application layer (Use Cases) must be agnostic of web controllers, databases, or specific security implementations.");

    // RULE 4: Adapters isolation
    @ArchTest
    static final ArchRule persistence_should_not_depend_on_web =
            noClasses()
                    .that().resideInAPackage("..infrastructure.out.persistence..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.in.web..")
                    .because("Persistence adapters should only implement output ports, without any knowledge of the HTTP/Web delivery mechanism.");

}
