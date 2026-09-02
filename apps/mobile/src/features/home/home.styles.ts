import { StyleSheet } from 'react-native';

import { BrandColors } from '@/constants/brand';

export const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: BrandColors.background,
  },

  content: {
    flexGrow: 1,
  },

  header: {
    backgroundColor: BrandColors.primary,
    paddingHorizontal: 24,
    paddingTop: 54,
    paddingBottom: 34,
    borderBottomLeftRadius: 30,
    borderBottomRightRadius: 30,
  },

  headerTop: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 16,
  },

  logoContainer: {
    width: 250,
    height: 105,
    maxWidth: '78%',
    borderRadius: 16,
    backgroundColor: BrandColors.white,
    paddingHorizontal: 10,
    paddingVertical: 6,
    overflow: 'hidden',
  },

  brandLogo: {
    width: '100%',
    height: '100%',
  },

  locationLabel: {
    color: '#B8D8EA',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.2,
    marginTop: 18,
  },

  location: {
    color: '#D7EAF4',
    fontSize: 14,
    marginTop: 3,
  },

  profileButton: {
    width: 45,
    height: 45,
    borderRadius: 23,
    backgroundColor: 'rgba(255,255,255,0.14)',
    alignItems: 'center',
    justifyContent: 'center',
  },

  heroContent: {
    marginTop: 28,
  },

  heroTitle: {
    color: BrandColors.white,
    fontSize: 34,
    fontWeight: '900',
  },

  heroDescription: {
    color: '#D7EAF4',
    fontSize: 15,
    lineHeight: 22,
    maxWidth: 510,
    marginTop: 9,
  },

  primaryButton: {
    backgroundColor: BrandColors.white,
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
    paddingHorizontal: 19,
    paddingVertical: 13,
    borderRadius: 14,
    marginTop: 22,
  },

  primaryButtonText: {
    color: BrandColors.primary,
    fontSize: 15,
    fontWeight: '800',
  },

  body: {
    width: '100%',
    maxWidth: 900,
    alignSelf: 'center',
    paddingHorizontal: 20,
    paddingTop: 27,
    paddingBottom: 50,
  },

  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },

  sectionTitle: {
    color: BrandColors.textPrimary,
    fontSize: 21,
    fontWeight: '800',
  },

  linkText: {
    color: BrandColors.secondary,
    fontSize: 14,
    fontWeight: '700',
  },

  sportsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 13,
    marginTop: 16,
  },

  sportCard: {
    backgroundColor: BrandColors.surface,
    width: '48%',
    minWidth: 155,
    flexGrow: 1,
    padding: 17,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: BrandColors.border,
  },

  sportIcon: {
    width: 52,
    height: 52,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },

  sportName: {
    color: BrandColors.textPrimary,
    fontSize: 17,
    fontWeight: '800',
    marginTop: 14,
  },

  sportSubtitle: {
    color: BrandColors.textSecondary,
    fontSize: 13,
    lineHeight: 18,
    marginTop: 4,
  },

  servicesTitle: {
    marginTop: 31,
  },

  servicesList: {
    gap: 11,
    marginTop: 15,
  },

  serviceCard: {
    backgroundColor: BrandColors.surface,
    flexDirection: 'row',
    alignItems: 'center',
    padding: 15,
    borderRadius: 17,
    borderWidth: 1,
    borderColor: BrandColors.border,
  },

  serviceIcon: {
    width: 49,
    height: 49,
    borderRadius: 15,
    backgroundColor: '#E8F2F8',
    alignItems: 'center',
    justifyContent: 'center',
  },

  serviceContent: {
    flex: 1,
    paddingHorizontal: 13,
  },

  serviceTitle: {
    color: BrandColors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
  },

  serviceDescription: {
    color: BrandColors.textSecondary,
    fontSize: 13,
    marginTop: 3,
  },

  upcomingCard: {
    backgroundColor: BrandColors.warningBackground,
    flexDirection: 'row',
    alignItems: 'center',
    padding: 17,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: BrandColors.warningBorder,
    marginTop: 28,
  },

  upcomingIcon: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: BrandColors.white,
    alignItems: 'center',
    justifyContent: 'center',
  },

  upcomingContent: {
    flex: 1,
    marginLeft: 14,
  },

  upcomingLabel: {
    color: BrandColors.warningText,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },

  upcomingTitle: {
    color: BrandColors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
    marginTop: 4,
  },

  upcomingDescription: {
    color: BrandColors.textSecondary,
    fontSize: 13,
    marginTop: 3,
  },
});