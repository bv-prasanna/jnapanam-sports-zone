import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { BrandColors } from '@/constants/brand';
import { setStoredValue } from '@/lib/storage';

export function LanguageSwitch({compact=false}:{compact?:boolean}){
  const {i18n}=useTranslation();
  const selected=i18n.resolvedLanguage==='kn'?'kn':'en';
  async function choose(language:'en'|'kn'){
    await i18n.changeLanguage(language);
    await setStoredValue('jnapanam.language',language);
  }
  return <View style={[styles.row,compact&&styles.compact]}>
    {(['en','kn'] as const).map(language=><Pressable key={language} accessibilityRole="button"
      accessibilityLabel={language==='en'?'English':'ಕನ್ನಡ'} accessibilityState={{selected:selected===language}}
      onPress={()=>void choose(language)} style={[styles.option,compact&&styles.compactOption,selected===language&&styles.selected]}>
      <Text style={[styles.text,compact&&styles.compactText,selected===language&&styles.selectedText]}>{language==='en'?'EN':'ಕನ್ನಡ'}</Text>
    </Pressable>)}
  </View>;
}
const styles=StyleSheet.create({
 row:{flexDirection:'row',backgroundColor:'#E9E4D8',borderRadius:12,padding:3,alignSelf:'center'},
 compact:{maxWidth:113},
 option:{paddingHorizontal:12,paddingVertical:9,borderRadius:9,minHeight:37,justifyContent:'center'},
 compactOption:{paddingHorizontal:6,paddingVertical:7,minWidth:42,alignItems:'center'},
 selected:{backgroundColor:BrandColors.primary},
 text:{color:BrandColors.textSecondary,fontSize:12,fontWeight:'700'},
 compactText:{fontSize:12},
 selectedText:{color:BrandColors.white}
});
