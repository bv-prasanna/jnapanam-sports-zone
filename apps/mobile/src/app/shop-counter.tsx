import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Variant = { id:string; sku:string; label:string; pricePaise:number; stockQuantity:number; status:string };
type Product = { id:string; name:string; status:string; variants:Variant[] };
type Order = { id:string; order_number:string; status:string; fulfilment_type:string; payment_method:string; payment_status:string; total_paise:number; recipient_name:string; mobile?:string; created_at:string };

type Tab = 'ORDERS'|'WALK_IN';
const staffRoles = ['SUPER_ADMIN','ADMIN','SHOP_KEEPER'];
const orderSteps = ['PLACED','CONFIRMED','PACKED','READY_FOR_PICKUP','DELIVERED'];

function money(paise:number){return new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format((paise||0)/100)}

export default function ShopCounterScreen(){
  const {user}=useAuth();
  const allowed=!!user?.roles.some(role=>staffRoles.includes(role));
  const [tab,setTab]=useState<Tab>('ORDERS');
  const [orders,setOrders]=useState<Order[]>([]); const [products,setProducts]=useState<Product[]>([]);
  const [query,setQuery]=useState(''); const [busy,setBusy]=useState(''); const [loading,setLoading]=useState(true); const [refreshing,setRefreshing]=useState(false); const [error,setError]=useState('');

  const load=useCallback(async()=>{
    if(!allowed)return;
    try{setError('');const [o,p]=await Promise.all([api<Order[]>('/api/v1/admin/commerce/orders'),api<Product[]>('/api/v1/admin/commerce/products')]);setOrders(o);setProducts(p.filter(x=>x.status==='ACTIVE'));}
    catch(e){setError(e instanceof ApiError?e.message:'Unable to load shop counter.');}
    finally{setLoading(false);setRefreshing(false)}
  },[allowed]);
  useEffect(()=>{void load()},[load]);

  const visibleOrders=useMemo(()=>{const q=query.trim().toLowerCase();return orders.filter(o=>!q||[o.order_number,o.recipient_name,o.mobile,o.status,o.fulfilment_type].some(v=>v?.toLowerCase().includes(q)))},[orders,query]);

  async function updateStatus(order:Order,status:string){try{setBusy(order.id+status);setError('');await api(`/api/v1/admin/commerce/orders/${order.id}/status`,{method:'PATCH',body:JSON.stringify({status})});await load()}catch(e){setError(e instanceof ApiError?e.message:'Unable to update order.')}finally{setBusy('')}}
  function nextStatus(order:Order){const index=orderSteps.indexOf(order.status);return index>=0&&index<orderSteps.length-1?orderSteps[index+1]:null}

  if(!allowed)return <View style={styles.center}><Text style={styles.title}>Shop Keeper access required</Text><Text style={styles.muted}>Sign in with a Shop Keeper, Admin, or Super Admin account.</Text></View>;

  return <ScrollView style={styles.screen} contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={()=>{setRefreshing(true);void load()}}/>}>
    <View style={styles.heading}><View><Text style={styles.eyebrow}>SHOP KEEPER</Text><Text style={styles.title}>Shop Counter</Text></View><Pressable style={styles.refresh} onPress={()=>void load()}><MaterialCommunityIcons name="refresh" size={23} color={BrandColors.primary}/></Pressable></View>
    <Text style={styles.muted}>Handle prebooked pickup/delivery and prepare walk-in sales. Cash/UPI refunds require Admin approval.</Text>
    <View style={styles.tabs}>{(['ORDERS','WALK_IN'] as Tab[]).map(value=><Pressable key={value} onPress={()=>setTab(value)} style={[styles.tab,tab===value&&styles.tabActive]}><Text style={[styles.tabText,tab===value&&styles.tabTextActive]}>{value==='ORDERS'?'Prebooked orders':'Walk-in sale'}</Text></Pressable>)}</View>
    {!!error&&<Text style={styles.error}>{error}</Text>}
    {loading?<ActivityIndicator color={BrandColors.accent}/>:tab==='ORDERS'?<>
      <View style={styles.search}><MaterialCommunityIcons name="magnify" size={20} color={BrandColors.textSecondary}/><TextInput value={query} onChangeText={setQuery} placeholder="Order no., customer, mobile, status" style={styles.searchInput}/></View>
      <Text style={styles.count}>{visibleOrders.length} orders</Text>
      {visibleOrders.map(order=>{const next=nextStatus(order);return <View key={order.id} style={styles.card}><View style={styles.row}><Text style={styles.number}>{order.order_number}</Text><Text style={styles.status}>{order.status.replaceAll('_',' ')}</Text></View><Text style={styles.customer}>{order.recipient_name} {order.mobile?`· ${order.mobile}`:''}</Text><Text style={styles.detail}>{order.fulfilment_type.replaceAll('_',' ')} · {money(order.total_paise)}</Text><Text style={[styles.payment,order.payment_status==='PAID'&&styles.paid]}>Payment: {order.payment_status} · {order.payment_method.replaceAll('_',' ')}</Text><View style={styles.actions}>{next&&<Pressable disabled={!!busy} style={styles.action} onPress={()=>void updateStatus(order,next)}><MaterialCommunityIcons name="arrow-right-circle-outline" size={18} color={BrandColors.primary}/><Text style={styles.actionText}>{busy===order.id+next?'Updating…':`Move to ${next.replaceAll('_',' ')}`}</Text></Pressable>}</View></View>})}
      {visibleOrders.length===0&&<Text style={styles.empty}>No matching orders.</Text>}
    </>:<>
      <View style={styles.notice}><Text style={styles.noticeTitle}>Walk-in POS foundation</Text><Text style={styles.muted}>Active products and stock are shown below. Full counter cart, receipt, exchange/store-credit and refund-approval workflow are the next commerce step; this screen intentionally does not create an untracked sale.</Text></View>
      {products.flatMap(product=>product.variants.filter(v=>v.status==='ACTIVE').map(variant=><View key={variant.id} style={styles.card}><Text style={styles.customer}>{product.name}</Text><Text style={styles.detail}>{variant.label} · {variant.sku}</Text><View style={styles.row}><Text style={styles.number}>{money(variant.pricePaise)}</Text><Text style={styles.detail}>Stock {variant.stockQuantity}</Text></View></View>))}
      {products.length===0&&<Text style={styles.empty}>No active products.</Text>}
    </>}
  </ScrollView>;
}

