package it.luncent.cloud_storage.resource.model.request;

import jakarta.validation.constraints.NotBlank;

public record MoveRequest(@NotBlank String from,
                          @NotBlank String to) {
}
