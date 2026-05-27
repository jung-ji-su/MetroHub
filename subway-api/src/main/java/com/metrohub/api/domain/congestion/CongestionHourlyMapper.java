package com.metrohub.api.domain.congestion;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CongestionHourlyMapper {

    void upsert(@Param("stationName") String stationName,
                @Param("hourOfDay") int hourOfDay,
                @Param("congestionLevel") int congestionLevel);

    List<CongestionHourlyDto> findByStation(@Param("stationName") String stationName);
}
