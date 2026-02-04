package it.luncent.cloud_storage.common.constants;

import lombok.Builder;

@Builder
public record PopulationSettings(boolean includeDirectories,
                                 boolean includeMarkers,
                                 boolean includeFiles) {
}
