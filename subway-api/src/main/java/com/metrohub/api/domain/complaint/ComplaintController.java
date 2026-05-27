package com.metrohub.api.domain.complaint;

import com.metrohub.api.domain.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<ComplaintDto.Response> create(
            @AuthenticationPrincipal String email,
            @RequestBody ComplaintDto.CreateRequest request) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(complaintService.createComplaint(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDto.Response>> getMyComplaints(
            @AuthenticationPrincipal String email) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(complaintService.getMyComplaints(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDto.Response> getComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaint(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        Long userId = resolveUserId(email);
        complaintService.deleteComplaint(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(String email) {
        return userMapper.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }
}
