import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const webStorage = {
  get: (key: string) => typeof window === 'undefined' ? null : window.localStorage.getItem(key),
  set: (key: string, value: string) => { if (typeof window !== 'undefined') window.localStorage.setItem(key, value); },
  remove: (key: string) => { if (typeof window !== 'undefined') window.localStorage.removeItem(key); },
};

export async function getStoredValue(key: string) {
  return Platform.OS === 'web' ? webStorage.get(key) : SecureStore.getItemAsync(key);
}

export async function setStoredValue(key: string, value: string) {
  if (Platform.OS === 'web') webStorage.set(key, value);
  else await SecureStore.setItemAsync(key, value);
}

export async function removeStoredValue(key: string) {
  if (Platform.OS === 'web') webStorage.remove(key);
  else await SecureStore.deleteItemAsync(key);
}
