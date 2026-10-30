import * as Location from 'expo-location';

export type EventLocationHeaders = Record<string, string>;
let cached: {latitude:number; longitude:number; time:number}|null = null;

/** Never prompts during an API call; consent is requested explicitly from Profile. */
export async function getEventLocationHeaders(): Promise<EventLocationHeaders> {
  try {
    const permission = await Location.getForegroundPermissionsAsync();
    if (!permission.granted) {
      return {'X-Location-Status': permission.status === 'denied' ? 'DENIED' : 'NOT_REQUESTED'};
    }
    const servicesEnabled = await Location.hasServicesEnabledAsync();
    if (!servicesEnabled) return {'X-Location-Status': 'UNAVAILABLE'};
    let coords = cached;
    if (!coords || Date.now() - coords.time > 60000) {
      const known = await Location.getLastKnownPositionAsync({maxAge:60000,requiredAccuracy:1000});
      if (known) {
        coords = {latitude:known.coords.latitude,longitude:known.coords.longitude,time:known.timestamp};
      } else {
        const fresh = await Promise.race([
          Location.getCurrentPositionAsync({accuracy:Location.Accuracy.Balanced}),
          new Promise<null>(resolve => setTimeout(()=>resolve(null),2500)),
        ]);
        if (!fresh) return {'X-Location-Status':'UNAVAILABLE'};
        coords = {latitude:fresh.coords.latitude,longitude:fresh.coords.longitude,time:fresh.timestamp};
      }
      cached = coords;
    }
    if (!Number.isFinite(coords.latitude)||!Number.isFinite(coords.longitude)
      ||Math.abs(coords.latitude)>90||Math.abs(coords.longitude)>180)
       return {'X-Location-Status':'INVALID'};
    return {
      'X-Client-Latitude': coords.latitude.toFixed(7),
      'X-Client-Longitude': coords.longitude.toFixed(7),
      'X-Location-Status':'CAPTURED'
    };
  } catch {
    return {'X-Location-Status':'UNAVAILABLE'};
  }
}

export async function enableEventLocation(): Promise<'CAPTURED'|'DENIED'|'UNAVAILABLE'> {
  try {
    const result = await Location.requestForegroundPermissionsAsync();
    if (!result.granted) return 'DENIED';
    const enabled = await Location.hasServicesEnabledAsync();
    return enabled ? 'CAPTURED' : 'UNAVAILABLE';
  } catch { return 'UNAVAILABLE'; }
}
