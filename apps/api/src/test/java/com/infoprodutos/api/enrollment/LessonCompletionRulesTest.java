package com.infoprodutos.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LessonCompletionRulesTest {

    @Test
    void reachesWatchThreshold_atExactly90Percent() {
        // 90% de 100 = 90
        assertThat(LessonCompletionRules.reachesWatchThreshold(90, 100)).isTrue();
        assertThat(LessonCompletionRules.reachesWatchThreshold(89, 100)).isFalse();
    }

    @Test
    void reachesWatchThreshold_ceilForOddDurations() {
        // 90% de 10 = 9.0 -> ceil 9
        assertThat(LessonCompletionRules.reachesWatchThreshold(9, 10)).isTrue();
        assertThat(LessonCompletionRules.reachesWatchThreshold(8, 10)).isFalse();
    }

    @Test
    void reachesWatchThreshold_falseWhenNoDuration() {
        assertThat(LessonCompletionRules.reachesWatchThreshold(999, null)).isFalse();
        assertThat(LessonCompletionRules.reachesWatchThreshold(999, 0)).isFalse();
        assertThat(LessonCompletionRules.reachesWatchThreshold(999, -1)).isFalse();
    }
}
