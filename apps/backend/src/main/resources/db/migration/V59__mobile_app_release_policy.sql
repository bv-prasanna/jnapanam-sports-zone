-- Safe default: no APK URL configured and no version enforcement until an administrator publishes one.
CREATE TABLE mobile_app_release_policy (
 id SMALLINT PRIMARY KEY CHECK (id = 1),
 latest_version_code INTEGER NOT NULL DEFAULT 1 CHECK (latest_version_code > 0),
 latest_version_name VARCHAR(40) NOT NULL DEFAULT '1.0.0',
 minimum_supported_version_code INTEGER NOT NULL DEFAULT 1 CHECK (minimum_supported_version_code > 0),
 update_mode VARCHAR(12) NOT NULL DEFAULT 'OFF' CHECK (update_mode IN ('OFF','OPTIONAL','MANDATORY')),
 apk_url TEXT,
 release_notes VARCHAR(1500),
 updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT chk_android_version_order CHECK(minimum_supported_version_code <= latest_version_code),
 CONSTRAINT chk_enabled_has_url CHECK(update_mode='OFF' OR apk_url IS NOT NULL)
);
INSERT INTO mobile_app_release_policy(id) VALUES(1);
