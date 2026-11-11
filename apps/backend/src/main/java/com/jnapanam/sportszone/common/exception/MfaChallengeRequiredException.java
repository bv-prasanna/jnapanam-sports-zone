package com.jnapanam.sportszone.common.exception;

/** Distinguishes a required second factor from an invalid password without leaking auth details. */
public class MfaChallengeRequiredException extends UnauthorizedException {
    public MfaChallengeRequiredException() { super("MFA challenge required"); }
}
