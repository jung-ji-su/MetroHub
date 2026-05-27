package com.metrohub.api.domain.trending;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping
    public ResponseEntity<TrendingDto> getTrending() {
        return ResponseEntity.ok(trendingService.getCurrent());
    }
}
