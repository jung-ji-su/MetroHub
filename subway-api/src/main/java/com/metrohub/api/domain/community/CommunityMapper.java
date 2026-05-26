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
    Optional<CommunityPost> findPostById(@Param("id") Long id);
    void insertPost(CommunityPost post);
    void deletePost(@Param("id") Long id, @Param("userId") Long userId);

    List<CommunityComment> findCommentsByPostId(@Param("postId") Long postId);
    void insertComment(CommunityComment comment);
    void deleteComment(@Param("id") Long id, @Param("userId") Long userId);
}
