package com.metrohub.api.domain.community;

import com.metrohub.api.domain.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final UserMapper userMapper;

    @GetMapping("/line/{lineNumber}/posts")
    public ResponseEntity<List<CommunityDto.PostResponse>> getPostsByLine(
            @PathVariable String lineNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(communityService.getPostsByLine(lineNumber, page, size));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<CommunityDto.PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getPost(id));
    }

    @PostMapping("/posts")
    public ResponseEntity<CommunityDto.PostResponse> createPost(
            @AuthenticationPrincipal String email,
            @RequestBody CommunityDto.PostCreateRequest request) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(communityService.createPost(userId, request));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommunityDto.CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(communityService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommunityDto.CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email,
            @RequestBody CommunityDto.CommentCreateRequest request) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(communityService.createComment(postId, userId, request));
    }

    private Long resolveUserId(String email) {
        return userMapper.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }
}
