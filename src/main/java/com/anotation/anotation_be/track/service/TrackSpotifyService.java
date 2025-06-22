package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.constants.Constants;
import com.anotation.anotation_be.common.constants.URIConstants;
import com.anotation.anotation_be.common.dto.track.TrackInfoDto;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.track.dto.SpotifyAccessTokenDto;
import com.anotation.anotation_be.track.entity.Tracks;
import com.anotation.anotation_be.track.repo.TrackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackSpotifyService {
    //region AutoWired Object
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final RedisTemplate<String, TrackInfoDto> redisTrackInfoTemplate;
    private final TrackRepository trackRepository;
    //endregion
    //region Constants
    private static final String REDIS_SPOTIFY = "SPOTIFY:ACCESS:TOKEN";
    private static final String REDIS_INDEX = ":INDEX:";
    private static final String REDIS_SIMPLE_TRACK = "SIMPLE:TRACK:";
    private static final String REDIS_TRACK_INFO = "TRACK:INFO:";

    private static final Long SLEEP_MILLI = 100L;
    //endregion
    //region ENV
    @Value("${spotify.client-id}")
    private String spotifyClientId;
    @Value("${spotify.client-secret}")
    private String spotifyClientSecret;
    //endregion

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
}
