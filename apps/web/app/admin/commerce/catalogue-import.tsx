"use client";

import { ChangeEvent, useMemo, useState } from "react";
import { ApiError, api } from "@/lib/api";
import styles from "./catalogue-import.module.css";

type Category={id:string;code:string;name:string;nameKn?:string;description?:string;imageUrl?:string;displayOrder:number;status:string;loyaltyMaxPercent?:number};
type Variant={id:string;sku:string;label:string;pricePaise:number;stockQuantity:number;lowStockThreshold:number;status:string};
type Product={id:string;categoryId:string;sku:string;name:string;nameKn?:string;description?:string;descriptionKn?:string;imageUrl?:string;taxPercent:number;status:string;loyaltyMaxPercent?:number;variants:Variant[]};
type Row=Record<string,string>;
const columns=["category_code","category_name","category_name_kn","category_status","product_sku","product_name","product_name_kn","description","description_kn","image_url","tax_percent","product_status","loyalty_max_percent","variant_sku","variant_label","price","stock_quantity","low_stock_threshold","variant_status"];

export function CatalogueImport({onComplete}:{onComplete:()=>Promise<void>}){
 const[rows,setRows]=useState<Row[]>([]);const[errors,setErrors]=useState<string[]>([]);const[busy,setBusy]=useState(false);const[message,setMessage]=useState("");
 const validation=useMemo(()=>rows.flatMap((r,i)=>validate(r,i+2)),[rows]);
 async function selectFile(e:ChangeEvent<HTMLInputElement>){const file=e.target.files?.[0];if(!file)return;const parsed=parseCsv(await file.text());setRows(parsed);setErrors([]);setMessage(parsed.length?`${parsed.length} row(s) ready for validation.`:"The CSV contains no data rows.")}
 function download(){const sample=[columns.join(","),"CRICKET,Cricket,,ACTIVE,BAT-001,English Willow Bat,,Professional bat,,,18,ACTIVE,30,BAT-001-SR,Standard,4500,10,5,ACTIVE"].join("\n");const url=URL.createObjectURL(new Blob([sample],{type:"text/csv"}));const a=document.createElement("a");a.href=url;a.download="jnapanam-catalogue-template.csv";a.click();URL.revokeObjectURL(url)}
 async function run(){if(validation.length){setErrors(validation);return}setBusy(true);setErrors([]);setMessage("");try{
   const categories=await api<Category[]>("/api/v1/admin/commerce/categories");
   const products=await api<Product[]>("/api/v1/admin/commerce/products");
   const categoryMap=new Map(categories.map(c=>[c.code.toUpperCase(),c]));
   const productMap=new Map(products.map(p=>[p.sku.toUpperCase(),p]));
   for(const row of rows){
     const categoryBody={code:row.category_code,name:row.category_name,nameKn:blank(row.category_name_kn),description:null,imageUrl:null,displayOrder:0,loyaltyMaxPercent:null,status:row.category_status||"ACTIVE"};
     let category=categoryMap.get(row.category_code.toUpperCase());
     if(category){await api("/api/v1/admin/commerce/categories/"+category.id,{method:"PUT",body:JSON.stringify(categoryBody)})}
     else{const made=await api<{id:string}>("/api/v1/admin/commerce/categories",{method:"POST",body:JSON.stringify(categoryBody)});category={...categoryBody,id:made.id,nameKn:categoryBody.nameKn||undefined,description:undefined,imageUrl:undefined,loyaltyMaxPercent:undefined};categoryMap.set(row.category_code.toUpperCase(),category)}
     const productBody={categoryId:category.id,sku:row.product_sku,name:row.product_name,nameKn:blank(row.product_name_kn),description:blank(row.description),descriptionKn:blank(row.description_kn),imageUrl:blank(row.image_url),taxPercent:num(row.tax_percent),status:row.product_status||"ACTIVE",loyaltyMaxPercent:nullableNum(row.loyalty_max_percent)};
     let product=productMap.get(row.product_sku.toUpperCase());
     if(product){await api("/api/v1/admin/commerce/products/"+product.id,{method:"PUT",body:JSON.stringify(productBody)})}
     else{const made=await api<{id:string}>("/api/v1/admin/commerce/products",{method:"POST",body:JSON.stringify(productBody)});product={...productBody,id:made.id,variants:[],nameKn:productBody.nameKn||undefined,description:productBody.description||undefined,descriptionKn:productBody.descriptionKn||undefined,imageUrl:productBody.imageUrl||undefined,loyaltyMaxPercent:productBody.loyaltyMaxPercent??undefined};productMap.set(row.product_sku.toUpperCase(),product)}
     const variantBody={sku:row.variant_sku,label:row.variant_label||"Standard",pricePaise:Math.round(num(row.price)*100),stockQuantity:Math.trunc(num(row.stock_quantity)),lowStockThreshold:Math.trunc(num(row.low_stock_threshold)||5),status:row.variant_status||"ACTIVE"};
     const existing=product.variants.find(v=>v.sku.toUpperCase()===row.variant_sku.toUpperCase());
     if(existing)await api("/api/v1/admin/commerce/variants/"+existing.id,{method:"PUT",body:JSON.stringify(variantBody)});
     else await api("/api/v1/admin/commerce/products/"+product.id+"/variants",{method:"POST",body:JSON.stringify(variantBody)});
   }
   setMessage(`Successfully imported ${rows.length} catalogue row(s).`);setRows([]);await onComplete();
 }catch(e){setErrors([e instanceof ApiError?e.message:"Catalogue import failed. No further rows were processed."])}finally{setBusy(false)}}
 return <section className={styles.panel}><div className={styles.heading}><div><h2>Bulk catalogue import</h2><p>Upload categories, products, pricing, inventory and loyalty rules in one CSV.</p></div><button type="button" onClick={download}>Download template</button></div>
   <label className={styles.upload}>Choose CSV file<input type="file" accept=".csv,text/csv" onChange={e=>void selectFile(e)}/></label>
   {!!message&&<p className={styles.message}>{message}</p>}
   {!!validation.length&&<div className={styles.errors}><b>Fix these errors before import:</b>{validation.slice(0,20).map(x=><span key={x}>{x}</span>)}</div>}
   {!!errors.length&&<div className={styles.errors}>{errors.map(x=><span key={x}>{x}</span>)}</div>}
   {!!rows.length&&<><div className={styles.preview}><table><thead><tr><th>Category</th><th>Product</th><th>Variant</th><th>Price</th><th>Stock</th><th>Status</th></tr></thead><tbody>{rows.slice(0,10).map((r,i)=><tr key={i}><td>{r.category_code}</td><td>{r.product_name}</td><td>{r.variant_sku}</td><td>₹{r.price}</td><td>{r.stock_quantity}</td><td>{r.product_status||"ACTIVE"}</td></tr>)}</tbody></table></div><button className={styles.import} disabled={busy||!!validation.length} onClick={()=>void run()}>{busy?"Importing…":`Import ${rows.length} rows`}</button></>}
 </section>
}
function validate(r:Row,row:number){const e:string[]=[];for(const key of ["category_code","category_name","product_sku","product_name","variant_sku","price","stock_quantity"])if(!r[key])e.push(`Row ${row}: ${key} is required.`);if(r.price&&(!Number.isFinite(Number(r.price))||Number(r.price)<0))e.push(`Row ${row}: price must be zero or greater.`);if(r.stock_quantity&&(!Number.isInteger(Number(r.stock_quantity))||Number(r.stock_quantity)<0))e.push(`Row ${row}: stock_quantity must be a whole number zero or greater.`);return e}
function parseCsv(text:string){const matrix:string[][]=[];let row:string[]=[],cell="",quoted=false;for(let i=0;i<text.length;i++){const ch=text[i];if(ch==='"'&&quoted&&text[i+1]==='"'){cell+='"';i++}else if(ch==='"')quoted=!quoted;else if(ch===","&&!quoted){row.push(cell.trim());cell=""}else if((ch==="\n"||ch==="\r")&&!quoted){if(ch==="\r"&&text[i+1]==="\n")i++;row.push(cell.trim());if(row.some(Boolean))matrix.push(row);row=[];cell=""}else cell+=ch}row.push(cell.trim());if(row.some(Boolean))matrix.push(row);if(matrix.length<2)return[];const headers=matrix[0].map(x=>x.toLowerCase());return matrix.slice(1).map(values=>Object.fromEntries(headers.map((h,i)=>[h,values[i]||""])))}
function blank(v?:string){return v?.trim()||null}function num(v?:string){return Number(v||0)}function nullableNum(v?:string){return v?.trim()?Number(v):null}
