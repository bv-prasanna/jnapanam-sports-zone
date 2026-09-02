"use client";
import {useEffect} from "react";
import {Language,translateText} from "@/lib/i18n";

const originalText=new WeakMap<Text,string>();
const originalAttributes=new WeakMap<Element,Record<string,string>>();

function translateNode(node:Text,language:Language){
  if(node.parentElement?.closest("script,style"))return;
  let source=originalText.get(node);
  if(language==="kn"){
    const current=node.nodeValue||"";
    if(/[\u0C80-\u0CFF]/.test(current))return;
    if(source===undefined||current!==translateText("kn",source)){source=current;originalText.set(node,source)}
    const next=translateText("kn",source);
    if(current!==next)node.nodeValue=next;
  }else if(source!==undefined&&node.nodeValue!==source)node.nodeValue=source;
}
function translateAttributes(element:Element,language:Language){
  const names=["placeholder","aria-label","title"];
  let originals=originalAttributes.get(element);
  if(!originals){originals={};originalAttributes.set(element,originals)}
  for(const name of names){if(!element.hasAttribute(name))continue;const current=element.getAttribute(name)||"";let source=originals[name];if(language==="kn"){if(source===undefined||current!==translateText("kn",source)){source=current;originals[name]=source}const next=translateText("kn",source);if(current!==next)element.setAttribute(name,next)}else if(source!==undefined&&current!==source)element.setAttribute(name,source)}
}
function translateTree(root:Node,language:Language){
  if(root.nodeType===Node.TEXT_NODE){translateNode(root as Text,language);return}
  if(root.nodeType===Node.ELEMENT_NODE)translateAttributes(root as Element,language);
  const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT|NodeFilter.SHOW_ELEMENT);
  let node=walker.nextNode();while(node){if(node.nodeType===Node.TEXT_NODE)translateNode(node as Text,language);else translateAttributes(node as Element,language);node=walker.nextNode()}
}
export function DocumentLanguage({language}:{language:Language}){
  useEffect(()=>{translateTree(document.body,language);const observer=new MutationObserver(records=>{for(const record of records){if(record.type==="characterData")translateNode(record.target as Text,language);else if(record.type==="attributes")translateAttributes(record.target as Element,language);else record.addedNodes.forEach(node=>translateTree(node,language))}});observer.observe(document.body,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:["placeholder","aria-label","title"]});document.documentElement.lang=language==="kn"?"kn":"en";return()=>observer.disconnect()},[language]);
  return null;
}
