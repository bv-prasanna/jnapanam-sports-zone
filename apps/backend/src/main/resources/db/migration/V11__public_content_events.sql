CREATE TABLE public_events(
 id UUID PRIMARY KEY,slug VARCHAR(100) NOT NULL UNIQUE,title VARCHAR(180) NOT NULL,description TEXT NOT NULL,
 event_type VARCHAR(30) NOT NULL,sport_id UUID REFERENCES sports(id),facility_id UUID REFERENCES facilities(id),
 starts_at TIMESTAMPTZ NOT NULL,ends_at TIMESTAMPTZ NOT NULL,registration_url VARCHAR(500),image_url VARCHAR(500),
 capacity INTEGER,status VARCHAR(20) NOT NULL,featured BOOLEAN NOT NULL,display_order INTEGER NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE announcements(
 id UUID PRIMARY KEY,title VARCHAR(180) NOT NULL,body TEXT NOT NULL,priority INTEGER NOT NULL,
 publish_from TIMESTAMPTZ NOT NULL,publish_until TIMESTAMPTZ,status VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE gallery_items(
 id UUID PRIMARY KEY,title VARCHAR(180) NOT NULL,media_type VARCHAR(20) NOT NULL,media_url VARCHAR(500) NOT NULL,
 thumbnail_url VARCHAR(500),alt_text VARCHAR(250),sport_id UUID REFERENCES sports(id),published BOOLEAN NOT NULL,
 display_order INTEGER NOT NULL,created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE content_pages(
 id UUID PRIMARY KEY,slug VARCHAR(80) NOT NULL UNIQUE,title VARCHAR(180) NOT NULL,summary VARCHAR(500),
 body TEXT NOT NULL,status VARCHAR(20) NOT NULL,updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE contact_inquiries(
 id UUID PRIMARY KEY,name VARCHAR(120) NOT NULL,email VARCHAR(180) NOT NULL,mobile VARCHAR(20),
 subject VARCHAR(180) NOT NULL,message TEXT NOT NULL,status VARCHAR(20) NOT NULL,admin_note VARCHAR(500),
 created_at TIMESTAMPTZ NOT NULL,resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_public_events_status_start ON public_events(status,starts_at);
CREATE INDEX idx_announcements_publish ON announcements(status,publish_from,publish_until);
CREATE INDEX idx_gallery_published_order ON gallery_items(published,display_order);
CREATE INDEX idx_contact_status_created ON contact_inquiries(status,created_at DESC);

INSERT INTO content_pages(id,slug,title,summary,body,status,updated_at) VALUES
(gen_random_uuid(),'about-us','About Jnapanam Sports Zone','Play, train, compete and grow.','Jnapanam Sports Zone makes quality sports, fitness and coaching accessible to families in Holalkere.','PUBLISHED',now()),
(gen_random_uuid(),'services','Our Services','Sports, memberships, coaching and competitions.','Explore facility bookings, memberships, training camps, tournaments and community sporting activities.','PUBLISHED',now());
