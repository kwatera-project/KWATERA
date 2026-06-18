package io.github.kwatera_project.kwatera.reservation_service.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

// Runs Cucumber BDD tests for the reservation-service
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME,
    value = "io.github.kwatera_project.kwatera.reservation_service.bdd")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
class CucumberRunnerTest {}
