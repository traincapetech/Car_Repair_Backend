package com.carservice.backend.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Fail-fast safety guard preventing any integration test from connecting to or polluting
 * the development database ('car_service_dev').
 *
 * Automatically inspects the active DataSource on test context bootstrap.
 * If the connection URL or active catalog points to 'car_service_dev', it aborts test execution
 * with a fatal IllegalStateException before any test operations can execute.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestDatabaseSafetyGuard implements BeanPostProcessor, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseSafetyGuard.class);
    private static final String FORBIDDEN_DATABASE = "car_service_dev";

    private final DataSource dataSource;

    public TestDatabaseSafetyGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        verifySafety(dataSource);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource ds) {
            verifySafety(ds);
        }
        return bean;
    }

    private void verifySafety(DataSource ds) {
        try (Connection conn = ds.getConnection()) {
            String url = conn.getMetaData().getURL();
            String catalog = conn.getCatalog();

            log.info("TestDatabaseSafetyGuard: Inspecting active test datasource [catalog={}, url={}]", catalog, url);

            if ((url != null && url.toLowerCase().contains(FORBIDDEN_DATABASE)) ||
                (catalog != null && catalog.equalsIgnoreCase(FORBIDDEN_DATABASE))) {
                String errorMsg = String.format(
                    "FATAL TEST SAFETY VIOLATION: Integration tests are configured against the development " +
                    "database ('%s')! Active JDBC URL: %s, Catalog: %s. " +
                    "Tests MUST run against a dedicated test database (e.g., 'car_service_test'). " +
                    "Aborting test execution immediately to prevent data pollution.",
                    FORBIDDEN_DATABASE, url, catalog
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            log.info("TestDatabaseSafetyGuard: Isolation check passed. Connected to '{}'.", catalog);
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            log.warn("TestDatabaseSafetyGuard: Notice during connection inspection: {}", e.getMessage());
        }
    }
}
