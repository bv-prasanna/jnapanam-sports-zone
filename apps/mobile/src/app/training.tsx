import { useTranslation } from 'react-i18next';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
export default function TrainingScreen(){const {t}=useTranslation();return <ScreenShell title={t('navigation.training')}><EmptyState icon="whistle-outline" title={t('training.emptyTitle')} description={t('training.emptyDescription')}/></ScreenShell>;}
