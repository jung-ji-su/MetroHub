package com.metrohub.api.domain.complaint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintDto.Response> create(
            @AuthenticationPrincipal String email,
            @RequestBody ComplaintDto.CreateRequest request) {
        // TODO: email → userId 변환 (UserMapper 조회)
        return ResponseEntity.ok(complaintService.createComplaint(1L, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDto.Response>> getMyComplaints(
            @AuthenticationPrincipal String email) {
        // TODO: email → userId 변환 (UserMapper 조회)
        return ResponseEntity.ok(complaintService.getMyComplaints(1L));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDto.Response> getComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaint(id));
    }
}
