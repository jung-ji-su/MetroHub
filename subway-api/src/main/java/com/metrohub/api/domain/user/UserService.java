package com.metrohub.api.domain.user;

import com.metrohub.api.global.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final long REFRESH_EXPIRY_DAYS = 7;

    @Transactional
    public UserDto.Response register(UserDto.RegisterRequest request) {
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();
        userMapper.insert(user);
        return UserDto.Response.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_EXPIRY_DAYS))
                .build();
        refreshTokenMapper.insert(refreshToken);
        return UserDto.LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getEmail()))
                .refreshToken(refreshTokenValue)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public UserDto.LoginResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenMapper.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Refresh Token입니다."));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenMapper.deleteByToken(refreshTokenValue);
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }
        User user = userMapper.findById(stored.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String newRefreshToken = UUID.randomUUID().toString();
        refreshTokenMapper.deleteByToken(refreshTokenValue);
        RefreshToken next = RefreshToken.builder()
                .userId(user.getId())
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_EXPIRY_DAYS))
                .build();
        refreshTokenMapper.insert(next);
        return UserDto.LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getEmail()))
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenMapper.deleteByToken(refreshTokenValue);
    }

    @Transactional(readOnly = true)
    public UserDto.Response getProfile(String email) {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserDto.Response.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
