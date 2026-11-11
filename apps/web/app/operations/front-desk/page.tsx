'use client';
import {useEffect,useState} from 'react';
import {api} from '@/lib/api';
import {SiteHeader} from '@/components/site-header';
import {PortalSideNavigation} from '@/components/portal-side-navigation';
import shell from '@/components/portal-shell.module.css';
import styles from './front-desk.module.css';

type Equipment={id:string;equipment_name:string;facility_name?:string;available_quantity:number;total_quantity:number;deposit_paise:number};
export default function FrontDesk(){
 const [customer,setCustomer]=useState(''),[slot,setSlot]=useState(''),[qty,setQty]=useState(1);
 const [msg,setMsg]=useState(''),[inventory,setInventory]=useState<Equipment[]>([]);
 async function load(){try{setInventory(await api<Equipment[]>('/api/v1/release1/ops/equipment-inventory'));}catch{setMsg('Unable to load equipment availability.');}}
 useEffect(()=>{void load()},[]);
 async function walkIn(e:React.FormEvent<HTMLFormElement>){
  e.preventDefault();
  if(!customer.trim()||!slot.trim()||!Number.isInteger(qty)||qty<1){setMsg('Enter valid customer, slot and quantity.');return;}
  try{
   setMsg('');
   const result=await api<{id:string}>('/api/v1/frontdesk/walk-in',{
    method:'POST',body:JSON.stringify({customerUserId:customer.trim(),slotId:slot.trim(),quantity:qty})
   });
   setMsg('Walk-in booking created: '+result.id);
  }catch(e){setMsg(e instanceof Error?e.message:'Unable to create booking');}
 }
 return <><SiteHeader/><div className={shell.shell}><PortalSideNavigation active="operations"/><main className={styles.page}>
  <header className={styles.header}><span>OPERATIONS</span><h1>Front Desk</h1><p>Walk-in booking, pay-at-venue and equipment availability.</p></header>
  <section className={styles.card}><h2>Walk-in booking</h2>
   <form className={styles.form} onSubmit={walkIn}>
    <label>Customer user ID<input required value={customer} onChange={e=>setCustomer(e.target.value)} placeholder="Enter customer ID"/></label>
    <label>Available slot ID<input required value={slot} onChange={e=>setSlot(e.target.value)} placeholder="Enter slot ID"/></label>
    <label>Members<input type="number" min={1} required value={qty} onChange={e=>setQty(Number(e.target.value))}/></label>
    <button type="submit">Create pay-at-venue booking</button>
   </form>
   {msg&&<p className={styles.notice} role="status">{msg}</p>}
  </section>
  <section className={styles.card}><div className={styles.sectionHead}><h2>Equipment availability</h2><button type="button" onClick={()=>void load()}>Refresh</button></div>
   <div className={styles.tableScroll}><table><thead><tr><th>Equipment</th><th>Facility</th><th>Available</th><th>Total</th><th>Deposit</th></tr></thead><tbody>{inventory.map(x=><tr key={x.id}><td>{x.equipment_name}</td><td>{x.facility_name||'General'}</td><td>{x.available_quantity}</td><td>{x.total_quantity}</td><td>₹{Number(x.deposit_paise||0)/100}</td></tr>)}</tbody></table></div>
  </section>
 </main></div></>;
}
