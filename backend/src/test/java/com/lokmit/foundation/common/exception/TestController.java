package com.lokmit.foundation.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal throw-away controller used by {@link GlobalExceptionHandlerTest}
 * to exercise exception-mapping. Not part of production code.
 */
@Validated
@RestController
public class TestController {

    @GetMapping("/api/v1/test/not-found")
    public void notFound() {
        throw new NotFoundException("Widget not found");
    }

    @GetMapping("/api/v1/test/bad-request")
    public void badRequest() {
        throw new BadRequestException("Bad widget input");
    }

    @GetMapping("/api/v1/test/conflict")
    public void conflict() {
        throw new ConflictException("Widget already exists");
    }

    @GetMapping("/api/v1/test/number")
    public String number(@RequestParam Integer value) {
        return String.valueOf(value);
    }

    @GetMapping("/api/v1/test/missing-param")
    public String missingParam(@RequestParam String required) {
        return required;
    }

    @PostMapping("/api/v1/test/body")
    public void body(@Valid @RequestBody TestPayload payload) {
    }

    @GetMapping("/api/v1/test/validated-param")
    public String validatedParam(@RequestParam @Min(5) int number) {
        return String.valueOf(number);
    }

    @GetMapping("/api/v1/test/generic")
    public void generic() {
        throw new IllegalStateException("boom");
    }
}
