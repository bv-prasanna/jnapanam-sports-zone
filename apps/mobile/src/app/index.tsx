import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Image, Pressable, ScrollView, Text, View } from 'react-native';

import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { styles } from '@/features/home/home.styles';
import { api } from '@/lib/api';

type Sport = { code: string; name: string; description?: string };

const sportIcons: Record<string, keyof typeof MaterialCommunityIcons.glyphMap> = {
  SWIMMING: 'swim',
  CRICKET: 'cricket',
  BADMINTON: 'badminton',
  FOOTBALL: 'soccer',
  GYM: 'dumbbell',
  AEROBICS: 'human-handsup',
  CARROM: 'checkerboard',
  CHESS: 'chess-king',
  TABLE_TENNIS: 'table-tennis',
};

const services = [
  { titleKey: 'home.memberships', descriptionKey: 'home.membershipsDescription', icon: 'card-account-details-outline', route: '/profile' },
  { titleKey: 'home.trainingCamps', descriptionKey: 'home.trainingCampsDescription', icon: 'whistle-outline', route: '/training' },
  { titleKey: 'home.tournaments', descriptionKey: 'home.tournamentsDescription', icon: 'trophy-outline', route: '/tournaments' },
] as const;

export default function HomeScreen() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [sports, setSports] = useState<Sport[]>([]);
  const [loadingSports, setLoadingSports] = useState(true);

  useEffect(() => {
    api<Sport[]>('/api/v1/public/sports')
      .then(setSports)
      .catch(() => setSports([]))
      .finally(() => setLoadingSports(false));
  }, []);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.topBar}>
          <Image
            source={require('@/assets/images/jnapanam-logo-current.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel={t('common.appName')}
          />
          <LanguageSwitch />
        </View>

        <View style={styles.hero}>
          <Text style={styles.eyebrow}>{t('home.destination')}</Text>
          <Text style={styles.greeting}>
            {user ? t('home.welcome', { name: user.fullName.split(' ')[0] }) : t('home.welcomeGuest')}
          </Text>
          <Text style={styles.heroTitle}>{t('home.headline')}</Text>
          <Text style={styles.heroDescription}>{t('home.description')}</Text>
          <Pressable style={styles.primaryButton} onPress={() => router.push('/bookings')}>
            <MaterialCommunityIcons name="calendar-clock" size={20} color={BrandColors.surface} />
            <Text style={styles.primaryButtonText}>{t('home.bookSlot')}</Text>
          </Pressable>
        </View>

        <View style={styles.body}>
          <View style={styles.sectionHeader}>
            <View>
              <Text style={styles.sectionEyebrow}>{t('home.atJnapanam')}</Text>
              <Text style={styles.sectionTitle}>{t('home.chooseSport')}</Text>
            </View>
            <Pressable onPress={() => router.push('/bookings')}>
              <Text style={styles.linkText}>{t('common.viewAll')}</Text>
            </Pressable>
          </View>

          {loadingSports ? (
            <ActivityIndicator color={BrandColors.accent} style={styles.loader} />
          ) : sports.length ? (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.sportsScroller}>
              {sports.map((sport) => (
                <Pressable key={sport.code} style={styles.sportCard} onPress={() => router.push('/bookings')}>
                  <View style={styles.sportIcon}>
                    <MaterialCommunityIcons
                      name={sportIcons[sport.code] || 'medal-outline'}
                      size={28}
                      color={BrandColors.accent}
                    />
                  </View>
                  <Text style={styles.sportName}>{sport.name}</Text>
                  {!!sport.description && <Text style={styles.sportSubtitle} numberOfLines={2}>{sport.description}</Text>}
                  <View style={styles.cardLink}>
                    <Text style={styles.cardLinkText}>{t('home.bookNow')}</Text>
                    <MaterialCommunityIcons name="arrow-right" size={16} color={BrandColors.accent} />
                  </View>
                </Pressable>
              ))}
            </ScrollView>
          ) : (
            <Text style={styles.mutedText}>{t('home.sportsUnavailable')}</Text>
          )}

          <Text style={[styles.sectionTitle, styles.servicesTitle]}>{t('home.exploreMore')}</Text>
          <View style={styles.servicesList}>
            {services.map((service) => (
              <Pressable key={service.titleKey} style={styles.serviceCard} onPress={() => router.push(service.route)}>
                <View style={styles.serviceIcon}>
                  <MaterialCommunityIcons name={service.icon} size={25} color={BrandColors.primary} />
                </View>
                <View style={styles.serviceContent}>
                  <Text style={styles.serviceTitle}>{t(service.titleKey)}</Text>
                  <Text style={styles.serviceDescription}>{t(service.descriptionKey)}</Text>
                </View>
                <MaterialCommunityIcons name="chevron-right" size={24} color={BrandColors.accent} />
              </Pressable>
            ))}
          </View>

          <Pressable style={styles.profileCard} onPress={() => router.push('/profile')}>
            <MaterialCommunityIcons name={user ? 'account-circle-outline' : 'login'} size={29} color={BrandColors.surface} />
            <View style={styles.profileCopy}>
              <Text style={styles.profileTitle}>{user ? t('navigation.profile') : t('auth.login')}</Text>
              <Text style={styles.profileDescription}>
                {user ? user.email : t('auth.loginDescription')}
              </Text>
            </View>
            <MaterialCommunityIcons name="arrow-right" size={21} color={BrandColors.surface} />
          </Pressable>
        </View>
      </ScrollView>
    </View>
  );
}
