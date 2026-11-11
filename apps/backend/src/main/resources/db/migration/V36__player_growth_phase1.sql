-- Player growth phase 1: private, coach-reviewed skill assessments and goals.
CREATE TABLE player_growth_assessments (
 id UUID PRIMARY KEY, player_id UUID NOT NULL REFERENCES users(id),
 sport_id UUID NOT NULL REFERENCES sports(id), assessor_id UUID NOT NULL REFERENCES users(id),
 skill VARCHAR(100) NOT NULL, score INTEGER NOT NULL CHECK(score BETWEEN 0 AND 100),
 remarks VARCHAR(500), assessed_on DATE NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_player_growth_assessments_player ON player_growth_assessments(player_id,sport_id,assessed_on DESC);
CREATE TABLE player_growth_goals (
 id UUID PRIMARY KEY, player_id UUID NOT NULL REFERENCES users(id),
 sport_id UUID NOT NULL REFERENCES sports(id), created_by UUID NOT NULL REFERENCES users(id),
 title VARCHAR(140) NOT NULL, target_score INTEGER CHECK(target_score BETWEEN 0 AND 100),
 due_on DATE, status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ACHIEVED','CANCELLED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_player_growth_goals_player ON player_growth_goals(player_id,status);
