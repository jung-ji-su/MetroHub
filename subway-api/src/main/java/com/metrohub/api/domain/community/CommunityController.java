package com.metrohub.api.domain.community;

import com.metrohub.api.domain.user.UserMapper;
import jakarta.validation.Valid;
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
            @Valid @RequestBody CommunityDto.PostCreateRequest request) {
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
            @Valid @RequestBody CommunityDto.CommentCreateRequest request) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(communityService.createComment(postId, userId, request));
    }

    @GetMapping("/posts/my")
    public ResponseEntity<List<CommunityDto.PostResponse>> getMyPosts(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(communityService.getMyPosts(userId, page, size));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email) {
        Long userId = resolveUserId(email);
        communityService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<CommunityDto.CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String email,
            @RequestBody java.util.Map<String, String> body) {
        Long userId = resolveUserId(email);
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(communityService.updateComment(commentId, userId, content));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String email) {
        Long userId = resolveUserId(email);
        communityService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<CommunityDto.LikeResponse> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email) {
        Long userId = resolveUserId(email);
        return ResponseEntity.ok(communityService.toggleLike(postId, userId));
    }

    @GetMapping("/posts/{postId}/like")
    public ResponseEntity<CommunityDto.LikeResponse> getLikeStatus(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email) {
        Long userId = email != null ? userMapper.findByNickname(email).map(u -> u.getId()).orElse(null) : null;
        return ResponseEntity.ok(communityService.getLikeStatus(postId, userId));
    }

    private Long resolveUserId(String email) {
        return userMapper.findByNickname(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }
}
