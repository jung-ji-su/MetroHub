package com.metrohub.api.domain.community;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CommunityMapper {
    List<CommunityPost> findPostsByLineNumber(@Param("lineNumber") String lineNumber,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);
    List<CommunityPost> findPostsByUserId(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);
    Optional<CommunityPost> findPostById(@Param("id") Long id);
    void insertPost(CommunityPost post);
    void deletePost(@Param("id") Long id, @Param("userId") Long userId);

    List<CommunityComment> findCommentsByPostId(@Param("postId") Long postId);
    Optional<CommunityComment> findCommentById(@Param("id") Long id);
    void insertComment(CommunityComment comment);
    void updateComment(@Param("id") Long id, @Param("userId") Long userId, @Param("content") String content);
    void deleteComment(@Param("id") Long id, @Param("userId") Long userId);

    // 좋아요
    void insertLike(@Param("postId") Long postId, @Param("userId") Long userId);
    void deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);
    boolean existsLike(@Param("postId") Long postId, @Param("userId") Long userId);
    int countLikes(@Param("postId") Long postId);
}
