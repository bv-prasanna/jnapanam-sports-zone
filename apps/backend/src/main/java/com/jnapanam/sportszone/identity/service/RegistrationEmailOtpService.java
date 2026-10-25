package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationEmailOtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String smtpHost, smtpUser, smtpPassword, smtpFrom;
    private final int smtpPort;
    private final boolean smtpAuth, smtpTls;

    public RegistrationEmailOtpService(JdbcTemplate jdbc, PasswordEncoder encoder,
        @Value("${spring.mail.host:localhost}") String smtpHost,
        @Value("${spring.mail.port:587}") int smtpPort,
        @Value("${spring.mail.username:}") String smtpUser,
        @Value("${spring.mail.password:}") String smtpPassword,
        @Value("${app.notification.email.from:}") String smtpFrom,
        @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
        @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean smtpTls) {
        this.jdbc=jdbc; this.encoder=encoder;
        this.smtpHost=smtpHost; this.smtpPort=smtpPort;
        this.smtpUser=smtpUser; this.smtpPassword=smtpPassword;
        this.smtpFrom=smtpFrom; this.smtpAuth=smtpAuth; this.smtpTls=smtpTls;
    }

    @Transactional
    public void request(String rawEmail) {
        String email=normalize(rawEmail);
        if (email==null) throw new BadRequestException("Invalid email");
        Integer cooldown=jdbc.queryForObject(
            "select count(*) from registration_email_otp_challenges where email=? and created_at>now()-interval '60 seconds'",
            Integer.class,email);
        if(cooldown!=null&&cooldown>0) throw new ConflictException("Please wait before requesting another OTP");
        Integer count=jdbc.queryForObject(
            "select count(*) from registration_email_otp_challenges where email=? and created_at>now()-interval '1 hour'",
            Integer.class,email);
        if(count!=null&&count>=5) throw new ConflictException("Email OTP request limit reached");
        String otp=String.format("%06d",RANDOM.nextInt(1_000_000));
        jdbc.update("insert into registration_email_otp_challenges(id,email,otp_hash,expires_at) values(?,?,?,?)",
            UUID.randomUUID(),email,encoder.encode(otp),Timestamp.from(Instant.now().plusSeconds(300)));
        JavaMailSenderImpl mail=new JavaMailSenderImpl();
        mail.setHost(smtpHost); mail.setPort(smtpPort);
        mail.setUsername(smtpUser); mail.setPassword(smtpPassword);
        mail.getJavaMailProperties().put("mail.smtp.auth",String.valueOf(smtpAuth));
        mail.getJavaMailProperties().put("mail.smtp.starttls.enable",String.valueOf(smtpTls));
        mail.getJavaMailProperties().put("mail.smtp.connectiontimeout","5000");
        mail.getJavaMailProperties().put("mail.smtp.timeout","5000");
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(smtpFrom.isBlank()?smtpUser:smtpFrom);
        message.setTo(email);
        message.setSubject("Jnapanam Sports Zone - Email verification");
        message.setText("Your Jnapanam registration verification code is "+otp+
            ". It expires in 5 minutes. Never share this code.");
        mail.send(message);
    }

    @Transactional(noRollbackFor=BadRequestException.class)
    public void verify(String rawEmail,String otp) {
        String email=normalize(rawEmail);
        if(email==null||otp==null||!otp.matches("[0-9]{6}"))
            throw new BadRequestException("Invalid verification code");
        List<Map<String,Object>> rows=jdbc.queryForList(
            "select * from registration_email_otp_challenges where email=? and verified_at is null order by created_at desc limit 1 for update", email);
        if(rows.isEmpty()) throw new BadRequestException("Email OTP not requested");
        Map<String,Object> row=rows.get(0);
        if(Instant.now().isAfter(((Timestamp)row.get("expires_at")).toInstant()))
            throw new BadRequestException("Email OTP expired");
        if(((Number)row.get("attempts")).intValue()>=5) throw new BadRequestException("Too many OTP attempts");
        if(!encoder.matches(otp,(String)row.get("otp_hash"))) {
            jdbc.update("update registration_email_otp_challenges set attempts=attempts+1 where id=?",row.get("id"));
            throw new BadRequestException("Invalid verification code");
        }
        jdbc.update("update registration_email_otp_challenges set verified_at=now() where id=?",row.get("id"));
    }

    private static String normalize(String value) {
        if(value==null) return null;
        String email=value.trim().toLowerCase(Locale.ROOT);
        return email.length()<=180&&email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")?email:null;
    }
}
