package com.metrohub.api.domain.arrival;

import com.metrohub.api.domain.congestion.SeoulSubwayApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/arrival")
@RequiredArgsConstructor
public class ArrivalController {

    private final SeoulSubwayApiClient seoulClient;

    private record Cached(long ts, List<ArrivalDto> data) {}
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();
    private static final long CACHE_MS = 10_000L;

    @GetMapping("/{stationName}")
    public ResponseEntity<List<ArrivalDto>> getArrivals(@PathVariable String stationName) {
        Cached hit = cache.get(stationName);
        if (hit != null && System.currentTimeMillis() - hit.ts() < CACHE_MS) {
            return ResponseEntity.ok(hit.data());
        }
        List<ArrivalDto> result = seoulClient.fetchArrivalDetails(stationName)
            .stream()
            .map(d -> new ArrivalDto(
                d.getLineCode(),
                d.getDirection(),
                d.getDestination(),
                d.getArrivalMessage(),
                d.getEtaSeconds(),
                d.getTrainStatus()
            ))
            .collect(Collectors.toList());
        cache.put(stationName, new Cached(System.currentTimeMillis(), result));
        return ResponseEntity.ok(result);
    }
}
