-- Profile photos must survive stateless Northflank container restarts and redeployments.
-- One tightly bounded image per account; data is read only by UUID used in existing public photo URLs.
CREATE TABLE user_profile_photos (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  media_type varchar(32) NOT NULL
    CHECK (media_type IN ('image/jpeg','image/png','image/webp')),
  photo_bytes bytea NOT NULL
    CHECK (octet_length(photo_bytes) BETWEEN 1 AND 5242880),
  updated_at timestamptz NOT NULL DEFAULT now()
);
