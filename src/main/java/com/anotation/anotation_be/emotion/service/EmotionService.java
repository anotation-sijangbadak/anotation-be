package com.anotation.anotation_be.emotion.service;

import com.anotation.anotation_be.auth.entity.Users;
import com.anotation.anotation_be.auth.repo.AuthRepository;
import com.anotation.anotation_be.auth.service.AuthService;
import com.anotation.anotation_be.common.constants.URIConstants;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.EmotionEnum;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.emotion.dto.request.EmotionPredictReqDto;
import com.anotation.anotation_be.emotion.dto.request.TranslateReqDto;
import com.anotation.anotation_be.emotion.dto.response.EmotionPredictResDto;
import com.anotation.anotation_be.emotion.dto.response.TranslateResDto;
import com.anotation.anotation_be.emotion.dto.response.UserEmotionResDto;
import com.anotation.anotation_be.emotion.dto.request.UserPromptReqDto;
import com.anotation.anotation_be.emotion.entity.Emotions;
import com.anotation.anotation_be.emotion.entity.Traces;
import com.anotation.anotation_be.emotion.repo.EmotionRepository;
import com.anotation.anotation_be.emotion.repo.TraceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmotionService {
    private final RestClient restClient;
    private final AuthRepository authRepository;
    private final EmotionRepository emotionRepository;
    private final TraceRepository traceRepository;

    @Value("${papago.api-key-id}")
    private String papagoApiKeyId;
    @Value("${papago.api-key}")
    private String papagoApiKey;

    public EmotionService(RestClient.Builder restClientBuilder, AuthRepository authRepository, EmotionRepository emotionRepository, TraceRepository traceRepository) {
        this.restClient = restClientBuilder.build();
        this.authRepository = authRepository;
        this.emotionRepository = emotionRepository;
        this.traceRepository = traceRepository;
    }


    @Transactional
    public UserEmotionResDto getEmotion(TokenUserInfo userInfo, UserPromptReqDto reqDto) {
        // 사용자의 프롬프트 번역
        String translated = null;
        try {
            translated = restClient.post()
                    .uri(URIConstants.PAPAGO_URI + "/translation")
                    .header("X-NCP-APIGW-API-KEY-ID", papagoApiKeyId)
                    .header("X-NCP-APIGW-API-KEY", papagoApiKey)
                    .header("Content-Type", "application/json")
                    .body(new TranslateReqDto(reqDto.getPrompt()))
                    .retrieve()
                    .body(TranslateResDto.class)
                    .getMessage()
                    .getResult()
                    .getTranslatedText();

            if (translated == null || translated.isBlank()) {
                log.warn("값이 비어있습니다.");
                throw new Exception();
            }
        } catch (Exception e) {
            log.warn("파파고 API 사용 과정에서 번역에 문제가 발생했습니다.");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파파고 번역에 실패하였습니다.");
        }

        // 감정 분류 데이터 모델에게 번역된 문장 전달
        List<EmotionPredictResDto.Result> result = null;
        try {
            result = restClient.post()
                    .uri("http://localhost:9000" + "/predict")
                    .header("Content-Type", "application/json")
                    .body(new EmotionPredictReqDto(translated))
                    .retrieve()
                    .body(EmotionPredictResDto.class)
                    .getResult();

            if (result == null || result.isEmpty()) {
                log.warn("감정 태그를 추출하지 못했습니다. : result 리스트 값 없음");
                throw new Exception();
            }
        } catch (Exception e) {
            log.warn("감정 태그 추출에 실패하였습니다.");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "감정 태그 추출에 실패하였습니다.");
        }

        // 감정 리스트 만들기
        List<EmotionEnum> emotionList = null;
        try {
            emotionList = result.stream()
                    .map(EmotionPredictResDto.Result::getLabel)
                    .map(String::toUpperCase)
                    .map(EmotionEnum::valueOf)
                    .toList();

            if(emotionList.isEmpty()) {
                throw new Exception();
            }
        } catch (Exception e) {
            log.warn("감정 태그 추출 중 문제가 발생했습니다. : python 코드에서 알수없는 감정 태그 토큰 발생");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "감정 태그 추출 중 문제가 발생했습니다. : python 코드에서 알수없는 감정 태그 토큰 발생");
        }

        // User 찾기
        Users user = authRepository.findByEmail(userInfo.getEmail()).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        // 기록 객체 생성
        Traces trace = Traces.builder()
                .prompt(reqDto.getPrompt())
                .user(user)
                .emotions(new ArrayList<>())
                .build();

        // 감정 객체 생성 및 DB 저장
        emotionList.stream()
                .map(emotionEnum ->
                        Emotions.builder()
                                .emotion(emotionEnum)
                                .trace(trace)
                                .build()
                )
                .forEach(trace::addEmotion);

        // 기록 객체 DB 저장
        traceRepository.save(trace);

        return UserEmotionResDto.builder()
                .translatedText(translated)
                .emotionList(
                        emotionList.stream()
                                .map(EmotionEnum::getEmotion_kr)
                                .toList()
                ).build();

        // TODO: 3. 이걸로 Spotify한테 잘 조립해서 쿼리를 날리자
        // TODO: 4. 받은 트랙을 프론트로 보내자
    }
}
