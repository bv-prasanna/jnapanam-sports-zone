import { Redirect } from 'expo-router';
/** Business configuration is web-only; legacy Android admin links return to the customer home. */
export default function AdminWebOnly(){return <Redirect href="/"/>;}
