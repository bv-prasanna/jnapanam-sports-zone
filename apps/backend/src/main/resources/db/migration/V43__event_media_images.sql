-- Small, durable event images. Stored in PostgreSQL because free Northflank container disks are ephemeral.
CREATE TABLE event_media_images (
  id uuid PRIMARY KEY,
  data bytea NOT NULL,
  content_type varchar(32) NOT NULL,
  byte_size integer NOT NULL CHECK (byte_size > 0 AND byte_size <= 2097152),
  created_at timestamptz NOT NULL DEFAULT now()
);
