-- V11: dashboard:view permission for the Admin Dashboard read APIs (A2).
--
-- Why a new permission: the dashboard aggregates platform-wide data (users,
-- jobs, applications, enquiries). Reusing users:manage would wrongly lock
-- ADMIN out of its own landing view; messages:manage / settings:manage carry
-- the wrong semantics. dashboard:view is a pure read authorization, additive
-- only — existing role grants and semantics are unchanged.
--
-- Granted to SUPER_ADMIN and ADMIN only. MODERATOR and normal platform roles
-- deliberately do NOT receive it. Idempotent-safe by construction (guarding
-- WHERE NOT EXISTS); the unique constraint uq_permissions_code backstops the
-- insert.

INSERT INTO permissions (code, description)
SELECT 'dashboard:view', 'View the administrative dashboard summary and recent activity.'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'dashboard:view');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'dashboard:view'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);
