ALTER TABLE users ADD COLUMN address_line VARCHAR(250);
ALTER TABLE users ADD COLUMN city VARCHAR(100);
ALTER TABLE users ADD COLUMN state VARCHAR(100);
ALTER TABLE users ADD COLUMN postal_code VARCHAR(12);
ALTER TABLE users ADD COLUMN emergency_contact_name VARCHAR(120);
ALTER TABLE users ADD COLUMN emergency_contact_mobile VARCHAR(20);
ALTER TABLE users ADD COLUMN force_password_change BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_admin_notes(
 id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 author_user_id UUID NOT NULL REFERENCES users(id),note VARCHAR(1000) NOT NULL,created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE user_tags(
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,tag VARCHAR(50) NOT NULL,
 PRIMARY KEY(user_id,tag)
);
CREATE INDEX idx_user_notes_user_created ON user_admin_notes(user_id,created_at DESC);
