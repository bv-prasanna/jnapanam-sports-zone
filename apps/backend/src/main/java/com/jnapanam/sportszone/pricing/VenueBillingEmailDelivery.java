package com.jnapanam.sportszone.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.notification.service.NotificationProviderSettingsService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Retryable notification outbox. Only PDF documents previously issued are deliverable. */
@Service
public class VenueBillingEmailDelivery {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final VenueInvoicePdfService pdf;
    private final NotificationProviderSettingsService settings;
    private final String smtpHost,user,password,from;
    private final int port;
    public VenueBillingEmailDelivery(JdbcTemplate jdbc,ObjectMapper json,VenueInvoicePdfService pdf,
        NotificationProviderSettingsService settings,
        @Value("${spring.mail.host:localhost}")String host,
        @Value("${spring.mail.port:587}")int port,
        @Value("${spring.mail.username:}")String user,
        @Value("${spring.mail.password:}")String password,
        @Value("${app.notification.email.from:}")String from){
        this.jdbc=jdbc;this.json=json;this.pdf=pdf;this.settings=settings;
        smtpHost=host;this.port=port;this.user=user;this.password=password;this.from=from;
    }
    @Scheduled(fixedDelayString="${app.billing.invoice-mail-period-ms:300000}",initialDelay=60000)
    public void dispatch() {
        // Opt-in only. Leave any queued messages untouched when delivery is disabled.
        Boolean enabled=jdbc.queryForObject(
            "select exists(select 1 from venue_billing_profile where id=1 and invoice_email_enabled=true)",
            Boolean.class);
        if(!Boolean.TRUE.equals(enabled))return;
        List<Map<String,Object>> jobs=jdbc.queryForList("""
          select d.id,d.recipient,b.kind,b.document_number,b.document_data::text as details
          from venue_billing_email_deliveries d join venue_billing_documents b on b.id=d.document_id
          where d.status in ('QUEUED','FAILED') and d.attempts < 5 and b.state='ISSUED'
          and (d.attempted_at is null or d.attempted_at < now() - interval '5 minutes')
          order by d.queued_at limit 10
          """);
        for(Map<String,Object> job:jobs){
            UUID id=(UUID)job.get("id");
            if(jdbc.update("""
                update venue_billing_email_deliveries set status='FAILED',attempts=attempts+1,
                attempted_at=now() where id=? and status in ('QUEUED','FAILED') and attempts<5
                """,id)==0)continue;
            try{
                Map<String,Object> details=json.readValue(String.valueOf(job.get("details")),
                    new TypeReference<Map<String,Object>>(){});
                String number=String.valueOf(job.get("document_number"));
                byte[] bytes=pdf.render(number,String.valueOf(job.get("kind")),details);
                send(String.valueOf(job.get("recipient")),number,bytes);
                jdbc.update("""
                    update venue_billing_email_deliveries set status='SENT',last_error=null,sent_at=now()
                    where id=?
                    """,id);
            }catch(Exception ex){
                String reason=ex.getClass().getSimpleName();
                jdbc.update("update venue_billing_email_deliveries set last_error=? where id=?",reason,id);
            }
        }
    }
    private void send(String to,String number,byte[] pdfBytes)throws Exception{
        String host=settings.value("SMTP_HOST",smtpHost);
        String sender=settings.value("SMTP_FROM",from);
        if(host==null||host.isBlank()||sender==null||sender.isBlank())
            throw new IllegalStateException("SMTP not configured");
        JavaMailSenderImpl mail=new JavaMailSenderImpl();
        mail.setHost(host);
        mail.setPort(Integer.parseInt(settings.value("SMTP_PORT",String.valueOf(port))));
        mail.setUsername(settings.value("SMTP_USERNAME",user));
        mail.setPassword(settings.value("SMTP_PASSWORD",password));
        mail.getJavaMailProperties().put("mail.smtp.auth","true");
        mail.getJavaMailProperties().put("mail.smtp.starttls.enable","true");
        var msg=mail.createMimeMessage();
        var helper=new MimeMessageHelper(msg,true,"UTF-8");
        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject("Jnapanam Sports Zone - Tax document "+number);
        helper.setText("Your issued Jnapanam billing document "+number+" is attached as a PDF. You can also sign in to your Jnapanam account to view the document.");
        helper.addAttachment("jnapanam-"+number+".pdf",new ByteArrayResource(pdfBytes),"application/pdf");
        mail.send(msg);
    }
}
