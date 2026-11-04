import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Href, router, usePathname } from 'expo-router';
import { useEffect,useState } from 'react';
import {api} from '@/lib/api';
import { useTranslation } from 'react-i18next';
import { Image, Modal, Pressable, ScrollView, StatusBar, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

type MenuItem = { href: Href; key: string; icon: keyof typeof MaterialCommunityIcons.glyphMap };
const customerItems: MenuItem[] = [
  {href:'/',key:'home',icon:'home-outline'},
  {href:'/bookings',key:'bookings',icon:'calendar-check-outline'},
  {href:'/memberships',key:'memberships',icon:'card-account-details-outline'},
  {href:'/training',key:'training',icon:'whistle-outline'},
  {href:'/growth',key:'growth',icon:'chart-line'},
  {href:'/tournaments',key:'tournaments',icon:'trophy-outline'},
  {href:'/shop',key:'shop',icon:'shopping-outline'},
  {href:'/notifications',key:'messages',icon:'bell-outline'},
  {href:'/profile',key:'profile',icon:'account-outline'},
  {href:'/billing',key:'billing',icon:'receipt-text-outline'},
  {href:'/subscriptions',key:'subscriptions',icon:'repeat'},
];
const operatorItem: MenuItem={href:'/operations',key:'operations',icon:'clipboard-account-outline'};
const shopkeeperItem: MenuItem={href:'/shop-counter',key:'shopCounter',icon:'storefront-outline'};

export function MobileHeader() {
  const {t}=useTranslation();
  const {user}=useAuth();
  const pathname=usePathname();
  const [open,setOpen]=useState(false);
  const [featureSettings,setFeatureSettings]=useState<{code:string;enabled:boolean;allowedRoles:string[]}[]|null>(null);
  useEffect(()=>{let active=true;void api<{code:string;enabled:boolean;allowedRoles:string[]}[]>('/api/v1/public/features')
    .then(values=>{if(active)setFeatureSettings(values)}).catch(()=>{});
    return()=>{active=false};
  },[]);
  const featureAllowed=(key:string)=>{
    const code=({bookings:'BOOKINGS',memberships:'MEMBERSHIPS',training:'TRAINING',growth:'GROWTH',
      tournaments:'TOURNAMENTS',shop:'SHOP',subscriptions:'SUBSCRIPTIONS'} as Record<string,string>)[key];
    if(!code)return true;
    if(!featureSettings)return code!=='TRAINING';
    const feature=featureSettings.find(f=>f.code===code);
    return !feature||(feature.enabled&&(!user?.roles.length?feature.allowedRoles.includes('CUSTOMER'):user.roles.some(r=>feature.allowedRoles.includes(r))));
  };
  const canOperate=!!user?.roles.some(r=>['OPERATIONS','ADMIN','SUPER_ADMIN'].includes(r));
  const canShop=!!user?.roles.some(r=>['SHOP_KEEPER','ADMIN','SUPER_ADMIN'].includes(r));
  const links=[...customerItems.filter(item=>featureAllowed(item.key)),...(canOperate?[operatorItem]:[]),...(canShop?[shopkeeperItem]:[])];
  function navigate(href:Href) {setOpen(false);router.push(href);}
  return <>
    <StatusBar barStyle="light-content" backgroundColor={BrandColors.primary}/>
    <SafeAreaView edges={['top']} style={styles.topSafe}>
      <View style={styles.header}>
        <Pressable accessibilityRole="button" accessibilityLabel={t('mobileMenu.open')} accessibilityHint={t('mobileMenu.openHint')}
          onPress={()=>setOpen(true)} style={styles.menuButton}>
          <MaterialCommunityIcons name="menu" size={28} color={BrandColors.white}/>
        </Pressable>
        <Pressable onPress={()=>navigate('/')} accessibilityRole="button" accessibilityLabel={t('navigation.home')} style={styles.identity}>
          <Image source={require('../../assets/images/jnapanam-brand.png')} style={styles.logo} resizeMode="contain"/>
          <View style={styles.nameWrap}><Text style={styles.brand} numberOfLines={1}>Jnapanam Sports Zone</Text>
            <Text style={styles.location} numberOfLines={1}>{t('common.location')}</Text></View>
        </Pressable>
        <LanguageSwitch compact />
      </View>
    </SafeAreaView>
    <Modal visible={open} animationType="fade" transparent statusBarTranslucent onRequestClose={()=>setOpen(false)}>
      <SafeAreaView style={styles.overlay} edges={['top','bottom']}>
        <Pressable style={styles.backdrop} accessibilityLabel={t('mobileMenu.close')} onPress={()=>setOpen(false)}/>
        <View style={styles.drawer} accessibilityViewIsModal>
          <View style={styles.drawerTop}><Text style={styles.drawerTitle}>{t('mobileMenu.title')}</Text>
            <Pressable onPress={()=>setOpen(false)} accessibilityRole="button" accessibilityLabel={t('mobileMenu.close')} style={styles.close}>
              <MaterialCommunityIcons name="close" size={25} color={BrandColors.textPrimary}/>
            </Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.links} keyboardShouldPersistTaps="handled">
            <Text style={styles.groupTitle}>{t('mobileMenu.customer')}</Text>
            {links.map((item,index)=><View key={item.key}>
              {index===customerItems.length&&<Text style={styles.groupTitle}>{t('mobileMenu.staff')}</Text>}
              <Pressable accessibilityRole="button" accessibilityState={{selected:pathname===item.href}}
                onPress={()=>navigate(item.href)} style={[styles.link,pathname===item.href&&styles.active]}>
                <MaterialCommunityIcons name={item.icon} color={pathname===item.href?BrandColors.accent:BrandColors.primary} size={23}/>
                <Text style={[styles.linkText,pathname===item.href&&styles.activeText]}>{t('mobileMenu.'+item.key)}</Text>
                {pathname===item.href&&<MaterialCommunityIcons name="check" size={18} color={BrandColors.accent}/>}
              </Pressable>
            </View>)}
            {!user&&<Pressable style={styles.link} onPress={()=>navigate('/login')}><MaterialCommunityIcons name="login" size={22} color={BrandColors.primary}/><Text style={styles.linkText}>{t('auth.login')}</Text></Pressable>}
            <Text style={styles.footer}>{t('mobileMenu.webAdmin')}</Text>
          </ScrollView>
        </View>
      </SafeAreaView>
    </Modal>
  </>;
}

