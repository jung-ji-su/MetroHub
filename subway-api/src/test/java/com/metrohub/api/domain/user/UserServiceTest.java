package com.metrohub.api.domain.user;

import com.metrohub.api.global.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock RefreshTokenMapper refreshTokenMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks UserService userService;

    private UserDto.RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new UserDto.RegisterRequest("테스터", "pass1234");

        user = User.builder()
                .id(1L)
                .nickname("테스터")
                .password("encoded-password")
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        given(userMapper.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
        willDoNothing().given(userMapper).insert(any(User.class));

        UserDto.Response response = userService.register(registerRequest);

        assertThat(response.getNickname()).isEqualTo("테스터");
        then(userMapper).should().insert(any(User.class));
    }

    @Test
    @DisplayName("중복 닉네임 회원가입 실패")
    void register_duplicateNickname_throws() {
        given(userMapper.existsByNickname("테스터")).willReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 닉네임");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("테스터", "pass1234");

        given(userMapper.findByNickname("테스터")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken(eq("테스터"), any())).willReturn("jwt-token");

        UserDto.LoginResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 닉네임으로 로그인 실패")
    void login_userNotFound_throws() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("없는유저", "pass1234");

        given(userMapper.findByNickname("없는유저")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디 또는 비밀번호");
    }

    @Test
    @DisplayName("비밀번호 불일치 로그인 실패")
    void login_wrongPassword_throws() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("테스터", "wrong");

        given(userMapper.findByNickname("테스터")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디 또는 비밀번호");
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void getProfile_success() {
        given(userMapper.findByNickname("테스터")).willReturn(Optional.of(user));

        UserDto.Response response = userService.getProfile("테스터");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 조회 실패")
    void getProfile_notFound_throws() {
        given(userMapper.findByNickname("없는유저")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("없는유저"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
