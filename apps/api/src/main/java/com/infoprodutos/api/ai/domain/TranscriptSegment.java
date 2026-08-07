package com.infoprodutos.api.ai.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript_segment")
@Getter
@Setter
@NoArgsConstructor
public class TranscriptSegment extends BaseEntity {

    @Column(name = "transcript_id", nullable = false)
    private UUID transcriptId;

    @Column(name = "sequence_index", nullable = false)
    private int sequenceIndex;

    @Column(name = "start_time_seconds", nullable = false, precision = 10, scale = 2)
    private BigDecimal startTimeSeconds;

    @Column(name = "end_time_seconds", nullable = false, precision = 10, scale = 2)
    private BigDecimal endTimeSeconds;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "topic", length = 255)
    private String topic;
}
