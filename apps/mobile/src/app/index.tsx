import { MaterialCommunityIcons } from '@expo/vector-icons';
import { StatusBar } from 'expo-status-bar';
import { useTranslation } from 'react-i18next';
import {
  Image,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';

import { BrandColors } from '@/constants/brand';
import { styles } from '../features/home/home.styles';

const sports = [
  {
    nameKey: 'home.swimming',
    subtitleKey: 'home.swimmingDescription',
    icon: 'swim',
    color: '#0877B9',
  },
  {
    nameKey: 'home.cricket',
    subtitleKey: 'home.cricketDescription',
    icon: 'cricket',
    color: '#F47A20',
  },
  {
    nameKey: 'home.badminton',
    subtitleKey: 'home.badmintonDescription',
    icon: 'badminton',
    color: '#8E44AD',
  },
  {
    nameKey: 'home.football',
    subtitleKey: 'home.footballDescription',
    icon: 'soccer',
    color: '#42A846',
  },
] as const;

const services = [
  {
    titleKey: 'home.memberships',
    descriptionKey: 'home.membershipsDescription',
    icon: 'card-account-details-outline',
  },
  {
    titleKey: 'home.trainingCamps',
    descriptionKey: 'home.trainingCampsDescription',
    icon: 'whistle-outline',
  },
  {
    titleKey: 'home.tournaments',
    descriptionKey: 'home.tournamentsDescription',
    icon: 'trophy-outline',
  },
] as const;

export default function HomeScreen() {
  const { t } = useTranslation();

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <View style={styles.headerTop}>
            <View style={styles.logoContainer}>
              <Image
                source={require('@/assets/images/jnapanam-logo.png')}
                style={styles.brandLogo}
                resizeMode="contain"
                accessibilityLabel={t('common.appName')}
              />
            </View>

            <Pressable
              style={styles.profileButton}
              accessibilityLabel={t('navigation.profile')}
            >
              <MaterialCommunityIcons
                name="account-outline"
                size={25}
                color={BrandColors.white}
              />
            </Pressable>
          </View>

          <Text style={styles.locationLabel}>
            {t('home.destination')}
          </Text>

          <Text style={styles.location}>
            {t('common.location')}
          </Text>

          <View style={styles.heroContent}>
            <Text style={styles.heroTitle}>
              {t('home.headline')}
            </Text>

            <Text style={styles.heroDescription}>
              {t('home.description')}
            </Text>

            <Pressable style={styles.primaryButton}>
              <MaterialCommunityIcons
                name="calendar-clock"
                size={20}
                color={BrandColors.primary}
              />

              <Text style={styles.primaryButtonText}>
                {t('home.bookSlot')}
              </Text>
            </Pressable>
          </View>
        </View>

        <View style={styles.body}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>
              {t('home.chooseSport')}
            </Text>

            <Pressable>
              <Text style={styles.linkText}>
                {t('common.viewAll')}
              </Text>
            </Pressable>
          </View>

          <View style={styles.sportsGrid}>
            {sports.map((sport) => (
              <Pressable key={sport.nameKey} style={styles.sportCard}>
                <View
                  style={[
                    styles.sportIcon,
                    { backgroundColor: `${sport.color}18` },
                  ]}
                >
                  <MaterialCommunityIcons
                    name={sport.icon}
                    size={31}
                    color={sport.color}
                  />
                </View>

                <Text style={styles.sportName}>
                  {t(sport.nameKey)}
                </Text>

                <Text style={styles.sportSubtitle}>
                  {t(sport.subtitleKey)}
                </Text>
              </Pressable>
            ))}
          </View>

          <Text style={[styles.sectionTitle, styles.servicesTitle]}>
            {t('home.exploreMore')}
          </Text>

          <View style={styles.servicesList}>
            {services.map((service) => (
              <Pressable
                key={service.titleKey}
                style={styles.serviceCard}
              >
                <View style={styles.serviceIcon}>
                  <MaterialCommunityIcons
                    name={service.icon}
                    size={26}
                    color={BrandColors.primary}
                  />
                </View>

                <View style={styles.serviceContent}>
                  <Text style={styles.serviceTitle}>
                    {t(service.titleKey)}
                  </Text>

                  <Text style={styles.serviceDescription}>
                    {t(service.descriptionKey)}
                  </Text>
                </View>

                <MaterialCommunityIcons
                  name="chevron-right"
                  size={25}
                  color={BrandColors.textSecondary}
                />
              </Pressable>
            ))}
          </View>

          <View style={styles.upcomingCard}>
            <View style={styles.upcomingIcon}>
              <MaterialCommunityIcons
                name="trophy"
                size={27}
                color={BrandColors.accent}
              />
            </View>

            <View style={styles.upcomingContent}>
              <Text style={styles.upcomingLabel}>
                {t('home.upcomingEvent')}
              </Text>

              <Text style={styles.upcomingTitle}>
                {t('home.championship')}
              </Text>

              <Text style={styles.upcomingDescription}>
                {t('home.registrationsOpeningSoon')}
              </Text>
            </View>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}