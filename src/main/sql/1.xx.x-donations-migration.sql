-- Donation webhook schema
-- Supports Ko-fi and Patreon donations with currency conversion

CREATE TABLE IF NOT EXISTS donation (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    donor_name TEXT,
    original_amount INTEGER NOT NULL,
    original_currency VARCHAR(3) NOT NULL,
    usd_amount INTEGER NOT NULL
);

-- Create index on created timestamp for efficient querying
CREATE INDEX IF NOT EXISTS idx_donation_created ON donation(created DESC);

-- Create index on donor name for reporting
CREATE INDEX IF NOT EXISTS idx_donation_donor_name ON donation(donor_name);
