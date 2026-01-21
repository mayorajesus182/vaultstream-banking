package com.vaultstream.customer.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.vaultstream.customer", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // Domain Layer Rules
    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..");

    // Application Layer Rules
    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure = noClasses().that()
            .resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    // Infrastructure Layer Rules
    // Infrastructure can depend on anything (it implements interfaces)

    // Naming Conventions
    @ArchTest
    static final ArchRule repositories_should_be_interfaces = classes().that().resideInAPackage("..domain.repository..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule use_cases_should_be_named_use_case = classes().that()
            .resideInAPackage("..application.usecase..")
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule controllers_should_be_in_infrastructure = classes().that()
            .haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..infrastructure.rest..");
}
