package io.github.kwatera_project.kwatera.ai_service.controller;

import io.github.kwatera_project.kwatera.ai_service.dto.*;
import io.github.kwatera_project.kwatera.ai_service.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ai")
public class AiController {
  private final AiService service;

  public AiController(AiService service) {
    this.service = service;
  }

  @PostMapping("/summarize")
  public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest request) {
    return service.summarize(request);
  }

  @PostMapping("/translate")
  public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
    return service.translate(request);
  }
}
