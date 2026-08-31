CREATE TABLE organizer_applications(
 id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES users(id),organization_name VARCHAR(160) NOT NULL,
 experience TEXT,contact_number VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL,
 review_note VARCHAR(300),applied_at TIMESTAMPTZ NOT NULL,reviewed_at TIMESTAMPTZ,
 CONSTRAINT uq_open_organizer_application UNIQUE(user_id,status)
);
CREATE TABLE tournaments(
 id UUID PRIMARY KEY,code VARCHAR(40) NOT NULL UNIQUE,name VARCHAR(160) NOT NULL,description TEXT,
 sport_id UUID NOT NULL REFERENCES sports(id),organizer_user_id UUID NOT NULL REFERENCES users(id),
 format VARCHAR(30) NOT NULL,status VARCHAR(30) NOT NULL,registration_opens DATE NOT NULL,
 registration_closes DATE NOT NULL,start_date DATE NOT NULL,end_date DATE NOT NULL,
 team_size INTEGER NOT NULL,max_teams INTEGER NOT NULL,entry_fee NUMERIC(12,2) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE tournament_teams(
 id UUID PRIMARY KEY,tournament_id UUID NOT NULL REFERENCES tournaments(id),captain_user_id UUID NOT NULL REFERENCES users(id),
 name VARCHAR(120) NOT NULL,status VARCHAR(20) NOT NULL,registered_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_tournament_team_name UNIQUE(tournament_id,name),CONSTRAINT uq_tournament_captain UNIQUE(tournament_id,captain_user_id)
);
CREATE TABLE tournament_team_members(
 id UUID PRIMARY KEY,team_id UUID NOT NULL REFERENCES tournament_teams(id) ON DELETE CASCADE,
 name VARCHAR(120) NOT NULL,email VARCHAR(180),mobile VARCHAR(20),created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE tournament_fixtures(
 id UUID PRIMARY KEY,tournament_id UUID NOT NULL REFERENCES tournaments(id),round_number INTEGER NOT NULL,
 match_number INTEGER NOT NULL,home_team_id UUID NOT NULL REFERENCES tournament_teams(id),away_team_id UUID REFERENCES tournament_teams(id),
 facility_id UUID REFERENCES facilities(id),scheduled_at TIMESTAMPTZ,home_score INTEGER,away_score INTEGER,
 winner_team_id UUID REFERENCES tournament_teams(id),status VARCHAR(20) NOT NULL,updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_tournament_match UNIQUE(tournament_id,round_number,match_number)
);
CREATE INDEX idx_tournament_status_dates ON tournaments(status,start_date);
CREATE INDEX idx_team_tournament ON tournament_teams(tournament_id,registered_at);
CREATE INDEX idx_fixture_tournament ON tournament_fixtures(tournament_id,round_number,match_number);
