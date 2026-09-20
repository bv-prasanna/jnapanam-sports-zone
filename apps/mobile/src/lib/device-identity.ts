import * as Device from 'expo-device';import * as SecureStore from 'expo-secure-store';
const KEY='jnapanam.deviceSignature.v1';
export async function getDeviceIdentity(){let signature=await SecureStore.getItemAsync(KEY);if(!signature){signature=globalThis.crypto?.randomUUID?.()||('jn-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2)+'-'+Math.random().toString(36).slice(2));await SecureStore.setItemAsync(KEY,signature);}return {deviceSignature:signature,deviceName:[Device.manufacturer,Device.modelName].filter(Boolean).join(' ')||'Mobile device'};}
