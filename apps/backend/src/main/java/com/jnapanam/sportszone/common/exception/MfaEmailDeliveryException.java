package com.jnapanam.sportszone.common.exception;
/** SMTP/provider failure: never claim MFA email was sent or persist an unusable challenge. */
public class MfaEmailDeliveryException extends RuntimeException {
    public MfaEmailDeliveryException(){super("Sign-in verification email could not be delivered");}
}
