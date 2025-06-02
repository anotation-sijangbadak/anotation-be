package com.anotation.anotation_be.auth.service;

import com.anotation.anotation_be.auth.dto.request.LoginRequestDto;
import com.anotation.anotation_be.auth.dto.request.SignupReqDto;
import com.anotation.anotation_be.auth.dto.response.LoginResponseDto;
import com.anotation.anotation_be.auth.dto.response.UserIdResDto;
import com.anotation.anotation_be.auth.entity.Users;
import com.anotation.anotation_be.auth.repo.AuthRepository;
import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.enums.Genre;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.common.jwt.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailPublisherService emailPublisherService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_KEY = "RT:";

    @Transactional
    public UserIdResDto signup(SignupReqDto reqDto) throws BusinessException {
        // Email 검증과 Nickname 검증은 끝났다고 가정하지만 일단 유효성 검증
        if (authRepository.existsByEmail(reqDto.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXIST);
        }

        if (authRepository.existsByNickname(reqDto.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXIST);
        }

        // 장르 비트 연산
        if (reqDto.getGenre().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_GENRE);
        }
        Long genre = (long) Genre.fromGenre(reqDto.getGenre());

        // 패스워드 암호화
        String encodedPassword = passwordEncoder.encode(reqDto.getPassword());

        // Users 객체 생성
        Users user = Users.builder()
                .email(reqDto.getEmail())
                .password(encodedPassword)
                .genre(genre)
                .nickname(reqDto.getNickname())
                .build();

        // INSERT
        authRepository.save(user);

        log.info("회원 가입이 정상적으로 처리되었습니다.");

        // 회원가입 이메일 전송
        EmailReqDto emailReqDto = EmailReqDto.builder().to(user.getEmail())
                .subject("Snap Sound 회원가입을 환영합니다!")
                .body("<h1>회원가입 대성공<h1>")
                .build();

        emailPublisherService.sendEmail(emailReqDto);

        return new UserIdResDto(user.getId());
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto reqDto) throws BusinessException {
        // 이메일로 user 객체 조회
        Users user= authRepository.findByEmail(reqDto.getEmail()).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        // 비밀번호 확인
        if(!passwordEncoder.matches(reqDto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // Access Token 발행
        String accessToken = jwtTokenProvider.createAccessToken(user);

        // Refresh Token 발행
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // Refresh Token Redis 저장
        try {
            refreshTokenService.saveRefreshToken(user.getEmail(), refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "서버 내부 토큰 저장 실패. 강하늘에게 문의하세요.");
        }

        // 응답
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(user.getNickname())
                .userId(user.getId())
                .build();
    }

    public void logout(String email) {
        // Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(email);
    }

    public String reissue(TokenUserInfo userInfo) throws BusinessException {
        // Refresh Token이 유효한지 확인
        // => Security에서는 Client가 준 Refresh Token의 기간을,
        //    Auth Service에서는 서버 내부에서 관리하고 있는 Refresh Token의 기간을 검증함
        try {
            refreshTokenService.getRefreshToken(userInfo.getEmail());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NO_TOKEN);
        }

        // Users 객체 찾기
        Users user = authRepository.findByEmail(userInfo.getEmail()).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        // Access Token 발급
        return jwtTokenProvider.createAccessToken(user);
    }
}