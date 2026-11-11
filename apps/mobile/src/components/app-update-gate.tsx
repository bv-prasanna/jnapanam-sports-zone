import {PropsWithChildren,useCallback,useEffect,useState} from 'react';
import {AppState,Linking,Platform,Pressable,ScrollView,StyleSheet,Text,View,ActivityIndicator} from 'react-native';
import {MaterialCommunityIcons} from '@expo/vector-icons';
import {BrandColors} from '@/constants/brand';
import {API_URL} from '@/lib/api';
import {AppReleasePolicy,clearServerUpdateBlock,decideUpdate,installedAndroidBuild,
 installedVersionName,loadReleasePolicy,onServerRequiredUpdate,PERMANENT_APP_LINK,savedReleasePolicy,wasBlockedByServer} from '@/lib/app-release';

export function AppUpdateGate({children}:PropsWithChildren){
 const [checking,setChecking]=useState(Platform.OS==='android');
 const [policy,setPolicy]=useState<AppReleasePolicy|null>(null);
 const [serverBlock,setServerBlock]=useState(wasBlockedByServer());
 const [dismissedCode,setDismissedCode]=useState<number|null>(null);
 const [error,setError]=useState('');
 const installed=installedAndroidBuild();
 const check=useCallback(async()=>{
   if(Platform.OS!=='android')return;
   const controller=new AbortController();
   const abort=setTimeout(()=>controller.abort(),7000);
   try{
     const next=await loadReleasePolicy(API_URL,controller.signal);
     setPolicy(next);setError('');clearServerUpdateBlock();setServerBlock(false);
     if(next.latestVersionCode!==dismissedCode)setDismissedCode(null);
   }catch{
     // A previously known mandatory policy remains enforceable offline.
     const cached=await savedReleasePolicy();
     if(cached)setPolicy(previous=>previous||cached);
     setError('Could not check the latest app version. Check your connection and retry.');
   }finally{clearTimeout(abort);setChecking(false);}
 },[dismissedCode]);

 useEffect(()=>{
   if(Platform.OS!=='android')return;
   void check();
   const sub=AppState.addEventListener('change',status=>{if(status==='active')void check();});
   const unsubscribe=onServerRequiredUpdate(()=>{setServerBlock(true);void check();});
   return()=>{sub.remove();unsubscribe();};
 },[check]);

 if(Platform.OS!=='android')return <>{children}</>;
 if(checking)return <View style={styles.center}><ActivityIndicator size="large" color={BrandColors.accent}/><Text style={styles.label}>Checking Jnapanam app version…</Text></View>;
 const required=serverBlock||decideUpdate(policy,installed)==='mandatory';
 const available=decideUpdate(policy,installed)==='optional';
 if(required||available&&dismissedCode!==policy?.latestVersionCode){
   return <ScrollView contentContainerStyle={styles.screen}>
    <View style={styles.card}>
     <MaterialCommunityIcons name={required?'alert-circle-outline':'cellphone-arrow-down'} color={BrandColors.accent} size={58}/>
     <Text style={styles.title}>{required?'App update required':'New app version available'}</Text>
     <Text style={styles.copy}>{required
       ?'To continue using Jnapanam, install the latest Android app version. Your current build is no longer supported.'
       :'A newer Jnapanam Android app is available. You can update now or continue using this version.'}</Text>
     <Text style={styles.version}>Installed {installedVersionName()} (build {installed}) · Latest {policy?.latestVersionName||'available'} (build {policy?.latestVersionCode||'—'})</Text>
     {!!policy?.releaseNotes&&<Text style={styles.notes}>{policy.releaseNotes}</Text>}
     <Pressable accessibilityRole="button" style={styles.action} onPress={()=>void Linking.openURL(PERMANENT_APP_LINK)}>
      <MaterialCommunityIcons name="download" color="#fff" size={19}/><Text style={styles.actionText}>Download updated app</Text>
     </Pressable>
     {!required&&<Pressable accessibilityRole="button" style={styles.outline} onPress={()=>setDismissedCode(policy?.latestVersionCode||0)}>
       <Text style={styles.outlineText}>Later · Continue to app</Text></Pressable>}
     <Pressable accessibilityRole="button" style={styles.outline} onPress={()=>void check()}>
       <Text style={styles.outlineText}>I've updated · Check again</Text></Pressable>
     {!!error&&<Text style={styles.error}>{error}</Text>}
     <Text style={styles.hint}>Normally Android can update this app without uninstalling it when the APK has the same package name and signing certificate. If Android reports a signature conflict, contact Jnapanam support before uninstalling because local app data may be lost.</Text>
    </View>
   </ScrollView>;
 }
 return <>{children}</>;
}
const styles=StyleSheet.create({
 screen:{flexGrow:1,backgroundColor:BrandColors.background,padding:23,alignItems:'center',justifyContent:'center'},
 center:{flex:1,alignItems:'center',justifyContent:'center',gap:14,backgroundColor:BrandColors.background},
 card:{width:'100%',maxWidth:510,backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,
  borderRadius:20,padding:25,alignItems:'center',gap:14},
 label:{color:BrandColors.textSecondary,fontWeight:'800'},
 title:{fontSize:25,fontWeight:'900',color:BrandColors.textPrimary,textAlign:'center'},
 copy:{color:BrandColors.textPrimary,fontSize:15,textAlign:'center',lineHeight:23},
 version:{fontWeight:'800',fontSize:13,textAlign:'center',color:BrandColors.primary},
 notes:{fontSize:13,lineHeight:20,color:BrandColors.textSecondary,backgroundColor:'#f5eee1',padding:11,borderRadius:10,alignSelf:'stretch'},
 action:{backgroundColor:BrandColors.primary,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:10,
  padding:14,borderRadius:12,alignSelf:'stretch'},
 actionText:{color:'#fff',fontWeight:'900',fontSize:16},
 outline:{borderColor:BrandColors.border,borderWidth:1,borderRadius:12,padding:13,alignSelf:'stretch',alignItems:'center'},
 outlineText:{color:BrandColors.primary,fontWeight:'900'},
 hint:{fontSize:12,color:BrandColors.textSecondary,lineHeight:18,textAlign:'center'},
 error:{fontSize:12,color:'#9c3219'}
});
