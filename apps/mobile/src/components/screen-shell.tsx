import { MaterialCommunityIcons } from '@expo/vector-icons';
import { PropsWithChildren } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { BrandColors } from '@/constants/brand';

export function ScreenShell({ title, children }: PropsWithChildren<{ title: string }>) {
  return <KeyboardAvoidingView behavior={Platform.OS==='ios'?'padding':undefined} style={styles.screen}>
    <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.content}>
      <Text style={styles.title}>{title}</Text>
      {children}
    </ScrollView>
  </KeyboardAvoidingView>;
}
export function LoadingScreen(){return <View style={styles.loading}><ActivityIndicator size="large" color={BrandColors.accent}/></View>;}
export function EmptyState({icon,title,description}:{icon:keyof typeof MaterialCommunityIcons.glyphMap;title:string;description:string}){
  return <View style={styles.empty}>
    <MaterialCommunityIcons name={icon} size={42} color={BrandColors.accent}/>
    <Text style={styles.emptyTitle}>{title}</Text><Text style={styles.emptyDescription}>{description}</Text>
  </View>;
}
const styles=StyleSheet.create({
  screen:{flex:1,backgroundColor:BrandColors.background},
  content:{padding:20,paddingTop:22,paddingBottom:50,width:'100%',maxWidth:900,alignSelf:'center'},
  title:{color:BrandColors.textPrimary,fontSize:27,fontWeight:'900',marginBottom:20},
  loading:{flex:1,alignItems:'center',justifyContent:'center',backgroundColor:BrandColors.background},
  empty:{backgroundColor:BrandColors.surface,borderColor:BrandColors.border,borderWidth:1,borderRadius:20,padding:28,alignItems:'center'},
  emptyTitle:{color:BrandColors.textPrimary,fontSize:18,fontWeight:'800',marginTop:13},
  emptyDescription:{color:BrandColors.textSecondary,fontSize:14,lineHeight:20,textAlign:'center',marginTop:6}
});
