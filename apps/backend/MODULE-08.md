# Module 08 — Tournaments and Competitions

## Included

- Customer organizer applications with admin approval/rejection
- Configurable tournaments by sport, format, dates, team size, capacity and entry fee
- Tournament publication and lifecycle statuses
- Team registration with roster validation, duplicate prevention and capacity control
- Customer team history and admin participant lists
- Single-elimination and league first-round fixture generation
- Fixture scheduling against configured facilities
- Score and winner recording with public schedules/results
- Tournament entry-fee support through the existing payment gateway (`TOURNAMENT` purpose)
- Full lifecycle regression automation

## APIs

- `GET /api/v1/public/tournaments`
- `GET /api/v1/public/tournaments/{id}/fixtures`
- `POST /api/v1/tournaments/organizer-applications`
- `POST /api/v1/tournaments/{id}/teams`
- `GET /api/v1/tournaments/teams/me`
- `GET/POST /api/v1/admin/tournaments`
- `PATCH /api/v1/admin/tournaments/{id}/status`
- `GET /api/v1/admin/tournaments/{id}/teams`
- `POST /api/v1/admin/tournaments/{id}/fixtures/generate`
- `PATCH /api/v1/admin/tournaments/fixtures/{id}/schedule`
- `PATCH /api/v1/admin/tournaments/fixtures/{id}/result`
- `GET/PATCH /api/v1/admin/tournaments/organizer-applications/**`
