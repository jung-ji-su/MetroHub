package com.metrohub.api.domain.complaint;

import com.metrohub.api.domain.user.User;
import com.metrohub.api.domain.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllComplaints(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(email);
        List<ComplaintDto.Response> items = complaintService.getAllComplaints(status, page, size);
        long total = complaintService.countAllComplaints(status);
        return ResponseEntity.ok(Map.of("items", items, "total", total, "page", page, "size", size));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintDto.Response> updateStatus(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(email);
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 값이 필요합니다.");
        }
        return ResponseEntity.ok(complaintService.updateStatus(id, newStatus));
    }

    private void requireAdmin(String email) {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }
}