const styles=StyleSheet.create({
 topSafe:{backgroundColor:BrandColors.primary},
 header:{height:68,paddingHorizontal:12,flexDirection:'row',alignItems:'center',gap:8},
 menuButton:{width:43,height:46,alignItems:'center',justifyContent:'center',borderRadius:10},
 identity:{flex:1,minWidth:0,flexDirection:'row',gap:8,alignItems:'center'},
 logo:{width:47,height:47,backgroundColor:'#fff',borderRadius:8},
 nameWrap:{flex:1,minWidth:0},
 brand:{fontSize:14,color:BrandColors.white,fontWeight:'900'},
 location:{fontSize:11,color:'#DCE5CF',marginTop:2},
 overlay:{flex:1,flexDirection:'row',backgroundColor:'transparent'},
 backdrop:{...StyleSheet.absoluteFill,backgroundColor:'rgba(8,20,14,.6)'},
 drawer:{width:'85%',maxWidth:350,backgroundColor:BrandColors.background,flex:1,elevation:12},
 drawerTop:{paddingHorizontal:19,paddingVertical:14,backgroundColor:BrandColors.surface,borderBottomWidth:1,borderBottomColor:BrandColors.border,flexDirection:'row',justifyContent:'space-between',alignItems:'center'},
 drawerTitle:{fontSize:20,color:BrandColors.textPrimary,fontWeight:'900'},
 close:{width:44,height:44,alignItems:'center',justifyContent:'center'},
 links:{paddingHorizontal:12,paddingBottom:38},
 groupTitle:{color:BrandColors.textSecondary,fontSize:12,fontWeight:'900',letterSpacing:.7,marginTop:18,marginBottom:6,marginHorizontal:9},
 link:{minHeight:50,flexDirection:'row',alignItems:'center',gap:13,borderRadius:12,paddingHorizontal:12,marginTop:3},
 active:{backgroundColor:BrandColors.accentSoft},
 linkText:{flex:1,color:BrandColors.textPrimary,fontSize:16,fontWeight:'700'},
 activeText:{color:BrandColors.accent,fontWeight:'900'},
 footer:{marginTop:24,paddingHorizontal:10,lineHeight:19,fontSize:12,color:BrandColors.textSecondary},
});
