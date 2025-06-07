package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.constants.URIConstants;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.track.dto.SpotifyAccessTokenDto;
import com.anotation.anotation_be.track.dto.TrackInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final RedisTemplate<String, TrackInfoDto> redisTrackInfoTemplate;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${gpt.bearer-token}")
    private String gptToken;
    @Value("${spotify.client-id}")
    private String spotifyClientId;
    @Value("${spotify.client-secret}")
    private String spotifyClientSecret;

    //region 음악 추천 받기 및 추천받은 트랙 기본 정보 캐싱
    public List<SimpleTrackDto> recommendMusicCaching(GPTEmotionReqDto reqDto) {
        // 프롬프트 생성
        String requestBody = getPrompt(reqDto);
        log.info("프롬프트 : {}", requestBody);

        LocalDateTime startTime = LocalDateTime.now();
        // GPT API 호출
        String response = getResponseByGPT(requestBody);

        // 추천 데이터 꺼내서 DTO로 변환하기
        List<SimpleTrackDto> recommendTracks = getSimpleTrackList(response);
        log.info("걸린 시간 (초) : {} 초", LocalDateTime.now().getSecond() - startTime.getSecond());

        // Redis에 저장하기
        for (int i = 0; i < recommendTracks.size(); i++) {
            // SIMPLE:TRACK:hwaha0824@gmail.com:INDEX:1
            log.info("title: {}, artist: {}", recommendTracks.get(i).getTitle(), recommendTracks.get(i).getArtist());
            redisSimpleTrackTemplate.opsForValue().set("SIMPLE:TRACK:" + reqDto.getEmail() + ":INDEX:" + i, recommendTracks.get(i), Duration.ofMinutes(10L));
        }

        // 음악 정보 2개씩 검증 및 저장 비동기 처리
        for (int i = 0; i < 3; i++) { // TODO: 이거 Constants로 관리
            rabbitTemplate.convertAndSend(MQConstants.EMOTION_SEND_EXCHANGE, MQConstants.EMOTION_CACHE_TRACK_KEY, new RedisTrackIndexDto(reqDto.getEmail(), i));
        }

        // 테스트를 위한 return 문
        return recommendTracks;
    }

    //region GPT API 요청 및 응답 처리
    private List<SimpleTrackDto> getSimpleTrackList(String response) {
        List<SimpleTrackDto> recommendTracks;
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GPT가 정보를 주지 않았어요..");
            }

            // content 꺼내기
            String generatedText = choices.get(0).path("message").path("content").asText();
            log.info(generatedText);
            recommendTracks = objectMapper.readValue(generatedText, new TypeReference<List<SimpleTrackDto>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JSON 파싱 과정에서 문제가 발생했습니다.");
        }
        return recommendTracks;
    }

    private String getResponseByGPT(String requestBody) {
        String response;
        try {
            response = restClient.post()
                    .uri(URIConstants.GPT_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + gptToken)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return response;
    }
    //endregion

    //region 프롬프트 생성
    private String getPrompt(GPTEmotionReqDto reqDto) {
        // 검색 기간 랜덤 (최근 10 ~ 15년 사이)
        int yearDuration = (int) (Math.random() * 5) + 10;

        // 감정 태그 리스트 문자열화
        String emotionStr = getEmotionStr(reqDto);

        // 장르 리스트 문자열화
        String genreStr = getGenreStr(reqDto);

        String prompt = """
                [시스템]
                - 당신은 감정에 민감한 음악 큐레이터입니다.
                - 사용자는 이렇게 표현했습니다: "%s"
                - 당신은 사용자의 표현을 분석하여 다음과 같은 감정을 파악했습니다:
                  %s
                - 감정은 중요도 순으로 나열되어 있으며, 1순위 감정에 가장 높은 가중치를 두고, 이후 순위에 따라 중요도를 낮추어 고려하세요.
                - 사용자가 선호하는 음악 장르는 %s입니다.
                
                [지침]
                - 파악된 감정과 장르를 기준으로, 감정 해소나 증폭을 시도하지 말고 감정 자체와 밀접한 음악을 추천하세요.
                - 감정 우선순위에 따라, 1순위 감정과 가장 관련 깊은 곡을 우선적으로 추천하고, 2순위, 3순위 감정과도 연관성을 고려해 보완하세요.
                - 추천하는 곡은 최근 %d년 이내에 발매된, **한국 시장**에 등록된 음악 중에서 유명한 곡만 나오지 않게 선택하세요.
                - 추천 곡 수는 **10곡 이상 15곡 이하**로 제한하세요.
                
                [포맷]
                - 각 곡을 추천한 이유를 곡의 가사, 멜로디, 아티스트의 특성 등을 활용하여 `reason`에 반드시 명시하세요.
                - 결과는 반드시 **JSON 배열** 형태로, 각 항목은 `title`(노래 제목)과 `artist`(아티스트명), `reason`(선정 이유)만 포함하세요.
                  예시: [{"title": "밤편지", "artist": "아이유", "reason": "아이유의 특유의 음색이 제시한 감정과 잘 부합함"}, {"title": "사건의 지평선", "artist": "윤하", "reason": "멜로디의 진행과 내부에 사용된 악기가 제시된 감정과 잘 부합함" }]
                - 추가 텍스트, 설명, 주석 없이 결과 배열만 출력하세요.
                
                [검증 조건]
                - **존재하지 않는 노래 제목이나 허구의 아티스트명**은 절대 생성하지 마세요.
                - **모르겠거나 확신이 없는 경우**, 절대 추천하지 말고 빈 배열([])을 반환하세요.
                - **감정 단어(예시: lonely, sadness 등)가 title에 그대로 들어가면 무효**입니다.
                - 당신이 존재 여부에 확신이 90퍼센트 이상일때만 추천하세요.
                - **곡명과 아티스트명을 변형하거나 번역하지 말고**, 원래의 공식 이름(한글 제목 포함)을 그대로 사용하세요.
                - **한국 정식 음원 서비스(스포티파이)에서 확인 가능한 곡만 추천**하세요.
                """.formatted(reqDto.getUserInput(), emotionStr, genreStr, yearDuration);

        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        return """
                {
                  "model": "gpt-4.1",
                  "messages": [
                              {
                                "role": "system",
                                "content": "%s"
                              }
                            ],
                  "temperature": 0.2,
                  "top_p": 0.5,
                  "frequency_penalty": 0,
                  "presence_penalty": 0,
                  "n": 1
                }
                """.formatted(escapedPrompt);
    }

    private String getGenreStr(GPTEmotionReqDto reqDto) {
        StringBuilder sb = new StringBuilder();
        List<String> genreList = reqDto.getGenreList();
        for (String genre : genreList) {
            sb.append(genre);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    private String getEmotionStr(GPTEmotionReqDto reqDto) {
        StringBuilder sb = new StringBuilder();
        List<String> emotionList = reqDto.getEmotionList();
        for (int i = 0; i < emotionList.size(); i++) {
            String emotion = emotionList.get(i);
            sb.append(i + 1);
            sb.append(". ");
            sb.append(emotion);

            sb.append("\n");
        }
        return sb.toString();
    }
    //endregion
    //endregion

    //region 추천 트랙 결과 검증 및 캐싱 로직
    public void recommendTrackInfoCaching(RedisTrackIndexDto reqDto) {
        if (reqDto.getIndex() > 0) {
            try {
                Thread.sleep(100L * reqDto.getIndex());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Set<String> remainKeys = redisSimpleTrackTemplate.keys("SIMPLE:TRACK:" + reqDto.getEmail() + ":*");
        if (remainKeys.isEmpty()) {
            log.info("모든 정보 캐싱이 끝났습니다.");
            return;
        }

        log.info("---------------------------- 스포티파이에 곡 검증 시작 ------------------------------");
        // Access Token이 있는 지 확인
        String spotifyAccessToken;
        if (redisStringTemplate.hasKey("SPOTIFY:ACCESS:TOKEN")) {
            spotifyAccessToken = redisStringTemplate.opsForValue().get("SPOTIFY:ACCESS:TOKEN");
        } else {
            log.info("토큰 재발급을 시작합니다.");
            spotifyAccessToken = getSpotifyAccessToken();
        }

        log.info("토큰 발급 성공 : {}", spotifyAccessToken);

        String key = remainKeys.iterator().next();
        SimpleTrackDto trackDto = redisSimpleTrackTemplate.opsForValue().get(key);
        redisSimpleTrackTemplate.delete(key);
        log.info("트랙 정보 가져오기 성공 : {}", trackDto); // TODO 여기 구조 조금 이상한데 좀 제대로 바꾸자

        // spotify api 호출
        String resultTracks;
        if (trackDto != null) {
            resultTracks = getTracksQueryFromSpotify(trackDto, spotifyAccessToken);
            if(resultTracks == null){
                // TODO: 여기에 다시 요청을 보내는 로직을 작성해주세요
                return;
            }
        } else {
            log.error("트랙 정보가 없습니다.");
            return;
        }

        // TrackInfoDto로 변환
        TrackInfoDto trackInfoDto = getTrackInfoDto(reqDto, resultTracks);
        if (trackInfoDto == null) {
            log.error("트랙 정보를 변환하는 데 실패했습니다.");
            return;
        }

        // Redis에 TrackInfoDto 저장
        int randomIndex = (int) (Math.random() * 999) + 1001; // TODO 이거도 Constants로 관리
        trackInfoDto.setIndex(randomIndex);
        redisTrackInfoTemplate.opsForValue().set("TRACK:INFO:" + reqDto.getEmail() + ":INDEX:" + randomIndex, trackInfoDto, Duration.ofMinutes(10));
    }

    private TrackInfoDto getTrackInfoDto(RedisTrackIndexDto reqDto, String resultTracks) {
        // TODO try catch 처리 똑바로 해놓기

        JsonNode root;
        try {
            root = objectMapper.readTree(resultTracks);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode items = root.path("tracks").path("items");
        if (items.isNull() || items.isEmpty()) {
            log.warn("아무 결과도 응답받지 못했습니다.");
            // ai가 거짓말 친거니까 다시 보내자
            rabbitTemplate.convertAndSend(MQConstants.EMOTION_SEND_EXCHANGE, MQConstants.EMOTION_CACHE_TRACK_KEY, new RedisTrackIndexDto(reqDto.getEmail(), reqDto.getIndex()));
            return null;
        }

        JsonNode firstItem = items.get(0);

        String spotifyId = firstItem.path("id").asText();
        String title = firstItem.path("name").asText();
        String artist = firstItem.path("artists").get(0).path("name").asText();
        String album = firstItem.path("album").path("name").asText();
        String thumbnail = firstItem.path("album").path("images").get(0).path("url").asText();
        int popularity = firstItem.path("popularity").asInt();
        String releaseDate = firstItem.path("album").path("release_date").asText();

        log.info("id : {}, title: {}, artist: {}, album: {}, thumbnail: {}, popularity: {}, releaseDate: {}", spotifyId, title, artist, album, thumbnail, popularity, releaseDate);

        return TrackInfoDto.builder()
                .spotifyId(spotifyId)
                .title(title)
                .artist(artist)
                .album(album)
                .thumbnail(thumbnail)
                .popularity(popularity)
                .releaseDate(releaseDate)
                .build();
    }

    private String getTracksQueryFromSpotify(SimpleTrackDto trackDto, String spotifyAccessToken) {
        String seachUri = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/search")
                .queryParam(
                        "q",
                        "artist:" + trackDto.getArtist() + " track:" + trackDto.getTitle())
                .queryParam("type", "track")
                .queryParam("market", "KR")
                .queryParam("limit", "10")
                .build()
                .toUriString();
        log.info("URI 변환 성공 : {}", seachUri);

        String searchResult;
        try {
            searchResult = restClient.get()
                    .uri(seachUri)
                    .header("Authorization", "Bearer " + spotifyAccessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            log.warn("아마 너무 많이 보내서 락걸린듯 ㅋㅋ");
            return null;
        }

        log.info("결과 쿼리: " + searchResult);
        return searchResult;
    }

    private String getSpotifyAccessToken() {
        SpotifyAccessTokenDto response = null;
        try {
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
                    .body(SpotifyAccessTokenDto.class);
        } catch (Exception e) {
            log.error("스포티파이 API가 Access Token을 던져주지 않아요.. ㅠㅠ");
        }

        if (response != null) {
            redisStringTemplate.opsForValue().set("SPOTIFY:ACCESS:TOKEN", response.getAccessToken(), Duration.ofSeconds(response.getExpiresIn()));
            log.info("스포티파이 Access Token 저장 완료! : {}", response.getAccessToken());
            return response.getAccessToken();
        }

        log.error("응답은 받았는데 null 값이 도착함. 확인해보세요.");
        return null;
    }
    //endregion

    public TrackInfoDto getTrack(TokenUserInfo userInfo) {
        Set<String> keys = redisTrackInfoTemplate.keys("TRACK:INFO:" + userInfo.getEmail() + ":INDEX:*");
        if (keys.isEmpty()) {
            return null;
        }

        // 저장되어 있는 DTO를 꺼내기
        String key = keys.iterator().next();
        TrackInfoDto resDto = redisTrackInfoTemplate.opsForValue().get(key);

        // 나중에 스냅으로 저장하기 위한 뭔가 뭔가가 필요함.
        redisTrackInfoTemplate.delete(key);

        // 다음으로 계속
        rabbitTemplate.convertAndSend(MQConstants.EMOTION_SEND_EXCHANGE, MQConstants.EMOTION_CACHE_TRACK_KEY, new RedisTrackIndexDto(userInfo.getEmail(), 0));

        return resDto;
    }

}
