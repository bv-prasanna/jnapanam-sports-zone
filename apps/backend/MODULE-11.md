# Module 11 — Customer, User and Role Management

## Included

- Paginated customer/staff directory with text, status and role filters
- Admin-created users with temporary passwords and forced-change status
- Editable contact, address and emergency-contact data
- ACTIVE, PENDING_APPROVAL, SUSPENDED and DISABLED lifecycle management
- Refresh-token revocation after suspension, role change or password reset
- Super-admin-controlled role replacement
- Protection against removing the final super administrator
- Admin password reset and authenticated customer password change
- Customer tags and private administrative notes
- Consolidated booking, check-in, membership, training, tournament and payment summary
- Duplicate email/mobile protection
- Existing audit layer covers every mutating user-management API
- Full user/role/password/lifecycle regression automation

## APIs

- `GET/POST /api/v1/admin/users`
- `GET/PUT /api/v1/admin/users/{id}`
- `PATCH /api/v1/admin/users/{id}/status`
- `PUT /api/v1/admin/users/{id}/roles` — super admin only
- `POST /api/v1/admin/users/{id}/password-reset`
- `POST /api/v1/admin/users/{id}/notes`
- `PUT /api/v1/admin/users/{id}/tags`
- `GET /api/v1/admin/users/roles/available`
- `POST /api/v1/profile/password`

Role assignment must use these APIs in production; direct database role promotion remains development-only.
