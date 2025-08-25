package com.cMall.feedShop.feed.application.service;

import com.cMall.feedShop.common.dto.PaginatedResponse;
import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.feed.application.dto.request.CommentCreateRequestDto;
import com.cMall.feedShop.feed.application.dto.response.CommentListResponseDto;
import com.cMall.feedShop.feed.application.dto.response.CommentResponseDto;
import com.cMall.feedShop.feed.domain.entity.Comment;
import com.cMall.feedShop.feed.domain.entity.Feed;
import com.cMall.feedShop.feed.domain.repository.CommentRepository;
import com.cMall.feedShop.feed.domain.repository.FeedRepository;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.model.UserProfile;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @Mock
    private UserProfile userProfile;

    @Mock
    private Feed feed;

    @Mock
    private Comment comment;

    @InjectMocks
    private CommentService commentService;

    private CommentCreateRequestDto createRequestDto;

    @BeforeEach
    void setUp() {
        // CommentCreateRequestDto 설정
        createRequestDto = CommentCreateRequestDto.builder()
                .content("테스트 댓글")
                .build();
    }

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_success() {
        // given
        Long feedId = 1L;
        Long userId = 1L;

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
        // User Mock 설정
        when(user.getId()).thenReturn(userId);
        when(user.getUserProfile()).thenReturn(userProfile);
        when(userProfile.getNickname()).thenReturn("테스트유저");
        when(userProfile.getProfileImageUrl()).thenReturn("test-profile.jpg");
        
        // Comment Mock 설정
        when(comment.getId()).thenReturn(1L);
        when(comment.getContent()).thenReturn("테스트 댓글");
        when(comment.getUser()).thenReturn(user);
        when(comment.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(comment.getUpdatedAt()).thenReturn(LocalDateTime.now());

        // when
        CommentResponseDto result = commentService.createComment(feedId, userId, createRequestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 댓글");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserNickname()).isEqualTo("테스트유저");

        verify(feedRepository).findById(feedId);
        verify(userRepository).findById(userId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 피드가 존재하지 않음")
    void createComment_feedNotFound() {
        // given
        Long feedId = 999L;
        Long userId = 1L;

        when(feedRepository.findById(feedId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(feedId, userId, createRequestDto))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);

        verify(feedRepository).findById(feedId);
        verify(userRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 사용자가 존재하지 않음")
    void createComment_userNotFound() {
        // given
        Long feedId = 1L;
        Long userId = 999L;

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(feedId, userId, createRequestDto))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(feedRepository).findById(feedId);
        verify(userRepository).findById(userId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getComments_success() {
        // given
        Long feedId = 1L;
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        List<Comment> comments = List.of(comment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        
        // 페이징된 댓글 ID 목록 조회
        Page<Long> commentIdsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(commentRepository.findCommentIdsByFeedId(feedId, pageable)).thenReturn(commentIdsPage);
        
        // 댓글 ID 목록으로 fetch join하여 댓글 상세 정보 조회
        when(commentRepository.findByIdsWithUser(List.of(1L))).thenReturn(comments);
        
        // User Mock 설정
        when(user.getId()).thenReturn(1L);
        when(user.getUserProfile()).thenReturn(userProfile);
        when(userProfile.getNickname()).thenReturn("테스트유저");
        when(userProfile.getProfileImageUrl()).thenReturn("test-profile.jpg");
        
        // Comment Mock 설정
        when(comment.getId()).thenReturn(1L);
        when(comment.getContent()).thenReturn("테스트 댓글");
        when(comment.getUser()).thenReturn(user);
        when(comment.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(comment.getUpdatedAt()).thenReturn(LocalDateTime.now());

        // when
        CommentListResponseDto result = commentService.getComments(feedId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalComments()).isEqualTo(1L);
        assertThat(result.getPagination()).isNotNull();
        assertThat(result.getPagination().getContent()).hasSize(1);
        assertThat(result.getPagination().getContent().get(0).getContent()).isEqualTo("테스트 댓글");

        verify(feedRepository).findById(feedId);
        verify(commentRepository).findCommentIdsByFeedId(feedId, pageable);
        verify(commentRepository).findByIdsWithUser(List.of(1L));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 피드가 존재하지 않음")
    void getComments_feedNotFound() {
        // given
        Long feedId = 999L;
        int page = 0;
        int size = 20;

        when(feedRepository.findById(feedId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getComments(feedId, page, size))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);

        verify(feedRepository).findById(feedId);
        verify(commentRepository, never()).findCommentIdsByFeedId(anyLong(), any(Pageable.class));
        verify(commentRepository, never()).findByIdsWithUser(anyList());
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        // given
        Long feedId = 1L;
        Long commentId = 1L;
        Long userId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(comment);
        
        // Feed Mock 설정
        when(feed.getId()).thenReturn(feedId);
        
        // Comment Mock 설정
        when(comment.getFeed()).thenReturn(feed);
        when(comment.isWrittenBy(userId)).thenReturn(true);

        // when
        commentService.deleteComment(feedId, commentId, userId);

        // then
        verify(commentRepository).findById(commentId);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글이 존재하지 않음")
    void deleteComment_commentNotFound() {
        // given
        Long feedId = 1L;
        Long commentId = 999L;
        Long userId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(feedId, commentId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);

        verify(commentRepository).findById(commentId);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 피드 ID가 일치하지 않음")
    void deleteComment_feedIdMismatch() {
        // given
        Long feedId = 999L; // 다른 피드 ID
        Long commentId = 1L;
        Long userId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        
        // Feed Mock 설정
        when(feed.getId()).thenReturn(1L); // 댓글의 피드 ID는 1L
        
        // Comment Mock 설정
        when(comment.getFeed()).thenReturn(feed);

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(feedId, commentId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);

        verify(commentRepository).findById(commentId);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void deleteComment_accessDenied() {
        // given
        Long feedId = 1L;
        Long commentId = 1L;
        Long userId = 999L; // 다른 사용자 ID

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        
        // Feed Mock 설정
        when(feed.getId()).thenReturn(feedId);
        
        // Comment Mock 설정
        when(comment.getFeed()).thenReturn(feed);
        when(comment.isWrittenBy(userId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(feedId, commentId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_ACCESS_DENIED);

        verify(commentRepository).findById(commentId);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 목록 조회 - 빈 페이지")
    void getComments_emptyPage() {
        // given
        Long feedId = 1L;
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Page<Comment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        
        // 페이징된 댓글 ID 목록 조회 (빈 페이지)
        Page<Long> emptyCommentIdsPage = new PageImpl<>(List.of(), pageable, 0);
        when(commentRepository.findCommentIdsByFeedId(feedId, pageable)).thenReturn(emptyCommentIdsPage);
        
        // 댓글 ID 목록으로 fetch join하여 댓글 상세 정보 조회 (빈 리스트)
        when(commentRepository.findByIdsWithUser(List.of())).thenReturn(List.of());

        // when
        CommentListResponseDto result = commentService.getComments(feedId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalComments()).isEqualTo(0L);
        assertThat(result.getPagination().getContent()).isEmpty();

        verify(feedRepository).findById(feedId);
        verify(commentRepository).findCommentIdsByFeedId(feedId, pageable);
        verify(commentRepository).findByIdsWithUser(List.of());
    }
}
