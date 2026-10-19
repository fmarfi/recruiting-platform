package com.recruiting.platform.pipeline.shared.dto;

import com.recruiting.platform.contracts.PipelineStage;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** OpenAPI StageCounts: every stage present, as a map keyed by stage name (serialises as an object). */
public final class StageCounts {

    private StageCounts() {
    }

    /**
     * Builds the counts with every stage present.
     *
     * @param rows rows of {stage, count}
     * @return stage to count
     */
    public static Map<PipelineStage, Long> of(Collection<Object[]> rows) {
        Map<PipelineStage, Long> m = new EnumMap<>(PipelineStage.class);
        for (PipelineStage s : PipelineStage.values()) {
            m.put(s, 0L);
        }
        for (Object[] r : rows) {
            m.put((PipelineStage) r[0], ((Number) r[1]).longValue());
        }
        return m;
    }
}
