package com.jnapanam.sportszone.notification.provider;
import static org.junit.jupiter.api.Assertions.*;import com.jnapanam.sportszone.notification.model.NotificationChannel;import java.util.*;import org.junit.jupiter.api.Test;
class NotificationProviderRegistryTest{
static class P implements NotificationProvider{private final NotificationChannel c;private final String n;P(NotificationChannel c,String n){this.c=c;this.n=n;}public NotificationChannel channel(){return c;}public String name(){return n;}public String send(com.jnapanam.sportszone.notification.model.Notification x){return n;}}
@Test void selectsConfiguredProviders(){var r=new NotificationProviderRegistry(List.of(new P(NotificationChannel.EMAIL,"smtp"),new P(NotificationChannel.EMAIL,"ses"),new P(NotificationChannel.SMS,"fast2sms"),new P(NotificationChannel.SMS,"other")),"ses","other");assertEquals("ses",r.provider(NotificationChannel.EMAIL).name());assertEquals("other",r.provider(NotificationChannel.SMS).name());}}

