package org.kiwi.console.generate.rest;

import org.kiwi.console.generate.GenerationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auto-test")
public class AutoTestController {

    private final GenerationService generationService;

    public AutoTestController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/next-action")
    public AutoTestAction nextAction(@RequestBody AutoTestStepRequest request) {
        return generationService.autoTestStep(request);
    }

    @PostMapping("/cancel")
    public void cancel(@RequestBody AutoTestCancelRequest request) {
        generationService.cancelAutoTest(request);
    }

}
