package it.luncent.cloud_storage.common.constants;

import lombok.Builder;

@Builder
public record PopulationFilter(boolean includeDirectories,
                               boolean includeMarkers,
                               boolean includeFiles) {
}
