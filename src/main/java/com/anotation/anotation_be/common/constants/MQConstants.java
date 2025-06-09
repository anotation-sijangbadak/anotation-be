package com.anotation.anotation_be.common.constants;

public class MQConstants {
    // EXCHANGE : (from Domain).exchange
    // QUEUE : (to Domain).queue
    // ROUTING_KEY : (from).(to).(task)
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String EMOTION_EXCHANGE = "emotion.exchange";
    public static final String TRACK_EXCHANGE = "track.exchange";

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String TRACK_QUEUE = "track.queue";

    public static final String SIGNUP_KEY = "user.email.signup";
    public static final String TRACK_RECOMMEND_KEY = "emotion.track.recommend";
    public static final String TRACK_CACHING_KEY = "track.track.caching";

    private MQConstants() {
        throw new IllegalStateException("Utility class");
    }
}
