/** Single source of truth for Admin sidebar and dashboard navigation.
 * Every module points to an existing route/view. No backend changes are required.
 */
export type AdminArea="Overview"|"Arena"|"Finance & commerce"|"Operations & people"|"System";
export type AdminModule={
  id:string;
  label:string;
  href:string;
  area:AdminArea;
  description:string;
  featured?:boolean;
  superAdmin?:boolean;
};
export const adminAreas:AdminArea[]=["Overview","Arena","Finance & commerce","Operations & people","System"];
export const adminModules:AdminModule[]=[
 {id:"dashboard",label:"Dashboard",href:"/admin",area:"Overview",description:"Review arena activity, bookings and revenue."},
 {id:"bookings",label:"Bookings",href:"/admin/bookings",area:"Arena",description:"Manage reservations, statuses and check-in records."},
 {id:"sports",label:"Sports",href:"/admin?view=sports",area:"Arena",description:"Add and manage active sports."},
 {id:"facilities",label:"Facilities",href:"/admin?view=facilities",area:"Arena",description:"Manage courts, facilities and their schedules."},
 {id:"memberships",label:"Memberships",href:"/admin?view=memberships",area:"Arena",description:"Configure membership plans and access."},
 {id:"training",label:"Training",href:"/admin?view=training",area:"Arena",description:"Manage coaching programs and batches."},
 {id:"tournaments",label:"Tournaments",href:"/admin?view=tournaments",area:"Arena",description:"Manage tournaments and organizer requests."},
 {id:"player-growth",label:"Player Growth",href:"/admin/player-growth",area:"Arena",description:"Review player progress and training goals."},
 {id:"pricing",label:"Dynamic Pricing",href:"/admin/pricing?tab=pricing-rules",area:"Finance & commerce",description:"Set peak and off-peak prices for site, sport, court or slot.",featured:true},
 {id:"taxes",label:"Tax Management",href:"/admin/pricing?tab=taxes",area:"Finance & commerce",description:"Create, update and deactivate applicable tax rules.",featured:true},
 {id:"offers",label:"Offers & Coupons",href:"/admin?view=promotions",area:"Finance & commerce",description:"Manage discount offers, coupon codes and validity.",featured:true},
 {id:"gst",label:"GST Billing & Refunds",href:"/admin/billing",area:"Finance & commerce",description:"Merchant GST settings, invoices and credit notes.",featured:true},
 {id:"commerce",label:"Commerce",href:"/admin/commerce",area:"Finance & commerce",description:"Shop products, categories, inventory and orders.",featured:true},
 {id:"payments",label:"Payments",href:"/admin?view=payments",area:"Finance & commerce",description:"Review collected payments and transaction records."},
 {id:"subscriptions",label:"Recurring Subscriptions",href:"/admin/subscriptions",area:"Finance & commerce",description:"Review recurring-payment plans and status."},
 {id:"users",label:"Users",href:"/admin/users",area:"Operations & people",description:"Manage staff, customers and role assignments."},
 {id:"verification",label:"Verification",href:"/admin/verification",area:"Operations & people",description:"Review submitted customer verification documents."},
 {id:"operators",label:"Operator Assignments",href:"/admin/operator-assignments",area:"Operations & people",description:"Assign facility access to operators.",featured:true},
 {id:"readiness",label:"Release Readiness",href:"/admin/release-readiness",area:"Operations & people",description:"Inspect operational exceptions and release checks."},
 {id:"operations",label:"Operations",href:"/operations",area:"Operations & people",description:"Run day-to-day venue operations."},
 {id:"counter",label:"Shop Counter",href:"/shop-counter",area:"Operations & people",description:"Manage over-the-counter sales and fulfillment."},
 {id:"app-management",label:"App Management",href:"/admin/app-management",area:"System",description:"Manage Android APK link, mandatory and optional updates, app versions and permanent download QR."},
 {id:"features",label:"Feature Visibility",href:"/admin/features",area:"System",description:"Enable customer features and choose permitted user roles."},
 {id:"notifications",label:"Notifications",href:"/admin?view=notifications",area:"System",description:"Manage communication templates."},
 {id:"providers",label:"Notification Providers",href:"/admin/notification-providers",area:"System",description:"Configure email and messaging channels."},
 {id:"content",label:"Content & Events",href:"/admin?view=content",area:"System",description:"Manage website content and announcements."},
 {id:"errors",label:"Error Logs",href:"/admin/errors",area:"System",description:"Review API failures with status, timestamp and reference ID."},
 {id:"audit",label:"Audit Log",href:"/admin?view=audit",area:"System",description:"Review significant administrative activity."},
];
export const visibleAdminModules=(superAdmin:boolean)=>adminModules.filter(module=>!module.superAdmin||superAdmin);
