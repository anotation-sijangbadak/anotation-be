package com.anotation.anotation_be.track.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
public class SpotifyAccessTokenDto {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;
}
