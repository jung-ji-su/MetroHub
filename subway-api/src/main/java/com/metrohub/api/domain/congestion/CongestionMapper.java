package com.metrohub.api.domain.congestion;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CongestionMapper {
    List<Congestion> findByStationName(@Param("stationName") String stationName);
    List<Congestion> findByLineNumber(@Param("lineNumber") String lineNumber);
    void upsert(Congestion congestion);
}
