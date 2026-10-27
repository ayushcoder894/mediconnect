package com.mediconnect.risk;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk/diabetes")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/prefill")
    public RiskService.PrefillDto prefill() {
        return riskService.prefill();
    }

    @PostMapping
    public RiskService.RiskResultDto assess(@Valid @RequestBody RiskService.AssessRequest req) {
        return riskService.assess(req);
    }

    @GetMapping("/history")
    public List<RiskService.RiskResultDto> history() {
        return riskService.history();
    }
}
