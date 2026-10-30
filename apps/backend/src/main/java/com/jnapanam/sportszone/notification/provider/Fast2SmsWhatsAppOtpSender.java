package com.jnapanam.sportszone.notification.provider;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Server-side delivery only; authentication and challenge verification stay in SmsOtpService. */
@Component
public class Fast2SmsWhatsAppOtpSender {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern SAFE_VERSION = Pattern.compile("v[0-9]+\\.[0-9]+");
    private final RestClient client = RestClient.create();
    private final String key, phoneId, template, language, version;

    public Fast2SmsWhatsAppOtpSender(
            @Value("${app.notification.whatsapp.fast2sms.api-key:}") String key,
            @Value("${app.notification.whatsapp.fast2sms.phone-number-id:}") String phoneId,
            @Value("${app.notification.whatsapp.fast2sms.template-name:}") String template,
            @Value("${app.notification.whatsapp.fast2sms.template-language:en_US}") String language,
            @Value("${app.notification.whatsapp.fast2sms.api-version:v26.0}") String version) {
        this.key=key; this.phoneId=phoneId; this.template=template;
        this.language=language; this.version=version;
    }

    public void send(String mobile, String otp) {
        if (key.isBlank() || phoneId.isBlank() || template.isBlank())
            throw new IllegalStateException("WhatsApp OTP sender/template/API key is not configured");
        if (!phoneId.matches("[0-9]+") || !SAFE_ID.matcher(template).matches()
                || !SAFE_ID.matcher(language).matches() || !SAFE_VERSION.matcher(version).matches())
            throw new IllegalStateException("Invalid WhatsApp OTP template or sender configuration");
        if (!mobile.matches("[0-9]{10}") || !otp.matches("[0-9]{6}"))
            throw new IllegalArgumentException("Invalid mobile or OTP");
        Map<String,Object> parameter = Map.of("type","text","text",otp);
        Map<String,Object> body = Map.of(
          "messaging_product","whatsapp", "recipient_type","individual",
          "to","91"+mobile, "type","template",
          "template",Map.of(
            "name",template, "language",Map.of("code",language),
            "components",List.of(
              Map.of("type","body","parameters",List.of(parameter)),
              Map.of("type","button","sub_type","url","index","0","parameters",List.of(parameter)))));
        client.post()
          .uri("https://www.fast2sms.com/dev/whatsapp/"+version+"/"+phoneId+"/messages")
          .header("Authorization",key).contentType(MediaType.APPLICATION_JSON)
          .body(body).retrieve().toBodilessEntity();
    }
}