const styles=StyleSheet.create({screen:{flex:1,backgroundColor:BrandColors.background},content:{padding:20,paddingTop:52,paddingBottom:110,gap:14},center:{flex:1,padding:28,alignItems:'center',justifyContent:'center',backgroundColor:BrandColors.background,gap:8},heading:{flexDirection:'row',justifyContent:'space-between',alignItems:'center'},eyebrow:{fontSize:12,fontWeight:'900',letterSpacing:1.2,color:BrandColors.accent},title:{fontSize:30,fontWeight:'900',color:BrandColors.textPrimary,marginTop:4},muted:{color:BrandColors.textSecondary,fontSize:14,lineHeight:21},refresh:{width:44,height:44,borderRadius:22,borderWidth:1,borderColor:BrandColors.border,backgroundColor:BrandColors.surface,alignItems:'center',justifyContent:'center'},tabs:{flexDirection:'row',gap:8},tab:{flex:1,borderWidth:1,borderColor:BrandColors.border,borderRadius:12,paddingVertical:11,alignItems:'center',backgroundColor:BrandColors.surface},tabActive:{backgroundColor:BrandColors.primary,borderColor:BrandColors.primary},tabText:{fontSize:12,fontWeight:'900',color:BrandColors.textSecondary},tabTextActive:{color:BrandColors.white},search:{flexDirection:'row',alignItems:'center',backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:14,paddingHorizontal:12},searchInput:{flex:1,minHeight:48,marginLeft:8},count:{fontWeight:'800',color:BrandColors.textSecondary},card:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:16,padding:15,gap:6},row:{flexDirection:'row',justifyContent:'space-between',gap:10,alignItems:'center'},number:{fontWeight:'900',color:BrandColors.accent},status:{fontSize:10,fontWeight:'900',color:BrandColors.primary,backgroundColor:'#EAF2F6',paddingHorizontal:8,paddingVertical:4,borderRadius:10},customer:{fontSize:17,fontWeight:'900',color:BrandColors.textPrimary},detail:{color:BrandColors.textSecondary},payment:{fontWeight:'800',color:'#9A3412'},paid:{color:'#146C43'},actions:{flexDirection:'row',flexWrap:'wrap',gap:8,marginTop:5},action:{flexDirection:'row',alignItems:'center',gap:6,borderWidth:1,borderColor:BrandColors.primary,borderRadius:10,paddingHorizontal:10,paddingVertical:8},actionText:{fontSize:11,fontWeight:'900',color:BrandColors.primary},notice:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:16,padding:15,gap:5},noticeTitle:{fontWeight:'900',fontSize:17,color:BrandColors.textPrimary},error:{color:'#A32613',backgroundColor:'#FFF0ED',borderRadius:12,padding:12},empty:{textAlign:'center',padding:28,color:BrandColors.textSecondary}});
