-- ========================================================
-- SNAPVEX STUDIO OS: SUPABASE POSTGRESQL SCHEMATIC
-- ========================================================

-- Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Define Custom Enum Types
CREATE TYPE system_role AS ENUM (
    'super_admin',
    'studio_admin',
    'photographer',
    'editor',
    'client'
);

CREATE TYPE lead_source AS ENUM (
    'Website',
    'WhatsApp',
    'Instagram',
    'Facebook',
    'Manual'
);

CREATE TYPE lead_status AS ENUM (
    'New Lead',
    'Follow Up',
    'Interested',
    'Quotation Sent',
    'Booked',
    'Lost'
);

CREATE TYPE studio_status AS ENUM (
    'Pending',
    'Active',
    'Suspended'
);

-- ==========================================
-- 1. STUDIOS (TENANTS) TABLE
-- ==========================================
CREATE TABLE studios (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status studio_status DEFAULT 'Pending'::studio_status,
    storage_used_mb DOUBLE PRECISION DEFAULT 0.0,
    storage_limit_mb DOUBLE PRECISION DEFAULT 51200.0, -- 50GB Default
    is_drive_approved BOOLEAN DEFAULT FALSE,
    is_drive_connected BOOLEAN DEFAULT FALSE,
    is_onedrive_connected BOOLEAN DEFAULT FALSE,
    plan_type VARCHAR(100) DEFAULT 'Free Launch Offer',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Relational indexes for search and tenant status
CREATE INDEX idx_studios_status ON studios(status);


-- ==========================================
-- 2. USERS TABLE (Supabase Auth links to auth.users)
-- ==========================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    role system_role NOT NULL DEFAULT 'client'::system_role,
    studio_id INT REFERENCES studios(id) ON DELETE SET NULL,
    full_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for lightning fast join resolutions during JWT decoding
CREATE INDEX idx_users_studio ON users(studio_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);


-- ==========================================
-- 3. LEADS (SALES CRM) TABLE
-- ==========================================
CREATE TABLE leads (
    id SERIAL PRIMARY KEY,
    studio_id INT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    source lead_source NOT NULL DEFAULT 'Manual'::lead_source,
    status lead_status NOT NULL DEFAULT 'New Lead'::lead_status,
    value NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Heavy read indexes for filtered dashboard query pipelines
CREATE INDEX idx_leads_studio ON leads(studio_id);
CREATE INDEX idx_leads_studio_status ON leads(studio_id, status);
CREATE INDEX idx_leads_source_value ON leads(source, value);


-- ==========================================
-- 4. EVENTS (SHOOT SESSIONS) TABLE
-- ==========================================
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    studio_id INT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- e.g. "Wedding", "Corporates"
    date_string VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    package_name VARCHAR(150),
    cloud_root_folder_id VARCHAR(255), -- ID of folder in Google Drive/OneDrive
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes to map events to studios and chronologies
CREATE INDEX idx_events_studio ON events(studio_id);
CREATE INDEX idx_events_studio_date ON events(studio_id, date_string);


-- ==========================================
-- 5. PAYMENTS TABLE (CRM INSTALLMENTS)
-- ==========================================
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    event_id INT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    studio_id INT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    package_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    pending_amount NUMERIC(12, 2) GENERATED ALWAYS AS (package_amount - paid_amount) STORED,
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_studio ON payments(studio_id);
CREATE INDEX idx_payments_event ON payments(event_id);


-- ========================================================
-- SUPABASE ROW-LEVEL SECURITY (RLS) POLICIES
-- ========================================================

-- Enable Row-Level Security
ALTER TABLE studios ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- 1. Helper function to retrieve requesting user's tenant ID
CREATE OR REPLACE FUNCTION get_user_studio_id()
RETURNS INT AS $$
    SELECT studio_id FROM users WHERE id = auth.uid();
$$ LANGUAGE sql SECURITY DEFINER;

-- 2. Helper function to check if the requesting user is a Super Admin
CREATE OR REPLACE FUNCTION is_super_admin()
RETURNS BOOLEAN AS $$
    SELECT role = 'super_admin'::system_role FROM users WHERE id = auth.uid();
$$ LANGUAGE sql SECURITY DEFINER;


-- STUDIOS RLS POLICIES
CREATE POLICY studio_super_admin_all
ON studios FOR ALL
USING (is_super_admin());

CREATE POLICY studio_admin_read_own
ON studios FOR SELECT
USING (id = get_user_studio_id());

CREATE POLICY studio_admin_update_own
ON studios FOR UPDATE
USING (id = get_user_studio_id() AND (SELECT role FROM users WHERE id = auth.uid()) = 'studio_admin'::system_role);


-- LEADS RLS POLICIES (CRM is local tenant isolator)
CREATE POLICY leads_tenant_isolation
ON leads FOR ALL
USING (is_super_admin() OR studio_id = get_user_studio_id());


-- EVENTS RLS POLICIES
CREATE POLICY events_tenant_isolation
ON events FOR ALL
USING (is_super_admin() OR studio_id = get_user_studio_id());


-- PAYMENTS RLS POLICIES
CREATE POLICY payments_tenant_isolation
ON payments FOR ALL
USING (is_super_admin() OR studio_id = get_user_studio_id());
