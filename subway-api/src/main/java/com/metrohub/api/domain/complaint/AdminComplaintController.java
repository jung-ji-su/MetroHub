package com.metrohub.api.domain.complaint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ComplaintDto.Response> items = complaintService.getAllComplaints(status, page, size);
        long total = complaintService.countAllComplaints(status);
        return ResponseEntity.ok(Map.of("items", items, "total", total, "page", page, "size", size));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintDto.Response> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 값이 필요합니다.");
        }
        return ResponseEntity.ok(complaintService.updateStatus(id, newStatus));
    }
}
