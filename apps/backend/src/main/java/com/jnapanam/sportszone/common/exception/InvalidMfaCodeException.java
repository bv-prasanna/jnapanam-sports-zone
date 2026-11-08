package com.jnapanam.sportszone.common.exception;
/** A password-verified sign-in supplied an invalid or expired second factor. */
public class InvalidMfaCodeException extends UnauthorizedException {
    public InvalidMfaCodeException(){super("Invalid or expired MFA code");}
}
