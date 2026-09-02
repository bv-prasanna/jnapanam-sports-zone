export type Language = "en" | "kn";
const words = {
  en: { home:"Home", sports:"Sports & Facilities", memberships:"Memberships", events:"Events", contact:"Contact", login:"Login", register:"Register Now", dashboard:"My Dashboard", logout:"Logout", welcome:"Welcome", bookings:"Bookings", training:"Training", tournaments:"Tournaments", payments:"Payments", loyalty:"Loyalty", notifications:"Notifications", profile:"Profile", admin:"Admin", loading:"Loading…", retry:"Retry", noData:"Nothing to show yet", bookNow:"Book a slot", payNow:"Pay now", payVenue:"Pay at venue" },
  kn: { home:"ಮುಖಪುಟ", sports:"ಕ್ರೀಡೆ ಮತ್ತು ಸೌಲಭ್ಯಗಳು", memberships:"ಸದಸ್ಯತ್ವಗಳು", events:"ಕಾರ್ಯಕ್ರಮಗಳು", contact:"ಸಂಪರ್ಕ", login:"ಲಾಗಿನ್", register:"ನೋಂದಣಿ", dashboard:"ನನ್ನ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್", logout:"ಲಾಗ್ ಔಟ್", welcome:"ಸ್ವಾಗತ", bookings:"ಬುಕಿಂಗ್‌ಗಳು", training:"ತರಬೇತಿ", tournaments:"ಪಂದ್ಯಾವಳಿಗಳು", payments:"ಪಾವತಿಗಳು", loyalty:"ಲಾಯಲ್ಟಿ", notifications:"ಅಧಿಸೂಚನೆಗಳು", profile:"ಪ್ರೊಫೈಲ್", admin:"ನಿರ್ವಹಣೆ", loading:"ಲೋಡ್ ಆಗುತ್ತಿದೆ…", retry:"ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ", noData:"ಇನ್ನೂ ಯಾವುದೇ ಮಾಹಿತಿ ಇಲ್ಲ", bookNow:"ಸ್ಲಾಟ್ ಬುಕ್ ಮಾಡಿ", payNow:"ಈಗ ಪಾವತಿಸಿ", payVenue:"ಕೇಂದ್ರದಲ್ಲಿ ಪಾವತಿಸಿ" },
} as const;
export type TranslationKey = keyof typeof words.en;
export const translate = (language: Language, key: TranslationKey) => words[language][key];
