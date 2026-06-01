package com.metrohub.api.domain.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock CommunityMapper communityMapper;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ObjectMapper objectMapper;

    @InjectMocks CommunityService communityService;

    private CommunityPost post;
    private CommunityComment comment;

    @BeforeEach
    void setUp() {
        post = CommunityPost.builder()
                .id(1L)
                .lineNumber("2")
                .userId(10L)
                .title("테스트 제목")
                .content("테스트 내용")
                .alert(false)
                .authorNickname("작성자")
                .createdAt(LocalDateTime.now())
                .build();

        comment = CommunityComment.builder()
                .id(1L)
                .postId(1L)
                .userId(10L)
                .content("댓글 내용")
                .authorNickname("댓글작성자")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("호선별 게시글 목록 조회")
    void getPostsByLine_success() {
        given(communityMapper.findPostsByLineNumber("2", 0, 10)).willReturn(List.of(post));

        List<CommunityDto.PostResponse> result = communityService.getPostsByLine("2", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목");
        assertThat(result.get(0).getLineNumber()).isEqualTo("2");
    }

    @Test
    @DisplayName("게시글 상세 조회")
    void getPost_success() {
        given(communityMapper.findPostById(1L)).willReturn(Optional.of(post));

        CommunityDto.PostResponse result = communityService.getPost(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("테스트 내용");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 실패")
    void getPost_notFound_throws() {
        given(communityMapper.findPostById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getPost(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("게시글 작성 (alert=false, Kafka 미발행)")
    void createPost_noAlert_success() {
        CommunityDto.PostCreateRequest req = new CommunityDto.PostCreateRequest("2", "제목", "내용", false);
        willDoNothing().given(communityMapper).insertPost(any());

        CommunityDto.PostResponse result = communityService.createPost(10L, req);

        assertThat(result.getTitle()).isEqualTo("제목");
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 작성 (alert=true, Kafka 발행)")
    void createPost_withAlert_sendsKafka() throws Exception {
        CommunityDto.PostCreateRequest req = new CommunityDto.PostCreateRequest("2", "제목", "내용", true);
        willDoNothing().given(communityMapper).insertPost(any());
        given(objectMapper.writeValueAsString(any())).willReturn("{\"id\":null}");

        communityService.createPost(10L, req);

        then(kafkaTemplate).should().send(eq("community-events"), anyString());
    }

    @Test
    @DisplayName("댓글 목록 조회")
    void getComments_success() {
        given(communityMapper.findCommentsByPostId(1L)).willReturn(List.of(comment));

        List<CommunityDto.CommentResponse> result = communityService.getComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void createComment_success() {
        given(communityMapper.findPostById(1L)).willReturn(Optional.of(post));
        willDoNothing().given(communityMapper).insertComment(any());

        CommunityDto.CommentCreateRequest req = new CommunityDto.CommentCreateRequest("댓글 내용");
        CommunityDto.CommentResponse result = communityService.createComment(1L, 10L, req);

        assertThat(result.getContent()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 실패")
    void createComment_postNotFound_throws() {
        given(communityMapper.findPostById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                communityService.createComment(999L, 10L, new CommunityDto.CommentCreateRequest("내용")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("처음 좋아요 → insertLike 호출, liked=true")
    void toggleLike_firstLike() {
        given(communityMapper.findPostById(1L)).willReturn(Optional.of(post));
        given(communityMapper.existsLike(1L, 10L)).willReturn(false);
        willDoNothing().given(communityMapper).insertLike(1L, 10L);
        given(communityMapper.countLikes(1L)).willReturn(1);

        CommunityDto.LikeResponse result = communityService.toggleLike(1L, 10L);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(1);
        then(communityMapper).should().insertLike(1L, 10L);
        then(communityMapper).should(never()).deleteLike(anyLong(), anyLong());
    }

    @Test
    @DisplayName("이미 좋아요 → deleteLike 호출, liked=false")
    void toggleLike_unlike() {
        given(communityMapper.findPostById(1L)).willReturn(Optional.of(post));
        given(communityMapper.existsLike(1L, 10L)).willReturn(true);
        willDoNothing().given(communityMapper).deleteLike(1L, 10L);
        given(communityMapper.countLikes(1L)).willReturn(0);

        CommunityDto.LikeResponse result = communityService.toggleLike(1L, 10L);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.getLikeCount()).isEqualTo(0);
        then(communityMapper).should().deleteLike(1L, 10L);
        then(communityMapper).should(never()).insertLike(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내 게시글 목록 조회")
    void getMyPosts_success() {
        given(communityMapper.findPostsByUserId(10L, 0, 5)).willReturn(List.of(post));

        List<CommunityDto.PostResponse> result = communityService.getMyPosts(10L, 0, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        willDoNothing().given(communityMapper).deleteComment(1L, 10L);

        assertThatCode(() -> communityService.deleteComment(1L, 10L))
                .doesNotThrowAnyException();

        then(communityMapper).should().deleteComment(1L, 10L);
    }
}
