# Module 10 — Public Content, Events and Enquiries

## Included

- Backend-managed upcoming events with sport/facility mapping, dates, images, registration links and featured ordering
- Scheduled public announcements with priority
- Image/video gallery metadata and accessibility alt text
- Editable website pages such as About Us and Services
- Public contact enquiry submission and admin resolution workflow
- Draft/published/archive lifecycle controls
- Public APIs requiring no login and protected admin content APIs
- Regression coverage for the complete publish-and-enquiry flow

## Public APIs

- `GET /api/v1/public/content/events`
- `GET /api/v1/public/content/announcements`
- `GET /api/v1/public/content/gallery`
- `GET /api/v1/public/content/pages/{slug}`
- `POST /api/v1/public/content/contact`

## Admin APIs

- `/api/v1/admin/content/events/**`
- `/api/v1/admin/content/announcements/**`
- `/api/v1/admin/content/gallery/**`
- `/api/v1/admin/content/pages/**`
- `/api/v1/admin/content/inquiries/**`

Media files remain on object/CDN storage; the database stores their URLs and metadata.
