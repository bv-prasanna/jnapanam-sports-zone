import { useTranslation } from 'react-i18next';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
export default function TournamentsScreen(){const {t}=useTranslation();return <ScreenShell title={t('navigation.tournaments')}><EmptyState icon="trophy-outline" title={t('tournaments.emptyTitle')} description={t('tournaments.emptyDescription')}/></ScreenShell>;}
