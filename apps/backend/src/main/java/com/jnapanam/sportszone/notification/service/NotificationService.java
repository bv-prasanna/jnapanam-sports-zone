package com.jnapanam.sportszone.notification.service;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.dto.BroadcastRequest;
import com.jnapanam.sportszone.notification.dto.NotificationResponse;
import com.jnapanam.sportszone.notification.dto.PreferenceRequest;
import com.jnapanam.sportszone.notification.dto.PreferenceResponse;
import com.jnapanam.sportszone.notification.dto.TemplateRequest;
import com.jnapanam.sportszone.notification.dto.TemplateResponse;
import com.jnapanam.sportszone.notification.model.Notification;
import com.jnapanam.sportszone.notification.model.NotificationChannel;
import com.jnapanam.sportszone.notification.model.NotificationPreference;
import com.jnapanam.sportszone.notification.model.NotificationStatus;
import com.jnapanam.sportszone.notification.model.NotificationTemplate;
import com.jnapanam.sportszone.notification.repository.NotificationPreferenceRepository;
import com.jnapanam.sportszone.notification.repository.NotificationRepository;
import com.jnapanam.sportszone.notification.repository.NotificationTemplateRepository;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final NotificationTemplateRepository templates;
    private final UserRepository users; private final NotificationProviderRegistry providers; private final JdbcTemplate jdbc;

    public NotificationService(NotificationRepository notifications, NotificationPreferenceRepository preferences,
            NotificationTemplateRepository templates, UserRepository users, NotificationProviderRegistry providers, JdbcTemplate jdbc) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.templates = templates;
        this.users = users; this.providers = providers; this.jdbc=jdbc;
    }

    @Transactional
    public void payment(UUID userId, String receipt, long paise) {
        notify(userId, "PAYMENT", "Payment successful", "Payment " + receipt + " of ₹" + money(paise) + " was successful.");
    }

    @Transactional
    public void refund(UUID userId, String receipt, long paise) {
        notify(userId, "REFUND", "Refund processed", "Refund of ₹" + money(paise) + " for " + receipt + " was processed.");
    }

    @Transactional
    public NotificationResponse notifyChannel(UUID userId, NotificationChannel channel, String category, String subject, String message) { User user=user(userId); return NotificationResponse.from(notifications.save(new Notification(user,channel,category,subject,message))); }

    @Transactional
    public NotificationResponse notify(UUID userId, String category, String subject, String message) {
        User user = user(userId);
        return NotificationResponse.from(notifications.save(new Notification(user, NotificationChannel.IN_APP, category, subject, message)));
    }

    @Transactional
    public void lifecycle(UUID userId, String category, String subject, String message) {
        User user = user(userId);
        notifications.save(new Notification(user, NotificationChannel.IN_APP, category, subject, message));
        NotificationPreference preference = preferences.findById(userId).orElseGet(() -> preferences.save(new NotificationPreference(user)));
        if (preference.isEmailEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) notifications.save(new Notification(user, NotificationChannel.EMAIL, category, subject, message));
        if (preference.isSmsEnabled() && user.getMobile() != null && !user.getMobile().isBlank()) notifications.save(new Notification(user, NotificationChannel.SMS, category, subject, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(UUID userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse read(UUID userId, UUID id) {
        Notification notification = notifications.findById(id).orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) throw new NotFoundException("Notification not found");
        notification.read();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public PreferenceResponse preference(UUID userId) {
        NotificationPreference preference = preferences.findById(userId)
                .orElseGet(() -> preferences.save(new NotificationPreference(user(userId))));
        return pref(preference);
    }

    @Transactional
    public PreferenceResponse preference(UUID userId, PreferenceRequest request) {
        NotificationPreference preference = preferences.findById(userId)
                .orElseGet(() -> new NotificationPreference(user(userId)));
        preference.update(request.emailEnabled(), request.smsEnabled(), request.pushEnabled());
        return pref(preferences.save(preference));
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        if (templates.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Template code already exists");
        NotificationTemplate template = new NotificationTemplate(request.code(), request.channels(), request.subject(), request.body());
        if (!request.active()) template.update(request.channels(), request.subject(), request.body(), false);
        return TemplateResponse.from(templates.save(template));
    }

    @Transactional
    public TemplateResponse update(UUID id, TemplateRequest request) {
        NotificationTemplate template = templates.findById(id).orElseThrow(() -> new NotFoundException("Template not found"));
        if (!template.getCode().equalsIgnoreCase(request.code())) throw new BadRequestException("Template code cannot be changed");
        template.update(request.channels(), request.subject(), request.body(), request.active());
        return TemplateResponse.from(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> templates() {
        return templates.findAllByOrderByCodeAsc().stream().map(TemplateResponse::from).toList();
    }

    @Transactional
    public int broadcast(BroadcastRequest request) {
        int count = 0;
        for (User user : users.findAll()) {
            if (user.isActive()) {
                notifications.save(new Notification(user, request.channel(), request.category(), request.subject(), request.message()));
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int dispatch() {
        List<Notification> batch = notifications.findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.QUEUED);
        for (Notification notification : batch) { try { var provider=providers.provider(notification.getChannel());String ref=provider.send(notification);notification.sent(ref);jdbc.update("insert into notification_delivery_attempts(id,notification_id,channel,provider,destination,status,provider_reference) values(?,?,?,?,?,?,?)",UUID.randomUUID(),notification.getId(),notification.getChannel().name(),provider.name(),destination(notification),"SENT",ref); } catch (Exception e) { notification.failed(e.getMessage());jdbc.update("insert into notification_delivery_attempts(id,notification_id,channel,status,error_message) values(?,?,?,?,?)",UUID.randomUUID(),notification.getId(),notification.getChannel().name(),"FAILED",e.getMessage()); } }
        return batch.size();
    }

    private String destination(Notification n){return switch(n.getChannel()){case EMAIL->n.getUser().getEmail();case SMS,WHATSAPP->n.getUser().getMobile();default->n.getUser().getId().toString();};}

    private User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private static String money(long paise) {
        return java.math.BigDecimal.valueOf(paise, 2).toPlainString();
    }

    private static PreferenceResponse pref(NotificationPreference preference) {
        return new PreferenceResponse(preference.getUserId(), preference.isEmailEnabled(),
                preference.isSmsEnabled(), preference.isPushEnabled());
    }
}
