"use client";

import { FormEvent, ReactNode, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ApiError, api } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./commerce.module.css";
import { CatalogueImport } from "./catalogue-import";

type Settings = {
  enabled:boolean; shopName:string; shopNameKn?:string; heroTitle:string; heroTitleKn?:string;
  heroSubtitle?:string; heroSubtitleKn?:string; logoUrl?:string; primaryColor:string;
  secondaryColor:string; accentColor:string; backgroundColor:string; textColor:string;
  loyaltyMaxPercent:number; loyaltyPointValuePaise:number; deliveryEnabled:boolean;
  deliveryFeePaise:number; payAtFacilityEnabled:boolean; contactPhone?:string; contactEmail?:string;
};
type Category = {id:string;code:string;name:string;nameKn?:string;description?:string;imageUrl?:string;status:string;displayOrder:number;loyaltyMaxPercent?:number};
type Variant = {id:string;sku:string;label:string;pricePaise:number;stockQuantity:number;lowStockThreshold:number;status:string};
type Product = {id:string;categoryId:string;categoryCode:string;sku:string;name:string;nameKn?:string;description?:string;descriptionKn?:string;imageUrl?:string;taxPercent:number;status:string;loyaltyMaxPercent?:number;variants:Variant[]};
type Order = {id:string;order_number:string;status:string;fulfilment_type:string;total_paise:number;recipient_name:string;created_at:string};
type Tab = "branding"|"categories"|"products"|"import"|"inventory"|"orders";
const emptyCategory={code:"",name:"",nameKn:"",description:"",imageUrl:"",displayOrder:0,loyaltyMaxPercent:"",status:"ACTIVE"};
const emptyProduct={categoryId:"",sku:"",name:"",nameKn:"",description:"",descriptionKn:"",imageUrl:"",taxPercent:0,loyaltyMaxPercent:"",status:"DRAFT"};

