package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/line")
@RequiredArgsConstructor
public class SubwayLineController {

    private final SubwayLineService subwayLineService;

    @GetMapping("/{lineCode}/trains")
    public ResponseEntity<List<TrainPositionDto>> getTrains(@PathVariable String lineCode) {
        return ResponseEntity.ok(subwayLineService.getTrains(lineCode));
    }
}
