package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.constants.PromptConstants;
import com.anotation.anotation_be.common.constants.URIConstants;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.common.enums.EmotionEnum;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.enums.Genre;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackGptService {
    //region AutoWired Object
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate;
    //endregion
    //region Constants
    private static final String REDIS_INDEX = ":INDEX:";
    private static final String REDIS_SIMPLE_TRACK = "SIMPLE:TRACK:";

    private static final int COUNT_PER_MQ = 3;
    //endregion
    //region ENV
    @Value("${gpt.bearer-token}")
    private String gptToken;
    //endregion



    /**
     * <pre>
     * MQ 메세지 처리 로직
     * 1. 프롬프트 생성
     * 2. OpenAI API에 프롬프트 전달 및 응답 처리
     * 3. 응답 데이터 DTO(SimpleTrackDto) 변환
     * 4. Redis에 DTO 저장
     * 5. 음악 정보 n개 실제 곡으로 변환 -> 비동기(MQ)
     * </pre>
     *
     * @param reqDto email, userInput, emotionList, genreList
     * @return List{title, artist, reason}
     */
    public List<SimpleTrackDto> recommendMusicCaching(GPTEmotionReqDto reqDto) throws BusinessException, IllegalArgumentException, AmqpException {
        String requestBody = getPrompt(reqDto); // 프롬프트 생성
        log.info("프롬프트 : {}", requestBody);

        LocalDateTime startTime = LocalDateTime.now();
        String response = getResponseByGPT(requestBody); // GPT API 호출
        List<SimpleTrackDto> recommendTracks = getSimpleTrackList(response); // 추천 데이터 꺼내서 DTO로 변환하기
        log.info("걸린 시간 (초) : {} 초", LocalDateTime.now().getSecond() - startTime.getSecond());

        // Redis에 저장하기
        for (int i = 0; i < recommendTracks.size(); i++) {
            // ex) SIMPLE:TRACK:hwaha0824@gmail.com:INDEX:1
            redisSimpleTrackTemplate.opsForValue().set(
                    REDIS_SIMPLE_TRACK + reqDto.getEmail() + REDIS_INDEX + i,
                    recommendTracks.get(i),
                    Duration.ofMinutes(10L)
            );
            log.info("title: {}, artist: {}", recommendTracks.get(i).getTitle(), recommendTracks.get(i).getArtist());
        }

        // 음악 정보 n개씩 검증 및 저장 비동기 처리
        Set<String> simpleTrackKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + reqDto.getEmail() + REDIS_INDEX + "*");
        if (!simpleTrackKeys.isEmpty()) {
            for (int i = 0; i < COUNT_PER_MQ; i++) {
                rabbitTemplate.convertAndSend(
                        MQConstants.TRACK_EXCHANGE,
                        MQConstants.TRACK_CACHING_KEY,
                        new RedisTrackIndexDto(reqDto.getEmail(), i)
                );
            }
        }

        // 테스트를 위한 return 문
        return recommendTracks;
    }

    /**
     * 프롬프트를 만드는 함수
     *
     * @param reqDto email, userInput, emotionList, genreList
     * @return prompt
     */
    private String getPrompt(GPTEmotionReqDto reqDto) {
        // 검색 기간 랜덤 (최근 10 ~ 15년 사이)
        int yearDuration = (int) (Math.random() * 5) + 10;

        // 감정 태그 리스트 문자열화
        String emotionStr = getEmotionStr(reqDto.getEmotionList());

        // 장르 리스트 문자열화
        String genreStr = getGenreStr(reqDto.getGenreList());

        // 장르 프롬프트
        String genrePrompt = ""; // TODO: switch case로 장르별로 프롬프트 세분화

        String prompt = PromptConstants.PROMPT
                .formatted(reqDto.getUserInput(), emotionStr, genreStr, yearDuration, genrePrompt)
                .replace("\"", "\\\"")
                .replace("\n", "\\n");


        return PromptConstants.PROMPT_CONFIG.formatted(prompt);
    }

    /**
     * 프롬프트에 들어갈 Genre Enum 값을 String으로 변환
     *
     * @param genreList Genre Enum List
     */
    private String getGenreStr(List<Genre> genreList) {
        StringBuilder sb = new StringBuilder();
        for (Genre genre : genreList) {
            sb.append(genre.name().toLowerCase());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    /**
     * 프롬프트에 들어갈 Emotion Enum 값을 String으로 변환
     */
    private String getEmotionStr(List<EmotionEnum> emotionList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emotionList.size(); i++) {
            String emotion = emotionList.get(i).name().toLowerCase();
            sb.append(i + 1);
            sb.append(". ");
            sb.append(emotion);

            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * GPT에게 생성한 프롬프트로 음악 추천 요청 전달
     *
     * @param requestBody 요청 바디
     * @return 응답 바디 (String)
     * @throws BusinessException GPT 응답 오류
     */
    private String getResponseByGPT(String requestBody) throws BusinessException {
        String response;
        response = restClient.post()
                .uri(URIConstants.GPT_URI)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + gptToken)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BusinessException(ErrorCode.EXTERNAL_API_REQUEST_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new BusinessException(ErrorCode.EXTERNAL_API_SERVER_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                })
                .body(String.class);

        return response;
    }

    /**
     * GPT 응답 데이터 DTO 변환
     *
     * @param response GPT 응답 바디(String)
     * @return List {title, artist, reason}
     * @throws BusinessException JSON 파싱 오류
     */
    private List<SimpleTrackDto> getSimpleTrackList(String response) throws BusinessException {
        List<SimpleTrackDto> recommendTracks;
        try {
            // response를 JsonNode에 올리기
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GPT가 정보를 주지 않았어요..");
            }

            // content 꺼내기
            String generatedText = choices.get(0).path("message").path("content").asText();
            log.info("GPT API로 부터 받은 응답 : {}", generatedText);

            // content를 List<SimpleTrackDto>로 변환
            recommendTracks = objectMapper.readValue(generatedText, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GPT 응답 바디 JSON 파싱 과정에서 문제가 발생했습니다.");
        }

        return recommendTracks;
    }
}
