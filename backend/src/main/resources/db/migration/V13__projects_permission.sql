-- V13: projects:manage permission for the Admin Projects management APIs
-- (A6).
--
-- Why a new permission: no V2 code covers the projects domain (V5 tables:
-- project_categories, projects, project_images). Reusing content:manage
-- would blur website-content semantics and would grant EDITOR write access
-- to the project portfolio, which the seed never intended. projects:manage
-- is additive only — existing role grants and semantics are unchanged.
--
-- Granted to SUPER_ADMIN and ADMIN only. MODERATOR and normal platform roles
-- deliberately do NOT receive it. Idempotent-safe by construction (guarding
-- WHERE NOT EXISTS); the unique constraint uq_permissions_code backstops the
-- insert.

INSERT INTO permissions (code, description)
SELECT 'projects:manage', 'Manage project categories, projects and project image metadata.'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'projects:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'projects:manage'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);
