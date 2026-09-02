"use client";
import Link from "next/link";
import {ArrowRight} from "lucide-react";
import {useApp} from "@/components/app-provider";

export function BookSlotLink(){
  const {user,ready}=useApp();
  const href=ready&&user?"/dashboard?section=bookings":"/register";
  return <Link className="btn primary large" href={href}>Book a Slot <ArrowRight/></Link>;
}
