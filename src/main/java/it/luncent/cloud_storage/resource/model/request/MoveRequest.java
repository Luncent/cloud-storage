package it.luncent.cloud_storage.resource.model.request;

import it.luncent.cloud_storage.resource.validation.MoveRequestValidation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestParam;

@MoveRequestValidation
public record MoveRequest(@RequestParam @NotBlank String from,
                          @RequestParam @NotBlank String to){
}