export default function CommerceAdminPage(){
  const {user,ready}=useApp();
  const [tab,setTab]=useState<Tab>("branding");
  const [settings,setSettings]=useState<Settings|null>(null);
  const [categories,setCategories]=useState<Category[]>([]);
  const [products,setProducts]=useState<Product[]>([]);
  const [orders,setOrders]=useState<Order[]>([]);
  const [category,setCategory]=useState({...emptyCategory});
  const [product,setProduct]=useState({...emptyProduct});
  const [variant,setVariant]=useState({productId:"",sku:"",label:"",price:"",stock:"0",lowStock:"5",status:"ACTIVE"});
  const [stock,setStock]=useState({variantId:"",quantity:"",reason:"Stock count adjustment"});
  const [editingCategoryId,setEditingCategoryId]=useState<string|null>(null);
  const [editingProductId,setEditingProductId]=useState<string|null>(null);
  const [message,setMessage]=useState(""); const [busy,setBusy]=useState(false);
  const isAdmin=!!user?.roles.some(r=>["SUPER_ADMIN","ADMIN"].includes(r));
  const isSuper=!!user?.roles.includes("SUPER_ADMIN");

  const load=useCallback(async()=>{
    if(!isAdmin)return;
    try{
      const [s,c,p,o]=await Promise.all([
        api<Settings>("/api/v1/admin/commerce/settings"),
        api<Category[]>("/api/v1/admin/commerce/categories"),
        api<Product[]>("/api/v1/admin/commerce/products"),
        api<Order[]>("/api/v1/admin/commerce/orders")
      ]);
      setSettings(s);setCategories(c);setProducts(p);setOrders(o);
      setProduct(v=>({...v,categoryId:v.categoryId||c[0]?.id||""}));
      setVariant(v=>({...v,productId:v.productId||p[0]?.id||""}));
    }catch(e){setMessage(errorText(e))}
  },[isAdmin]);
  useEffect(()=>{void load()},[load]);
  if(!ready)return <main className={styles.page}>Checking access…</main>;
  if(!isAdmin)return <main className={styles.page}><h1>Access denied</h1><Link href="/dashboard">Return to dashboard</Link></main>;

  async function run(action:()=>Promise<unknown>,success:string){
    try{setBusy(true);setMessage("");await action();setMessage(success);await load()}
    catch(e){setMessage(errorText(e))}finally{setBusy(false)}
  }
  async function saveSettings(e:FormEvent){e.preventDefault();if(!settings)return;await run(
    ()=>api("/api/v1/admin/commerce/settings",{method:"PUT",body:JSON.stringify(settings)}),"Shop settings saved.")}
  async function addCategory(e:FormEvent){e.preventDefault();const body=JSON.stringify({...category,loyaltyMaxPercent:numberOrNull(category.loyaltyMaxPercent)});await run(
    ()=>api(editingCategoryId?"/api/v1/admin/commerce/categories/"+editingCategoryId:"/api/v1/admin/commerce/categories",{method:editingCategoryId?"PUT":"POST",body}),
    editingCategoryId?"Category updated.":"Category added.");setCategory({...emptyCategory});setEditingCategoryId(null)}
  async function addProduct(e:FormEvent){e.preventDefault();const body=JSON.stringify({...product,loyaltyMaxPercent:numberOrNull(product.loyaltyMaxPercent)});await run(
    ()=>api(editingProductId?"/api/v1/admin/commerce/products/"+editingProductId:"/api/v1/admin/commerce/products",{method:editingProductId?"PUT":"POST",body}),
    editingProductId?"Product updated.":"Product added.");setProduct({...emptyProduct,categoryId:categories[0]?.id||""});setEditingProductId(null)}
  function editCategory(c:Category){setCategory({code:c.code,name:c.name,nameKn:c.nameKn||"",description:c.description||"",imageUrl:c.imageUrl||"",displayOrder:c.displayOrder,loyaltyMaxPercent:c.loyaltyMaxPercent??"",status:c.status});setEditingCategoryId(c.id);setTab("categories");window.scrollTo({top:0,behavior:"smooth"})}
  function editProduct(p:Product){setProduct({categoryId:p.categoryId,sku:p.sku,name:p.name,nameKn:p.nameKn||"",description:p.description||"",descriptionKn:p.descriptionKn||"",imageUrl:p.imageUrl||"",taxPercent:p.taxPercent,loyaltyMaxPercent:p.loyaltyMaxPercent??"",status:p.status});setEditingProductId(p.id);setTab("products");window.scrollTo({top:0,behavior:"smooth"})}
  function uploadLogo(file?:File){if(!file)return;if(file.size>1500000){setMessage("Logo must be smaller than 1.5 MB.");return}const reader=new FileReader();reader.onload=()=>setSettings(s=>s?{...s,logoUrl:String(reader.result)}:s);reader.onerror=()=>setMessage("Unable to read logo image.");reader.readAsDataURL(file)}
  async function addVariant(e:FormEvent){e.preventDefault();await run(
    ()=>api("/api/v1/admin/commerce/products/"+variant.productId+"/variants",{method:"POST",body:JSON.stringify({sku:variant.sku,label:variant.label,pricePaise:Math.round(Number(variant.price)*100),stockQuantity:Number(variant.stock),lowStockThreshold:Number(variant.lowStock),status:variant.status})}),
    "Variant and opening inventory added.")}
  async function adjustStock(e:FormEvent){e.preventDefault();await run(
    ()=>api("/api/v1/admin/commerce/variants/"+stock.variantId+"/stock",{method:"POST",body:JSON.stringify({quantityDelta:Number(stock.quantity),reason:stock.reason})}),
    "Inventory updated.");setStock(v=>({...v,quantity:""}))}
  async function orderStatus(id:string,status:string){await run(
    ()=>api("/api/v1/admin/commerce/orders/"+id+"/status",{method:"PATCH",body:JSON.stringify({status})}),"Order status updated.")}

  return <main className={styles.page}>
    <header className={styles.header}><div><span>ADMIN COMMERCE</span><h1>Shop management</h1><p>Branding, catalogue, pricing, inventory, loyalty and orders.</p></div><Link className={styles.back} href="/admin">← Admin console</Link></header>
    <nav className={styles.tabs}>{(["branding","categories","products","import","inventory","orders"] as Tab[]).map(x=><button key={x} className={tab===x?styles.active:""} onClick={()=>setTab(x)}>{title(x)}</button>)}</nav>
    {message&&<div className={styles.message}>{message}</div>}

    {tab==="branding"&&settings&&<form className={styles.form} onSubmit={saveSettings}>
      <Section title="Identity and content">
        <Field label="Shop name"><input value={settings.shopName} onChange={e=>setSettings({...settings,shopName:e.target.value})} required/></Field>
        <Field label="Kannada shop name"><input value={settings.shopNameKn||""} onChange={e=>setSettings({...settings,shopNameKn:e.target.value})}/></Field>
        <Field label="Hero title"><input value={settings.heroTitle} onChange={e=>setSettings({...settings,heroTitle:e.target.value})} required/></Field>
        <Field label="Kannada hero title"><input value={settings.heroTitleKn||""} onChange={e=>setSettings({...settings,heroTitleKn:e.target.value})}/></Field>
        <Field label="Hero subtitle"><input value={settings.heroSubtitle||""} onChange={e=>setSettings({...settings,heroSubtitle:e.target.value})}/></Field>
        <Field label="Kannada hero subtitle"><input value={settings.heroSubtitleKn||""} onChange={e=>setSettings({...settings,heroSubtitleKn:e.target.value})}/></Field>
        <Field label="Logo URL"><input value={settings.logoUrl||""} onChange={e=>setSettings({...settings,logoUrl:e.target.value})} placeholder="/images/jnapanam-logo.png"/></Field>
        <Field label="Upload logo image"><input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" onChange={e=>uploadLogo(e.target.files?.[0])}/>{settings.logoUrl&&<img className={styles.logoPreview} src={settings.logoUrl} alt="Shop logo preview"/>}</Field>
      </Section>
      <Section title="Theme colours">
        {(["primaryColor","secondaryColor","accentColor","backgroundColor","textColor"] as const).map(k=><Field label={title(k)} key={k}><div className={styles.color}><input type="color" value={settings[k]} onChange={e=>setSettings({...settings,[k]:e.target.value})}/><input value={settings[k]} pattern="^#[0-9A-Fa-f]{6}$" onChange={e=>setSettings({...settings,[k]:e.target.value})}/></div></Field>)}
      </Section>
      <Section title="Payments, delivery and loyalty">
        <Field label="Shop-wide maximum loyalty %"><input type="number" min="0" max="100" step=".01" value={settings.loyaltyMaxPercent} onChange={e=>setSettings({...settings,loyaltyMaxPercent:Number(e.target.value)})}/></Field>
        <Field label="Value of one point (paise)"><input type="number" min="1" value={settings.loyaltyPointValuePaise} onChange={e=>setSettings({...settings,loyaltyPointValuePaise:Number(e.target.value)})}/></Field>
        <Field label="Delivery fee (₹)"><input type="number" min="0" step=".01" value={settings.deliveryFeePaise/100} onChange={e=>setSettings({...settings,deliveryFeePaise:Math.round(Number(e.target.value)*100)})}/></Field>
        <Field label="Contact phone"><input value={settings.contactPhone||""} onChange={e=>setSettings({...settings,contactPhone:e.target.value})}/></Field>
        <Field label="Contact email"><input type="email" value={settings.contactEmail||""} onChange={e=>setSettings({...settings,contactEmail:e.target.value})}/></Field>
        <label className={styles.check}><input type="checkbox" checked={settings.enabled} onChange={e=>setSettings({...settings,enabled:e.target.checked})}/> Shop enabled</label>
        <label className={styles.check}><input type="checkbox" checked={settings.deliveryEnabled} onChange={e=>setSettings({...settings,deliveryEnabled:e.target.checked})}/> Delivery enabled</label>
        <label className={styles.check}><input type="checkbox" checked={settings.payAtFacilityEnabled} onChange={e=>setSettings({...settings,payAtFacilityEnabled:e.target.checked})}/> Pay at facility enabled</label>
      </Section>
      <button className={styles.primary} disabled={busy||!isSuper}>{isSuper?"Save shop settings":"Only Super Admin can change shop settings"}</button>
    </form>}

    {tab==="categories"&&<div className={styles.twoColumn}><form className={styles.form} onSubmit={addCategory}><h2>{editingCategoryId?"Edit category":"Add category"}</h2>
      <Field label="Code"><input value={category.code} onChange={e=>setCategory({...category,code:e.target.value})} required/></Field>
      <Field label="Name"><input value={category.name} onChange={e=>setCategory({...category,name:e.target.value})} required/></Field>
      <Field label="Kannada name"><input value={category.nameKn} onChange={e=>setCategory({...category,nameKn:e.target.value})}/></Field>
      <Field label="Category loyalty limit % (blank uses shop default)"><input type="number" min="0" max="100" value={category.loyaltyMaxPercent} onChange={e=>setCategory({...category,loyaltyMaxPercent:e.target.value})}/></Field>
      <button className={styles.primary} disabled={busy}>{editingCategoryId?"Save category":"Add category"}</button>{editingCategoryId&&<button type="button" className={styles.secondary} onClick={()=>{setEditingCategoryId(null);setCategory({...emptyCategory})}}>Cancel edit</button>}</form>
      <List title="Categories">{categories.map(c=><article key={c.id}><b>{c.name}</b><span>{c.code} · {c.status} · Loyalty {c.loyaltyMaxPercent??"shop default"}%</span><div className={styles.actions}><button onClick={()=>editCategory(c)}>Edit</button><button onClick={()=>{editCategory({...c,status:c.status==="ACTIVE"?"INACTIVE":"ACTIVE"});}}>{c.status==="ACTIVE"?"Deactivate":"Activate"}</button></div></article>)}</List></div>}

    {tab==="products"&&<div className={styles.twoColumn}><form className={styles.form} onSubmit={addProduct}><h2>{editingProductId?"Edit product":"Add product"}</h2>
      <Field label="Category"><select value={product.categoryId} onChange={e=>setProduct({...product,categoryId:e.target.value})}>{categories.map(c=><option value={c.id} key={c.id}>{c.name}</option>)}</select></Field>
      <Field label="SKU"><input value={product.sku} onChange={e=>setProduct({...product,sku:e.target.value})} required/></Field>
      <Field label="Name"><input value={product.name} onChange={e=>setProduct({...product,name:e.target.value})} required/></Field>
      <Field label="Kannada name"><input value={product.nameKn} onChange={e=>setProduct({...product,nameKn:e.target.value})}/></Field>
      <Field label="Description"><textarea value={product.description} onChange={e=>setProduct({...product,description:e.target.value})}/></Field>
      <Field label="Image URL"><input value={product.imageUrl} onChange={e=>setProduct({...product,imageUrl:e.target.value})}/></Field>
      <Field label="Tax %"><input type="number" min="0" max="100" step=".01" value={product.taxPercent} onChange={e=>setProduct({...product,taxPercent:Number(e.target.value)})}/></Field>
      <Field label="Product loyalty limit % (blank uses category/shop)"><input type="number" min="0" max="100" value={product.loyaltyMaxPercent} onChange={e=>setProduct({...product,loyaltyMaxPercent:e.target.value})}/></Field>
      <Field label="Status"><select value={product.status} onChange={e=>setProduct({...product,status:e.target.value})}><option>DRAFT</option><option>ACTIVE</option><option>INACTIVE</option></select></Field>
      <button className={styles.primary} disabled={busy||!categories.length}>{editingProductId?"Save product":"Add product"}</button>{editingProductId&&<button type="button" className={styles.secondary} onClick={()=>{setEditingProductId(null);setProduct({...emptyProduct,categoryId:categories[0]?.id||""})}}>Cancel edit</button>}</form>
      <List title="Products">{products.map(p=><article key={p.id}><b>{p.name}</b><span>{p.sku} · {p.categoryCode} · {p.status} · Loyalty {p.loyaltyMaxPercent??"inherited"}%</span><div className={styles.actions}><button onClick={()=>editProduct(p)}>Edit</button><button onClick={()=>{editProduct({...p,status:p.status==="ACTIVE"?"INACTIVE":"ACTIVE"});}}>{p.status==="ACTIVE"?"Unpublish":"Publish"}</button></div></article>)}</List></div>}

    {tab==="import"&&<CatalogueImport onComplete={load}/>}

    {tab==="inventory"&&<div className={styles.twoColumn}><div><form className={styles.form} onSubmit={addVariant}><h2>Add priced variant</h2>
      <Field label="Product"><select value={variant.productId} onChange={e=>setVariant({...variant,productId:e.target.value})}>{products.map(p=><option value={p.id} key={p.id}>{p.name}</option>)}</select></Field>
      <Field label="Variant SKU"><input value={variant.sku} onChange={e=>setVariant({...variant,sku:e.target.value})} required/></Field>
      <Field label="Label (size/colour/type)"><input value={variant.label} onChange={e=>setVariant({...variant,label:e.target.value})} required/></Field>
      <Field label="Selling price (₹)"><input type="number" min="0" step=".01" value={variant.price} onChange={e=>setVariant({...variant,price:e.target.value})} required/></Field>
      <Field label="Opening stock"><input type="number" min="0" value={variant.stock} onChange={e=>setVariant({...variant,stock:e.target.value})}/></Field>
      <Field label="Low-stock warning at"><input type="number" min="0" value={variant.lowStock} onChange={e=>setVariant({...variant,lowStock:e.target.value})}/></Field>
      <button className={styles.primary} disabled={busy||!products.length}>Add variant</button></form>
      <form className={styles.form} onSubmit={adjustStock}><h2>Adjust stock</h2>
      <Field label="Variant"><select value={stock.variantId} onChange={e=>setStock({...stock,variantId:e.target.value})}><option value="">Select variant</option>{products.flatMap(p=>p.variants.map(v=><option value={v.id} key={v.id}>{p.name} — {v.label} ({v.stockQuantity})</option>))}</select></Field>
      <Field label="Quantity adjustment"><input type="number" value={stock.quantity} onChange={e=>setStock({...stock,quantity:e.target.value})} placeholder="Use -5 to remove five" required/></Field>
      <Field label="Reason"><input value={stock.reason} onChange={e=>setStock({...stock,reason:e.target.value})} required/></Field>
      <button className={styles.primary} disabled={busy||!stock.variantId}>Update inventory</button></form></div>
      <List title="Inventory">{products.flatMap(p=>p.variants.map(v=><article className={v.stockQuantity<=v.lowStockThreshold?styles.low:""} key={v.id}><b>{p.name} — {v.label}</b><span>{v.sku} · ₹{(v.pricePaise/100).toFixed(2)} · Stock {v.stockQuantity}</span></article>))}</List></div>}

    {tab==="orders"&&<List title="Orders">{orders.map(o=><article key={o.id}><b>{o.order_number} — {o.recipient_name}</b><span>{o.fulfilment_type} · ₹{(Number(o.total_paise)/100).toFixed(2)} · {o.status}</span><select value={o.status} onChange={e=>void orderStatus(o.id,e.target.value)} disabled={busy}>{["PLACED","CONFIRMED","PACKED","READY_FOR_PICKUP","SHIPPED","DELIVERED","CANCELLED"].map(s=><option key={s}>{s}</option>)}</select></article>)}</List>}
  </main>
}

function Field({label,children}:{label:string;children:ReactNode}){return <label className={styles.field}><span>{label}</span>{children}</label>}
function Section({title:heading,children}:{title:string;children:ReactNode}){return <fieldset className={styles.section}><legend>{heading}</legend>{children}</fieldset>}
function List({title:heading,children}:{title:string;children:ReactNode}){return <section className={styles.list}><h2>{heading}</h2>{children}</section>}
function title(value:string){return value.replace(/([A-Z])/g," $1").replace(/^./,x=>x.toUpperCase())}
function numberOrNull(value:string|number){return value===""?null:Number(value)}
function errorText(e:unknown){return e instanceof ApiError?e.message:"Unable to complete this action."}
