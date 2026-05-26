package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    @GetMapping("/station/{stationName}")
    public ResponseEntity<List<CongestionDto.Response>> getByStation(@PathVariable String stationName) {
        return ResponseEntity.ok(congestionService.getCongestionByStation(stationName));
    }

    @GetMapping("/line/{lineNumber}")
    public ResponseEntity<List<CongestionDto.Response>> getByLine(@PathVariable String lineNumber) {
        return ResponseEntity.ok(congestionService.getCongestionByLine(lineNumber));
    }
}
