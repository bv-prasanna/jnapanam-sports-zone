# Jnapanam Sports Zone — Public Website

Responsive public website for Jnapanam Sports Zone, Holalkere.

## Included pages

- Home
- About Us
- Sports & Facilities
- Services
- Memberships
- Events
- Gallery
- Contact
- Login placeholder
- Registration placeholder

## Requirements

- Node.js 22.13 or newer
- npm

## Windows setup

1. Extract the ZIP to `C:\Projects\jnapanam-sports-zone-web`.
2. Open PowerShell in that folder.
3. Install packages:

   ```powershell
   npm install
   ```

4. Start the website:

   ```powershell
   npm run dev
   ```

5. Open the local address shown in PowerShell, normally `http://localhost:3000` or `http://localhost:5173`.

## Production check

```powershell
npm run build
npm start
```

## Hero video

The website already contains a cinematic fallback image. To use a real promotional video, add an optimized MP4 at:

```text
public/videos/jnapanam-hero.mp4
```

Recommended: 1920×1080, 10–20 seconds, muted, looping, H.264 MP4, ideally under 8 MB.

## Important placeholders

- Confirm the final phone number, email and address.
- Replace sample events with confirmed event data.
- Login, registration and form submission are visual placeholders until the backend phase.
- Kannada language switching will be connected during the localization phase.
