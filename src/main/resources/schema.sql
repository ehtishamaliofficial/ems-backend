-- =============================================================================
-- EMS Database Schema
-- =============================================================================

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id              SERIAL          PRIMARY KEY,
    name            VARCHAR(50)     UNIQUE NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ     DEFAULT NOW()
);

-- Users table (full EMS/HR profile — 30+ columns)
CREATE TABLE IF NOT EXISTS users (
    -- Identity
    id                      BIGSERIAL       PRIMARY KEY,
    erp_no                  VARCHAR(20)     UNIQUE NOT NULL,
    username                VARCHAR(50)     UNIQUE NOT NULL,
    email                   VARCHAR(100)    UNIQUE NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,

    -- Personal information
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    father_name             VARCHAR(100),
    cnic                    VARCHAR(15)     UNIQUE,
    date_of_birth           DATE,
    gender                  VARCHAR(10),
    marital_status          VARCHAR(20),
    blood_group             VARCHAR(5),
    phone_number            VARCHAR(20),
    emergency_contact_name  VARCHAR(100),
    emergency_contact_phone VARCHAR(20),

    -- Address
    permanent_address       TEXT,
    current_address         TEXT,
    city                    VARCHAR(50),
    province                VARCHAR(50),
    country                 VARCHAR(50)     DEFAULT 'Pakistan',
    postal_code             VARCHAR(10),

    -- Employment
    department              VARCHAR(100),
    designation             VARCHAR(100),
    employment_type         VARCHAR(30),
    joining_date            DATE,
    confirmation_date       DATE,
    termination_date        DATE,
    grade                   VARCHAR(10),
    salary                  NUMERIC(15, 2),
    bank_name               VARCHAR(100),
    bank_account_no         VARCHAR(30),
    profile_picture_url     VARCHAR(500),

    -- Security & Auth
    is_active               BOOLEAN         DEFAULT TRUE,
    is_email_verified       BOOLEAN         DEFAULT FALSE,
    two_factor_enabled      BOOLEAN         DEFAULT FALSE,
    two_factor_secret       VARCHAR(64),
    failed_login_attempts   INT             DEFAULT 0,
    account_locked_until    TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    last_login_ip           VARCHAR(45),
    password_changed_at     TIMESTAMPTZ,

    -- Audit
    created_at              TIMESTAMPTZ     DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     DEFAULT NOW(),
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50)
);

-- User ↔ Role many-to-many
CREATE TABLE IF NOT EXISTS user_roles (
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     INT         NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    UNIQUE NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     DEFAULT NOW(),
    revoked     BOOLEAN         DEFAULT FALSE
);

-- =============================================================================
-- Indexes for performance
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_erp_no      ON users(erp_no);
CREATE INDEX IF NOT EXISTS idx_users_username     ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email        ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_cnic         ON users(cnic);
CREATE INDEX IF NOT EXISTS idx_users_department   ON users(department);
CREATE INDEX IF NOT EXISTS idx_users_is_active    ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
