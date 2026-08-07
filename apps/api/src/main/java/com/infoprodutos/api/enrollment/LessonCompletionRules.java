package com.infoprodutos.api.enrollment;

/**
 * Regra de conclusão de aula (PRD §7 / DECISIONS #6):
 * COMPLETED quando last_position_seconds >= 90% da duração do vídeo,
 * ou por marcação manual. Status monotônico.
 */
public final class LessonCompletionRules {

    public static final double COMPLETION_THRESHOLD = 0.90;

    private LessonCompletionRules() {}

    /** True se a posição reportada alcança o limiar de 90% da duração. */
    public static boolean reachesWatchThreshold(int positionSeconds, Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return false;
        }
        return positionSeconds >= Math.ceil(durationSeconds * COMPLETION_THRESHOLD);
    }
}
