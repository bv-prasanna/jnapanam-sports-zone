package com.jnapanam.sportszone.notification.provider;
import com.jnapanam.sportszone.notification.model.*;import java.util.UUID;import org.springframework.stereotype.Component;
@Component public class InAppNotificationProvider implements NotificationProvider{public NotificationChannel channel(){return NotificationChannel.IN_APP;}public String name(){return "in-app";}public String send(Notification n){return "inapp-"+UUID.randomUUID();}}
