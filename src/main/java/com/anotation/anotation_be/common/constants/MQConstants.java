package com.anotation.anotation_be.common.constants;

public class MQConstants {
    public static final String SIGNUP_EXCHANGE = "auth.user.exchange";
    public static final String SIGNUP_QUEUE = "email.user.queue";
    public static final String SIGNUP_ROUTING_KEY = "user.signup";

    public static final String EMOTION_SEND_EXCHANGE = "emotion.recommend.exchange";
    public static final String EMOTION_SEND_QUEUE = "track.recommend.queue";
    public static final String EMOTION_SEND_ROUTING_KEY = "recommend.emotion.send";
    public static final String EMOTION_CACHE_TRACK_KEY = "recommend.track.cache";

    private MQConstants() {
        throw new IllegalStateException("Utility class");
    }
}
