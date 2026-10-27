package com.mediconnect.hospital;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    public List<HospitalService.HospitalDto> all() {
        return hospitalService.all();
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return hospitalService.cities();
    }

    @GetMapping("/organ-types")
    public List<String> organTypes() {
        return hospitalService.organTypes();
    }

    @GetMapping("/blood")
    public List<HospitalService.StockDto> blood(@RequestParam(required = false) String group,
                                                @RequestParam(required = false) String city) {
        return hospitalService.blood(group, city);
    }

    @GetMapping("/organs")
    public List<HospitalService.StockDto> organs(@RequestParam(required = false) String organ,
                                                 @RequestParam(required = false) String city) {
        return hospitalService.organs(organ, city);
    }
}
