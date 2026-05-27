package com.metrohub.api.domain.trending;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class TrendingService {

    private final AtomicReference<TrendingDto> cache = new AtomicReference<>(
            TrendingDto.builder().stations(List.of()).build()
    );

    public void update(TrendingDto dto) {
        cache.set(dto);
        log.debug("트렌딩 캐시 업데이트: {}역", dto.getStations().size());
    }

    public TrendingDto getCurrent() {
        return cache.get();
    }
}
