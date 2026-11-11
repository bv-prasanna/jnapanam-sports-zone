"use client";
import Link from "next/link";
import {adminAreas,visibleAdminModules} from "./admin-navigation";
import styles from "./admin-dashboard-links.module.css";

export function AdminDashboardLinks({superAdmin}:{superAdmin:boolean}){
  const modules=visibleAdminModules(superAdmin).filter(module=>module.id!=="dashboard");
  const featured=modules.filter(module=>module.featured);
  return <section className={styles.hub} aria-label="Admin module shortcuts">
    <div className={styles.heading}><div><span className={styles.eyebrow}>QUICK ACCESS</span><h2>Manage Jnapanam</h2><p>Open pricing, taxes, offers, staffing and billing directly from your dashboard.</p></div></div>
    <div className={styles.featured}>
      {featured.map(module=><Link href={module.href} className={styles.featuredCard} key={module.id}>
        <strong>{module.label}</strong><span>{module.description}</span>
        <span className={styles.open} aria-hidden="true">Open module →</span>
      </Link>)}
    </div>
    <div className={styles.heading}><div><h2>All Admin modules</h2><p>Every available management screen is listed below and in the shared sidebar.</p></div></div>
    <div className={styles.categories}>
      {adminAreas.filter(area=>area!=="Overview").map(area=>{
        const items=modules.filter(module=>module.area===area&&!module.featured);
        return items.length?<section className={styles.category} key={area} aria-label={area}>
          <h3>{area}</h3>
          <div className={styles.moduleLinks}>
            {items.map(module=><Link href={module.href} className={styles.moduleLink} key={module.id}>
              <span><strong>{module.label}</strong><small>{module.description}</small></span>
              <span aria-hidden="true">→</span>
            </Link>)}
          </div>
        </section>:null;
      })}
    </div>
  </section>;
}
