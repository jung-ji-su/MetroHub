package com.metrohub.api.domain.complaint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintMapper complaintMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ComplaintDto.Response createComplaint(Long userId, ComplaintDto.CreateRequest request) {
        Complaint complaint = Complaint.builder()
                .userId(userId)
                .category(request.getCategory())
                .stationName(request.getStationName())
                .content(request.getContent())
                .status("RECEIVED")
                .build();
        complaintMapper.insert(complaint);

        try {
            kafkaTemplate.send("complaint-events", objectMapper.writeValueAsString(complaint));
        } catch (Exception e) {
            log.warn("complaint-events Kafka 발행 실패 (complaintId={}): {}", complaint.getId(), e.getMessage());
        }
        return toResponse(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto.Response> getMyComplaints(Long userId) {
        return complaintMapper.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComplaint(Long userId, Long id) {
        complaintMapper.deleteByIdAndUserId(id, userId);
    }

    @Transactional(readOnly = true)
    public ComplaintDto.Response getComplaint(Long id) {
        return complaintMapper.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto.Response> getAllComplaints(String status, int page, int size) {
        return complaintMapper.findAll(status, page * size, size).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countAllComplaints(String status) {
        return complaintMapper.countAll(status);
    }

    @Transactional
    public ComplaintDto.Response updateStatus(Long id, String status) {
        complaintMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));
        complaintMapper.updateStatus(id, status);
        return complaintMapper.findById(id).map(this::toResponse).orElseThrow();
    }

    private ComplaintDto.Response toResponse(Complaint c) {
        return ComplaintDto.Response.builder()
                .id(c.getId())
                .category(c.getCategory())
                .stationName(c.getStationName())
                .content(c.getContent())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
