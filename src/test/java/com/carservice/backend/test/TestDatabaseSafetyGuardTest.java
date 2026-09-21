package com.carservice.backend.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TestDatabaseSafetyGuardTest {

    @Test
    @DisplayName("Safety guard passes when datasource points to car_service_test")
    void testSafetyGuardPassesForTestDatabase() throws Exception {
        DataSource mockDs = Mockito.mock(DataSource.class);
        Connection mockConn = Mockito.mock(Connection.class);
        DatabaseMetaData mockMetaData = Mockito.mock(DatabaseMetaData.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getURL()).thenReturn("jdbc:mysql://localhost:3306/car_service_test");
        when(mockConn.getCatalog()).thenReturn("car_service_test");

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard(mockDs);

        assertDoesNotThrow(() -> guard.postProcessAfterInitialization(mockDs, "dataSource"));
    }

    @Test
    @DisplayName("Safety guard throws IllegalStateException when datasource points to car_service_dev")
    void testSafetyGuardFailsForDevDatabase() throws Exception {
        DataSource mockDs = Mockito.mock(DataSource.class);
        Connection mockConn = Mockito.mock(Connection.class);
        DatabaseMetaData mockMetaData = Mockito.mock(DatabaseMetaData.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getURL()).thenReturn("jdbc:mysql://localhost:3306/car_service_dev");
        when(mockConn.getCatalog()).thenReturn("car_service_dev");

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard(mockDs);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                guard.postProcessAfterInitialization(mockDs, "dataSource")
        );

        assertTrue(ex.getMessage().contains("FATAL TEST SAFETY VIOLATION"));
        assertTrue(ex.getMessage().contains("car_service_dev"));
    }
}
