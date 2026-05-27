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
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks UserService userService;

    private UserDto.RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new UserDto.RegisterRequest("test@test.com", "password123", "테스터");

        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encoded-password")
                .nickname("테스터")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        given(userMapper.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
        willDoNothing().given(userMapper).insert(any(User.class));

        UserDto.Response response = userService.register(registerRequest);

        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
        then(userMapper).should().insert(any(User.class));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패")
    void register_duplicateEmail_throws() {
        given(userMapper.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("test@test.com", "password123");

        given(userMapper.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("test@test.com")).willReturn("jwt-token");

        UserDto.LoginResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 실패")
    void login_userNotFound_throws() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("notfound@test.com", "password123");

        given(userMapper.findByEmail("notfound@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("비밀번호 불일치 로그인 실패")
    void login_wrongPassword_throws() {
        UserDto.LoginRequest req = new UserDto.LoginRequest("test@test.com", "wrong");

        given(userMapper.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void getProfile_success() {
        given(userMapper.findByEmail("test@test.com")).willReturn(Optional.of(user));

        UserDto.Response response = userService.getProfile("test@test.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 조회 실패")
    void getProfile_notFound_throws() {
        given(userMapper.findByEmail("notfound@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("notfound@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
