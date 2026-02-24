package com.pos.system.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ReportingServiceTest {

    @Test
    public void testGenerateDailyZReport() {
        ReportingService service = new ReportingService();

        // Asserting that both CSV and Excel generation execute without throwing
        // exceptions.
        assertDoesNotThrow(() -> service.generateDailyZReportCSV());
        assertDoesNotThrow(() -> service.generateDailyZReportExcel());
    }
}
