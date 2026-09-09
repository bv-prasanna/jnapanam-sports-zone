package com.jnapanam.sportszone.notification.service;

import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.dto.*;
import com.jnapanam.sportszone.notification.model.*;
import com.jnapanam.sportszone.notification.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final NotificationTemplateRepository templates;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, NotificationPreferenceRepository preferences,
            NotificationTemplateRepository templates, UserRepository users) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.templates = templates;
        this.users = users;
    }

    @Transactional public void welcome(UUID userId, String name) {
        notify(userId, "ACCOUNT", "Welcome to Jnapanam Sports Zone",
                "Welcome " + name + ". Your customer account was created successfully.");
    }
    @Transactional public void passwordResetRequested(UUID userId) {
        notify(userId, "SECURITY", "Password reset requested",
                "A password reset was requested for your account. The reset link expires in 15 minutes.");
    }
    @Transactional public void passwordChanged(UUID userId) {
        notify(userId, "SECURITY", "Password changed",
                "Your password was changed successfully. Please contact support if this was not you.");
    }
    @Transactional public void booking(UUID userId, String bookingNumber, String state) {
        notify(userId, "BOOKING", "Booking " + readable(state),
                "Booking " + bookingNumber + " is " + readable(state).toLowerCase(Locale.ROOT) + ".");
    }
    @Transactional public void membership(UUID userId, String plan, String state) {
        notify(userId, "MEMBERSHIP", "Membership " + readable(state),
                plan + " membership is " + readable(state).toLowerCase(Locale.ROOT) + ".");
    }
    @Transactional public void training(UUID userId, String batch, String state) {
        notify(userId, "TRAINING", "Training " + readable(state),
                batch + " enrolment is " + readable(state).toLowerCase(Locale.ROOT) + ".");
    }
    @Transactional public void tournament(UUID userId, String tournament, String state) {
        notify(userId, "TOURNAMENT", "Tournament registration " + readable(state),
                tournament + " team registration is " + readable(state).toLowerCase(Locale.ROOT) + ".");
    }
    @Transactional public void commerce(UUID userId, String orderNumber, String state) {
        notify(userId, "COMMERCE", "Shop order " + readable(state),
                "Order " + orderNumber + " is " + readable(state).toLowerCase(Locale.ROOT) + ".");
    }
    @Transactional public void payment(UUID userId, String receipt, long paise) {
        notify(userId, "PAYMENT", "Payment successful",
                "Payment " + receipt + " of ₹" + money(paise) + " was successful.");
    }
    @Transactional public void refund(UUID userId, String receipt, long paise) {
        notify(userId, "REFUND", "Refund processed",
                "Refund of ₹" + money(paise) + " for " + receipt + " was processed.");
    }

    @Transactional
    public NotificationResponse notify(UUID userId, String category, String subject, String message) {
        return NotificationResponse.from(notifications.save(
                new Notification(user(userId), NotificationChannel.IN_APP, category, subject, message)));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(UUID userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse read(UUID userId, UUID id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) throw new NotFoundException("Notification not found");
        notification.read();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public PreferenceResponse preference(UUID userId) {
        NotificationPreference preference = preferences.findById(userId)
                .orElseGet(() -> preferences.save(new NotificationPreference(user(userId))));
        return preference(preference);
    }

    @Transactional
    public PreferenceResponse preference(UUID userId, PreferenceRequest request) {
        NotificationPreference preference = preferences.findById(userId)
                .orElseGet(() -> new NotificationPreference(user(userId)));
        preference.update(request.emailEnabled(), request.smsEnabled(), request.pushEnabled());
        return preference(preferences.save(preference));
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        if (templates.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Template code already exists");
        NotificationTemplate template = new NotificationTemplate(
                request.code(), request.channel(), request.subject(), request.body());
        if (!request.active()) template.update(request.channel(), request.subject(), request.body(), false);
        return TemplateResponse.from(templates.save(template));
    }

    @Transactional
    public TemplateResponse update(UUID id, TemplateRequest request) {
        NotificationTemplate template = templates.findById(id)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        if (!template.getCode().equalsIgnoreCase(request.code()))
            throw new IllegalArgumentException("Template code cannot be changed");
        template.update(request.channel(), request.subject(), request.body(), request.active());
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
                notifications.save(new Notification(user, request.channel(), request.category(),
                        request.subject(), request.message()));
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int dispatch() {
        List<Notification> batch = notifications.findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.QUEUED);
        for (Notification notification : batch) notification.sent("mock-" + UUID.randomUUID());
        return batch.size();
    }

    private User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new UnauthorizedException("User not found"));
    }
    private static String money(long paise) {
        return java.math.BigDecimal.valueOf(paise, 2).toPlainString();
    }
    private static String readable(String value) {
        String text = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
    private static PreferenceResponse preference(NotificationPreference preference) {
        return new PreferenceResponse(preference.getUserId(), preference.isEmailEnabled(),
                preference.isSmsEnabled(), preference.isPushEnabled());
    }
}
