package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.MfaChallengeRequiredException;
import com.jnapanam.sportszone.identity.dto.LoginRequest;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.*;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.security.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceLoginMfaTest {
 @Test void incorrectPasswordNeverStartsMfaOrSendsCode(){
  UserRepository users=mock(UserRepository.class);
  AuthenticationManager manager=mock(AuthenticationManager.class);
  MfaService mfa=mock(MfaService.class);
  User user=mock(User.class);
  when(user.getEmail()).thenReturn("customer@example.com");
  when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
  when(manager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
    .thenThrow(new BadCredentialsException("Invalid password"));
  AuthService auth=new AuthService(users,mock(RoleRepository.class),mock(RefreshTokenRepository.class),
    mock(PasswordEncoder.class),manager,mock(TokenService.class),mfa,mock(NotificationService.class));

  assertThrows(BadCredentialsException.class,()->auth.login(
    new LoginRequest("customer@example.com","wrong",null,null,null)));
  verifyNoInteractions(mfa);
 }

 @Test void validPasswordWithoutCodeRequestsMfaBeforeIssuingTokens(){
  UserRepository users=mock(UserRepository.class);
  AuthenticationManager manager=mock(AuthenticationManager.class);
  MfaService mfa=mock(MfaService.class);
  User user=mock(User.class);
  when(user.getEmail()).thenReturn("customer@example.com");
  when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
  when(manager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
    .thenAnswer(invocation->invocation.getArgument(0));
  doThrow(new MfaChallengeRequiredException()).when(mfa).verifyOrChallenge(user,null);
  RefreshTokenRepository tokensRepo=mock(RefreshTokenRepository.class);
  AuthService auth=new AuthService(users,mock(RoleRepository.class),tokensRepo,
    mock(PasswordEncoder.class),manager,mock(TokenService.class),mfa,mock(NotificationService.class));

  assertThrows(MfaChallengeRequiredException.class,()->auth.login(
    new LoginRequest("customer@example.com","correct",null,null,null)));
  verify(mfa).verifyOrChallenge(user,null);
  verifyNoInteractions(tokensRepo);
 }
}
