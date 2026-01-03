-- Enable extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

---
-- Table: MST_About
---
CREATE TABLE mst_about (
                           about_id BIGSERIAL PRIMARY KEY,
                           about_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                           about_desc TEXT,
                           is_active INTEGER DEFAULT 0,
                           created_at TIMESTAMPTZ DEFAULT now(),
                           created_by VARCHAR(100),
                           updated_at TIMESTAMPTZ,
                           updated_by VARCHAR(100)
);

---
-- Table: Users
---
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       user_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                       user_phone VARCHAR(20),
                       user_mail VARCHAR(100),
                       user_full_name VARCHAR(255),
                       about_id BIGINT REFERENCES mst_about(about_id),
                       is_backup_active INTEGER DEFAULT 0,
                       user_status INTEGER DEFAULT 0,
                       created_at TIMESTAMPTZ DEFAULT now(),
                       created_by VARCHAR(100),
                       updated_at TIMESTAMPTZ,
                       updated_by VARCHAR(100)
);

---
-- Table: About_User
---
CREATE TABLE about_user (
                            about_user_id BIGSERIAL PRIMARY KEY,
                            about_user_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                            user_id BIGINT REFERENCES users(user_id),
                            about_desc TEXT,
                            created_at TIMESTAMPTZ DEFAULT now(),
                            created_by VARCHAR(100),
                            updated_at TIMESTAMPTZ,
                            updated_by VARCHAR(100)
);

---
-- Table: User_OTP
---
CREATE TABLE user_otp (
                          otp_id BIGSERIAL PRIMARY KEY,
                          otp_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                          otp_code VARCHAR(10),
                          exp_at TIMESTAMPTZ,
                          otp_status INTEGER DEFAULT 0,
                          created_at TIMESTAMPTZ DEFAULT now(),
                          created_by VARCHAR(100),
                          updated_at TIMESTAMPTZ,
                          updated_by VARCHAR(100)
);

---
-- Table: Group
---
CREATE TABLE "group" ( -- "Group" is a reserved keyword in SQL
                         group_id BIGSERIAL PRIMARY KEY,
                         group_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                         group_name VARCHAR(255),
                         group_desc TEXT,
                         is_active INTEGER DEFAULT 0,
                         created_at TIMESTAMPTZ DEFAULT now(),
                         created_by VARCHAR(100),
                         updated_at TIMESTAMPTZ,
                         updated_by VARCHAR(100)
);

---
-- Table: chat_user
---
CREATE TABLE chat_user (
                           chat_user_id BIGSERIAL PRIMARY KEY,
                           chat_user_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                           chat_id BIGINT,
                           chat_to_user_id BIGINT REFERENCES users(user_id),
                           chat_to_group_id BIGINT REFERENCES "group"(group_id),
                           chat_type VARCHAR(50),
                           is_active INTEGER DEFAULT 0,
                           created_at TIMESTAMPTZ DEFAULT now(),
                           created_by VARCHAR(100),
                           updated_at TIMESTAMPTZ,
                           updated_by VARCHAR(100)
);

---
-- Table: Group_Member
---
CREATE TABLE group_member (
                              group_member_id BIGSERIAL PRIMARY KEY,
                              group_member_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                              group_id BIGINT REFERENCES "group"(group_id),
                              user_id BIGINT REFERENCES users(user_id),
                              member_type VARCHAR(50),
                              is_active INTEGER DEFAULT 0,
                              created_at TIMESTAMPTZ DEFAULT now(),
                              created_by VARCHAR(100),
                              updated_at TIMESTAMPTZ,
                              updated_by VARCHAR(100)
);

---
-- Table: Attachment
---
CREATE TABLE attachment (
                            attachment_id BIGSERIAL PRIMARY KEY,
                            attachment_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                            attachment_type VARCHAR(50),
                            attachment_source TEXT,
                            is_active INTEGER DEFAULT 0,
                            created_at TIMESTAMPTZ DEFAULT now(),
                            created_by VARCHAR(100),
                            updated_at TIMESTAMPTZ,
                            updated_by VARCHAR(100)
);

---
-- Table: chat_Detail
---
CREATE TABLE chat_detail (
                             user_chat_detail_id BIGSERIAL PRIMARY KEY,
                             user_chat_detail_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
                             chat_id BIGINT,
                             chat_desc TEXT,
                             chat_status INTEGER DEFAULT 0,
                             chat_attachment_id BIGINT REFERENCES attachment(attachment_id),
                             group_id BIGINT REFERENCES "group"(group_id),
                             is_favorite INTEGER DEFAULT 0,
                             created_at TIMESTAMPTZ DEFAULT now(),
                             created_by VARCHAR(100),
                             updated_at TIMESTAMPTZ,
                             updated_by VARCHAR(100)
);