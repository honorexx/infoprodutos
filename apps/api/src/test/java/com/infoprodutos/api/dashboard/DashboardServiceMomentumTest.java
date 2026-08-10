package com.infoprodutos.api.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse.ActivityPoint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceMomentumTest {

    @Test
    void momentumRisesWithActivityAndFallsWithInactivity() {
        DashboardService service = new DashboardService(null, null, null, null);
        int year = 2026;
        // Study every day in first week of July
        List<Instant> events = LocalDate.of(year, 7, 1)
                .datesUntil(LocalDate.of(year, 7, 8))
                .map(d -> d.atStartOfDay(ZoneOffset.UTC).toInstant())
                .toList();

        List<ActivityPoint> series = service.buildMomentumSeries(events, year);

        assertThat(series).hasSize(12);
        ActivityPoint june = series.get(5);
        ActivityPoint july = series.get(6);
        assertThat(june.value()).isEqualTo(0);
        assertThat(july.value()).isGreaterThan(0);
    }
}
