-- V12: services:manage permission for the Services & Expertise management
-- APIs (A5).
--
-- Why a new permission: no V2 code covers the services catalog (V4 tables:
-- service_categories, services, expertise_areas). Reusing content:manage
-- would blur website-content semantics and would grant EDITOR write access
-- to the commercial services catalog, which the seed never intended.
-- services:manage is additive only — existing role grants and semantics are
-- unchanged.
--
-- Granted to SUPER_ADMIN and ADMIN only. MODERATOR and normal platform roles
-- deliberately do NOT receive it. Idempotent-safe by construction (guarding
-- WHERE NOT EXISTS); the unique constraint uq_permissions_code backstops the
-- insert.

INSERT INTO permissions (code, description)
SELECT 'services:manage', 'Manage service categories, services and expertise areas.'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'services:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'services:manage'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);
