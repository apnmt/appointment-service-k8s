package de.apnmt.appointment;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchTest {

    @Test
    void servicesAndRepositoriesShouldNotDependOnWebLayer() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("de.apnmt.appointment");

        noClasses()
            .that()
            .resideInAnyPackage("de.apnmt.appointment.service..")
            .or()
            .resideInAnyPackage("de.apnmt.appointment.repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..de.apnmt.appointment.web..")
            .because("Services and repositories should not depend on web layer")
            .check(importedClasses);
    }
}
