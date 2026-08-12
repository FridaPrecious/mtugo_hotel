package com.mtugo.mtugo_hotel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(2));
            System.out.println("Connection test passed: " + conn.getMetaData().getURL());
        }
    }
}