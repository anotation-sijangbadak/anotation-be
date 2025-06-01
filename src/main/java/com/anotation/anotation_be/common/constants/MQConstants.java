package com.anotation.anotation_be.common.constants;

public class MQConstants {
    public static final String SIGNUP_EXCHANGE = "auth.user.exchange";
    public static final String SIGNUP_QUEUE = "email.user.queue";
    public static final String SIGNUP_ROUTING_KEY = "user.signup";

    private MQConstants() {
        throw new IllegalStateException("Utility class");
    }
}
