package com.metrohub.api.domain.community;

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
public class CommunityService {

    private final CommunityMapper communityMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CommunityDto.PostResponse> getPostsByLine(String lineNumber, int page, int size) {
        return communityMapper.findPostsByLineNumber(lineNumber, page * size, size).stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommunityDto.PostResponse getPost(Long id) {
        return communityMapper.findPostById(id)
                .map(this::toPostResponse)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    @Transactional
    public CommunityDto.PostResponse createPost(Long userId, CommunityDto.PostCreateRequest request) {
        CommunityPost post = CommunityPost.builder()
                .lineNumber(request.getLineNumber())
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .alert(request.isAlert())
                .build();
        communityMapper.insertPost(post);

        if (request.isAlert()) {
            try {
                kafkaTemplate.send("community-events", objectMapper.writeValueAsString(post));
            } catch (Exception e) {
                log.warn("community-events Kafka 발행 실패 (postId={}): {}", post.getId(), e.getMessage());
            }
        }
        return toPostResponse(post);
    }

    @Transactional(readOnly = true)
    public List<CommunityDto.CommentResponse> getComments(Long postId) {
        return communityMapper.findCommentsByPostId(postId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommunityDto.CommentResponse createComment(Long postId, Long userId,
                                                       CommunityDto.CommentCreateRequest request) {
        communityMapper.findPostById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        CommunityComment comment = CommunityComment.builder()
                .postId(postId)
                .userId(userId)
                .content(request.getContent())
                .build();
        communityMapper.insertComment(comment);
        return toCommentResponse(comment);
    }

    @Transactional
    public CommunityDto.LikeResponse toggleLike(Long postId, Long userId) {
        communityMapper.findPostById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        boolean already = communityMapper.existsLike(postId, userId);
        if (already) {
            communityMapper.deleteLike(postId, userId);
        } else {
            communityMapper.insertLike(postId, userId);
        }
        int count = communityMapper.countLikes(postId);
        return CommunityDto.LikeResponse.builder()
                .postId(postId)
                .likeCount(count)
                .liked(!already)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommunityDto.PostResponse> getMyPosts(Long userId, int page, int size) {
        return communityMapper.findPostsByUserId(userId, page * size, size).stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommunityDto.CommentResponse updateComment(Long commentId, Long userId, String content) {
        communityMapper.findCommentById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        communityMapper.updateComment(commentId, userId, content);
        return communityMapper.findCommentById(commentId)
                .map(this::toCommentResponse)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        communityMapper.findPostById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        communityMapper.deletePost(postId, userId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        communityMapper.deleteComment(commentId, userId);
    }

    @Transactional(readOnly = true)
    public CommunityDto.LikeResponse getLikeStatus(Long postId, Long userId) {
        return CommunityDto.LikeResponse.builder()
                .postId(postId)
                .likeCount(communityMapper.countLikes(postId))
                .liked(userId != null && communityMapper.existsLike(postId, userId))
                .build();
    }

    private CommunityDto.PostResponse toPostResponse(CommunityPost p) {
        return CommunityDto.PostResponse.builder()
                .id(p.getId())
                .lineNumber(p.getLineNumber())
                .title(p.getTitle())
                .content(p.getContent())
                .alert(Boolean.TRUE.equals(p.getAlert()))
                .authorNickname(p.getAuthorNickname())
                .createdAt(p.getCreatedAt())
                .likeCount(communityMapper.countLikes(p.getId()))
                .build();
    }

    private CommunityDto.CommentResponse toCommentResponse(CommunityComment c) {
        return CommunityDto.CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .authorId(c.getUserId())
                .content(c.getContent())
                .authorNickname(c.getAuthorNickname())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
