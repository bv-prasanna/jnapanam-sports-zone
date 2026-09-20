package com.jnapanam.sportszone.notification.provider;
import com.jnapanam.sportszone.notification.model.Notification;import com.jnapanam.sportszone.notification.model.NotificationChannel;
public interface NotificationProvider{NotificationChannel channel();String name();String send(Notification notification);}
