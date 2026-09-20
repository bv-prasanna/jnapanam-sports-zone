"use client";

import { CSSProperties, useCallback, useEffect, useState } from "react";
import { ApiError, api } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import styles from "./shop.module.css";

type Settings={enabled:boolean;shopName:string;shopNameKn?:string;heroTitle:string;heroTitleKn?:string;heroSubtitle?:string;heroSubtitleKn?:string;logoUrl?:string;primaryColor:string;secondaryColor:string;accentColor:string;backgroundColor:string;textColor:string;loyaltyMaxPercent:number;loyaltyPointValuePaise:number;deliveryEnabled:boolean;deliveryFeePaise:number;payAtFacilityEnabled:boolean};
type Variant={id:string;label:string;pricePaise:number;stockQuantity:number};
type Product={id:string;categoryCode:string;name:string;nameKn?:string;description?:string;descriptionKn?:string;imageUrl?:string;variants:Variant[]};
type Item={variantId:string;productName:string;variantLabel:string;quantity:number;lineTotalPaise:number};
type Cart={items:Item[];subtotalPaise:number};
type Quote={balancePoints:number;maximumRedeemablePoints:number;pointValuePaise:number;maximumDiscountPaise:number};
type Order={id:string;orderNumber:string;status:string;totalPaise:number;originalTotalPaise:number;loyaltyPointsUsed:number;loyaltyDiscountPaise:number;fulfilmentType:string};
const money=(v:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(v/100);
const heroMedia=["/images/hero-sports.jpg","/images/sport-cricket.png","/images/sport-swimming.png"];

export default function Shop(){
 const {user,language}=useApp();const [settings,setSettings]=useState<Settings|null>(null);const [products,setProducts]=useState<Product[]>([]);
 const [cart,setCart]=useState<Cart>({items:[],subtotalPaise:0});const [quote,setQuote]=useState<Quote|null>(null);const [points,setPoints]=useState(0);
 const [orders,setOrders]=useState<Order[]>([]);const [tab,setTab]=useState("shop");const [message,setMessage]=useState("");const [loading,setLoading]=useState(true);const [heroIndex,setHeroIndex]=useState(0);
 const load=useCallback(async()=>{setLoading(true);try{const [s,p]=await Promise.all([api<Settings>("/api/v1/commerce/settings"),api<Product[]>("/api/v1/commerce/products")]);setSettings(s);setProducts(p);if(user){const [c,o]=await Promise.all([api<Cart>("/api/v1/commerce/cart"),api<Order[]>("/api/v1/commerce/orders")]);setCart(c);setOrders(o);if(c.items.length)setQuote(await api<Quote>("/api/v1/commerce/cart/loyalty-quote"));else setQuote(null)}setMessage("")}catch(e){setMessage(e instanceof ApiError?e.message:"The shop could not be loaded. Please try again.")}finally{setLoading(false)}},[user]);
 useEffect(()=>{void load()},[load]);
 useEffect(()=>{const timer=window.setInterval(()=>setHeroIndex(i=>(i+1)%heroMedia.length),5000);return()=>window.clearInterval(timer)},[]);
 async function add(id:string){if(!user){setMessage("Please login to add items.");return}try{setCart(await api<Cart>("/api/v1/commerce/cart/items",{method:"PUT",body:JSON.stringify({variantId:id,quantity:1})}));setQuote(await api<Quote>("/api/v1/commerce/cart/loyalty-quote"));setMessage("Added to cart.")}catch(e){setMessage(errorText(e))}}
 async function remove(id:string){try{const c=await api<Cart>("/api/v1/commerce/cart/items/"+id,{method:"DELETE"});setCart(c);setQuote(c.items.length?await api<Quote>("/api/v1/commerce/cart/loyalty-quote"):null);setPoints(0)}catch(e){setMessage(errorText(e))}}
 async function checkout(){if(!user||!settings)return;try{await api("/api/v1/commerce/checkout",{method:"POST",body:JSON.stringify({fulfilmentType:"PICKUP",paymentMethod:"PAY_AT_PICKUP",recipientName:user.fullName,mobile:user.mobile||"Not provided",loyaltyPoints:points})});setMessage("Order placed. Collect and pay at the facility.");setTab("orders");setPoints(0);await load()}catch(e){setMessage(errorText(e))}}
 if(!settings&&loading)return <main className={styles.loading}>Loading shop…</main>;
 if(!settings)return <><SiteHeader/><main className={styles.page}><section className={styles.unavailable}><h1>Shop temporarily unavailable</h1><p>{message==="Internal Server Error"?"We could not load the shop right now. Please try again in a moment.":message}</p><button className={styles.primary} type="button" onClick={()=>void load()}>Try again</button></section></main><SiteFooter/></>;
 const vars={"--shop-primary":settings.primaryColor,"--shop-secondary":settings.secondaryColor,"--shop-accent":settings.accentColor,"--shop-bg":settings.backgroundColor,"--shop-text":settings.textColor} as CSSProperties;
 const shopName=language==="kn"&&settings.shopNameKn?settings.shopNameKn:settings.shopName;const hero=language==="kn"&&settings.heroTitleKn?settings.heroTitleKn:settings.heroTitle;const subtitle=language==="kn"&&settings.heroSubtitleKn?settings.heroSubtitleKn:settings.heroSubtitle;
 if(!settings.enabled)return <main className={styles.page} style={vars}><section className={styles.unavailable}>{settings.logoUrl&&<img src={settings.logoUrl} alt={shopName}/>}<h1>{shopName}</h1><p>The shop is currently unavailable. Please check again later.</p></section></main>;
 return <><SiteHeader/><main className={styles.page} style={vars}><section className={styles.hero}><div className={styles.heroMedia} style={{backgroundImage:`url(${heroMedia[heroIndex]})`}}/><div className={styles.heroShade}/><div className={styles.heroCopy}><span>{shopName}</span><h1>{hero}</h1><p>{subtitle}</p><div className={styles.heroDots}>{heroMedia.map((_,i)=><button key={i} className={i===heroIndex?styles.heroDotActive:""} onClick={()=>setHeroIndex(i)} aria-label={`Show shop hero ${i+1}`}/>)}</div></div></section>
 <nav className={styles.tabs}>{["shop","cart","orders"].map(x=><button key={x} className={tab===x?styles.active:""} onClick={()=>setTab(x)}>{x==="cart"?`Cart (${cart.items.length})`:x==="orders"?"My orders":"Shop"}</button>)}</nav>
 {message&&<p className={styles.message} role="status">{message}</p>}
 {tab==="shop"&&<section className={styles.grid}>{products.map(p=><article className={styles.card} key={p.id}>{p.imageUrl?<img className={styles.productImage} src={p.imageUrl} alt={p.name}/>:<div className={styles.placeholder}>{shopName}</div>}<small>{p.categoryCode}</small><h2>{language==="kn"&&p.nameKn?p.nameKn:p.name}</h2><p>{language==="kn"&&p.descriptionKn?p.descriptionKn:p.description}</p>{p.variants.map(v=><div className={styles.variant} key={v.id}><span><small>{language==="kn"?"ಗಾತ್ರ":"Size"}</small><b>{v.label}</b></span><span><small>{language==="kn"?"ಬೆಲೆ":"Price"}</small><b>{money(v.pricePaise)}</b></span><button disabled={!v.stockQuantity} onClick={()=>void add(v.id)}>{v.stockQuantity?(language==="kn"?"ಸೇರಿಸಿ":"Add"):(language==="kn"?"ಸ್ಟಾಕ್ ಇಲ್ಲ":"Out of stock")}</button></div>)}{!p.variants.length&&<p className={styles.noVariant}>Pricing and size options will be available soon.</p>}</article>)}</section>}
 {tab==="shop"&&!products.length&&!loading&&<p className={styles.empty}>No merchandise is available yet.</p>}
 {tab==="cart"&&<section className={styles.panel}><h2>Your cart</h2>{cart.items.map(i=><div className={styles.row} key={i.variantId}><div><b>{i.productName}</b><span>{i.variantLabel+" · Qty "+i.quantity}</span></div><b>{money(i.lineTotalPaise)}</b><button onClick={()=>void remove(i.variantId)}>Remove</button></div>)}{cart.items.length?<><div className={styles.total}><span>Subtotal</span><b>{money(cart.subtotalPaise)}</b></div>{quote&&quote.maximumRedeemablePoints>0&&<label className={styles.loyalty}><span>Use loyalty points</span><input type="number" min="0" max={quote.maximumRedeemablePoints} value={points} onChange={e=>setPoints(Math.min(quote.maximumRedeemablePoints,Math.max(0,Number(e.target.value))))}/><small>Balance: {quote.balancePoints} · Maximum for this cart: {quote.maximumRedeemablePoints} · Discount: {money(points*quote.pointValuePaise)}</small></label>}<div className={styles.payable}><span>Payable</span><b>{money(cart.subtotalPaise-points*(quote?.pointValuePaise||0))}</b></div><button className={styles.primary} disabled={!settings.payAtFacilityEnabled} onClick={()=>void checkout()}>{settings.payAtFacilityEnabled?"Collect & pay at facility":"Pay at facility unavailable"}</button></>:<p>Your cart is empty.</p>}</section>}
 {tab==="orders"&&<section className={styles.panel}><h2>My orders</h2>{orders.map(o=><div className={styles.row} key={o.id}><div><b>{o.orderNumber}</b><span>{o.fulfilmentType}{o.loyaltyPointsUsed?` · ${o.loyaltyPointsUsed} points used`:""}</span></div><b>{money(o.totalPaise)}</b><em>{o.status}</em></div>)}{!orders.length&&<p>No orders yet.</p>}</section>}
 </main><SiteFooter/></>
}
function errorText(e:unknown){return e instanceof ApiError?e.message:"Unable to complete this action."}

