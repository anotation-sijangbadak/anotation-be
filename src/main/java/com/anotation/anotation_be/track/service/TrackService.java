package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.constants.Constants;
import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.constants.PromptConstants;
import com.anotation.anotation_be.common.constants.URIConstants;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.EmotionEnum;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.enums.Genre;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.track.dto.SpotifyAccessTokenDto;
import com.anotation.anotation_be.track.dto.TrackInfoDto;
import com.anotation.anotation_be.track.entity.Tracks;
import com.anotation.anotation_be.track.repo.TrackRepository;
import com.anotation.anotation_be.user.entity.Users;
import com.anotation.anotation_be.user.repo.AuthRepository;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackService {
    //region AutoWired Object
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final RedisTemplate<String, TrackInfoDto> redisTrackInfoTemplate;
    private final TrackRepository trackRepository;
    private final AuthRepository authRepository;
    //endregion
    //region Constants
    private static final String REDIS_SPOTIFY = "SPOTIFY:ACCESS:TOKEN";
    private static final String REDIS_INDEX = ":INDEX:";
    private static final String REDIS_SIMPLE_TRACK = "SIMPLE:TRACK:";
    private static final String REDIS_TRACK_INFO = "TRACK:INFO:";

    private static final int COUNT_PER_MQ = 3;
    private static final Long SLEEP_MILLI = 100L;
    //endregion
    //region ENV
    @Value("${gpt.bearer-token}")
    private String gptToken;
    @Value("${spotify.client-id}")
    private String spotifyClientId;
    @Value("${spotify.client-secret}")
    private String spotifyClientSecret;
    //endregion

    //region 추천 음악 기본 정보 Redis 저장 로직

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
    //endregion
    //region 추천 음악 실제 정보 Redis 저장 로직

    /**
     * <pre>
     * MQ 메세지 처리 로직
     * 1. 전달받은 DTO의 index 값에 따라 약간의 시간 지연
     * 2. 변환 가능한 후보군 검색
     * 3. Spotify Access Token 유효성 확인 및 재발급
     * 4. Spotify API에서 실제 트랙 정보 응답
     * 5. Redis에 실제 트랙 정보 저장
     * </pre>
     *
     * @param reqDto email, index
     */
    public void recommendTrackInfoCaching(RedisTrackIndexDto reqDto) throws BusinessException {
        // index 값에 따라 시간 지연 발생시키기 (Too many request 방지)
        if (reqDto.getIndex() > 0) {
            try {
                Thread.sleep(SLEEP_MILLI * reqDto.getIndex());
            } catch (InterruptedException e) {
                // Thread.sleep()에 실패하더라도 로직 진행
                log.error("Index에 따른 시간 지연 과정에서 문제가 발생하였습니다.");
            }
        }

        // 변환 가능한 Simple Track DTO가 있는 지 확인
        Set<String> remainKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + reqDto.getEmail() + ":*");
        if (remainKeys.isEmpty()) {
            log.info("모든 정보 캐싱이 끝났습니다.");
            return;
        }

        log.info("---------------------------- 스포티파이에 곡 검증 시작 ------------------------------");

        // Access Token이 있는 지 확인
        String spotifyAccessToken;
        if (redisStringTemplate.hasKey(REDIS_SPOTIFY)) { // Token이 있다면
            spotifyAccessToken = redisStringTemplate.opsForValue().get(REDIS_SPOTIFY);
        } else { // Token이 없다면
            log.info("토큰 재발급을 시작합니다.");
            spotifyAccessToken = getSpotifyAccessToken();
        }

        String key = remainKeys.iterator().next(); // 키 하나 꺼내기
        SimpleTrackDto trackDto = redisSimpleTrackTemplate.opsForValue().get(key);
        redisSimpleTrackTemplate.delete(key); // 키 삭제
        if (trackDto == null) {
            log.warn("트랙 정보가 없습니다. Redis 내부에 빈 객체가 발견되어 진행을 종료합니다.");
            return;
        } else {
            log.info("트랙 정보 가져오기 성공 : {}", trackDto);
        }

        // -------------------------------------------------------------------------------------
        // TODO Circuit Breaker 도입 필요
        // Spotify API 호출
        String resultTracks = getTracksQueryFromSpotify(trackDto, spotifyAccessToken, false);
        // TrackInfoDto로 변환
        TrackInfoDto trackInfoDto = getTrackInfoDto(resultTracks);
        if (trackInfoDto == null) { // 실제 데이터가 존재하지 않는다면?
            log.error("트랙 정보를 변환하는 데 실패했습니다.");
            // 가수명은 유지한 채로 (감정을 설명하는 단어일 가능성이 높은)title을 단순 쿼리 단어로 사용하여 검색
            resultTracks = getTracksQueryFromSpotify(trackDto, spotifyAccessToken, true);
            trackInfoDto = getTrackInfoDto(resultTracks);
        }

        if (trackInfoDto == null) {
            log.error("대체 쿼리 요청이 실패하였습니다. 진행을 종료합니다.");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "존재하는 트랙이 없습니다.");
        }
        // -------------------------------------------------------------------------------------

        // Track DB에 존재하지 않는 곡이었다면 저장
        boolean present = trackRepository.findBySpotifyId(trackInfoDto.getSpotifyId()).isPresent();
        if (!present) {
            Tracks track = Tracks.builder()
                    .album(trackInfoDto.getAlbum())
                    .artist(trackInfoDto.getArtist())
                    .title(trackInfoDto.getTitle())
                    .releaseDate(trackInfoDto.getReleaseDate())
                    .thumbnail(trackInfoDto.getThumbnail())
                    .popularity(trackInfoDto.getPopularity())
                    .spotifyId(trackInfoDto.getSpotifyId())
                    .build();

            trackRepository.save(track);
        }

        // 랜덤 인덱스 생성
        int randomIndex = (int) (Math.random() * Constants.REDIS_INDEX_RANGE - 1) + Constants.REDIS_INDEX_RANGE + 1;
        trackInfoDto.setIndex(randomIndex);

        // Redis에 TrackInfoDto 저장
        redisTrackInfoTemplate.opsForValue().set(REDIS_TRACK_INFO + reqDto.getEmail() + REDIS_INDEX + randomIndex, trackInfoDto, Duration.ofMinutes(10));
    }

    /**
     * Spotify API에 접근하기 위한 Access Token을 발급받는 로직
     *
     * @return Access Token
     */
    private String getSpotifyAccessToken() throws BusinessException {
        SpotifyAccessTokenDto response;
        try {
            // 요청 바디 생성
            String requestBody = UriComponentsBuilder.newInstance()
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_id", spotifyClientId)
                    .queryParam("client_secret", spotifyClientSecret)
                    .build()
                    .encode()
                    .toUri()
                    .getRawQuery();

            // 요청 보내기
            response = restClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_REQUEST_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_SERVER_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                    })
                    .body(SpotifyAccessTokenDto.class);

        } catch (Exception e) {
            log.error("Spotify API로 부터 Access Token을 전달받는 과정에서 문제가 발생했습니다.");
            throw e;
        }

        if (response != null) {
            // Redis에 Spotify Access Token 저장
            redisStringTemplate.opsForValue().set(REDIS_SPOTIFY, response.getAccessToken(), Duration.ofSeconds(response.getExpiresIn()));
            log.info("스포티파이 Access Token 저장 완료! : {}", response.getAccessToken());

            return response.getAccessToken();
        } else {
            log.error("요청은 정상적으로 처리되었으나, 응답으로 null이 수신됨.");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "토큰을 발급받지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 기본 트랙 정보를 바탕으로 실제 트랙의 정보를 받아오는 로직
     *
     * @param trackDto           title, artist, reason
     * @param spotifyAccessToken Spotify API에 접근하기 위한 Access Token
     * @param isAlternative      true : 대체 트랙 찾기 / false : 기본 정보와 일치하는 트랙 찾기
     * @return 응답 바디 (String)
     */
    private String getTracksQueryFromSpotify(SimpleTrackDto trackDto, String spotifyAccessToken, boolean isAlternative) {
        // 검색을 위한 쿼리 생성
        String searchParam;
        if (isAlternative)
            searchParam = "artist:" + trackDto.getArtist().trim() + " " + trackDto.getTitle(); // 대체 쿼리라면 track 키워드 삭제
        else searchParam = "artist:" + trackDto.getArtist().trim() + " track:" + trackDto.getTitle(); // 아니라면 정상 쿼리 작성

        // 조건을 작성한 URI 변환
        String searchUri = UriComponentsBuilder.fromUriString(URIConstants.SPOTIFY_URI)
                .queryParam("q", searchParam)
                .queryParam("type", "track") // 단일 트랙만 조회
                .queryParam("market", "KR") // 한국 시장에 등록된 곡만 조회
                .queryParam("limit", "10") // 최대 검색 결과 10개로 제한
                .build()
                .toUriString();
        log.info("URI 변환 성공 : {}", searchUri);

        // Spotify API로 부터 검색 결과 조회
        String searchResult;
        try {
            searchResult = restClient.get()
                    .uri(searchUri)
                    .header("Authorization", "Bearer " + spotifyAccessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_REQUEST_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_SERVER_ERROR, "에러 응답 코드 : " + res.getStatusCode());
                    })
                    .body(String.class);
        } catch (Exception e) {
            log.warn("Spotify에서 트랙 정보를 얻어오지 못했습니다.");
            throw e;
        }

        log.info("결과 쿼리: {}", searchResult);
        return searchResult;
    }

    /**
     * Spotify에게 받은 응답 바디(String)를 Track Info Dto로 변환
     *
     * @param resultTracks 응답 바디(String)
     * @return TrackInfoDto
     */
    private TrackInfoDto getTrackInfoDto(String resultTracks) {
        TrackInfoDto trackInfoDto;
        try {
            // String 응답 바디를 JSON 형식으로 변환
            JsonNode root = objectMapper.readTree(resultTracks);
            JsonNode items = root.path("tracks").path("items");
            if (items.isNull() || items.isEmpty()) {
                log.warn("아무 결과도 응답받지 못했습니다.");
                return null;
            }

            JsonNode firstItem = items.get(0);

            trackInfoDto = TrackInfoDto.builder()
                    .spotifyId(firstItem.path("id").asText())
                    .title(firstItem.path("name").asText())
                    .artist(firstItem.path("artists").get(0).path("name").asText())
                    .album(firstItem.path("album").path("name").asText())
                    .thumbnail(firstItem.path("album").path("images").get(0).path("url").asText())
                    .popularity(firstItem.path("popularity").asInt())
                    .releaseDate(firstItem.path("album").path("release_date").asText())
                    .build();

        } catch (Exception e) {
            log.error("Spotify API로 부터 받은 응답 데이터를 JSON 파싱하는 과정에서 오류가 발생했습니다.");
            return null;
        }

        return trackInfoDto;
    }

    //endregion

    public TrackInfoDto getTrack(TokenUserInfo userInfo) {
        // 전달할 Track 정보가 있는지 확인
        Set<String> keys = redisTrackInfoTemplate.keys(REDIS_TRACK_INFO + userInfo.getEmail() + REDIS_INDEX + "*");
        if (keys.isEmpty()) {
            Set<String> simpleTrackKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + userInfo.getEmail() + REDIS_INDEX + "*");
            if (!simpleTrackKeys.isEmpty()) {
                // 기본 정보 있는 거 하나 바꾸기
                rabbitTemplate.convertAndSend(
                        MQConstants.TRACK_EXCHANGE,
                        MQConstants.TRACK_CACHING_KEY,
                        new RedisTrackIndexDto(userInfo.getEmail(), 0)
                );

                // 3개씩 전파하는데도 실제 데이터가 없었다는 것은 GPT 신뢰도가 바닥이라는 것
                return TrackInfoDto.builder().index(-1).build();
            }

            // 모두 변환했기 때문에 정보가 없음을 전달
            return null;
        }

        // 저장되어 있는 DTO를 꺼내기
        String key = keys.iterator().next();
        TrackInfoDto resDto = redisTrackInfoTemplate.opsForValue().get(key);

        if (resDto == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Redis에서 받아온 정보가 비어있습니다.");
        }

        // 다시 조회되지 않도록 키 이름 변경
        redisTrackInfoTemplate.rename(key, "SEND:INFO:" + userInfo.getEmail() + REDIS_INDEX + resDto.getIndex());

        // 다음으로 계속
        Set<String> simpleTrackKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + userInfo.getEmail() + REDIS_INDEX + "*");
        if (!simpleTrackKeys.isEmpty()) {
            for (int i = 0; i < COUNT_PER_MQ; i++) {
                rabbitTemplate.convertAndSend(
                        MQConstants.TRACK_EXCHANGE,
                        MQConstants.TRACK_CACHING_KEY,
                        new RedisTrackIndexDto(userInfo.getEmail(), i)
                );
            }
        }

        return resDto;
    }


}
