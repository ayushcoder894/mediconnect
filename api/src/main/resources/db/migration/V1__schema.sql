-- MediConnect core schema (MySQL 8)

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospitals (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(160) NOT NULL,
    city    VARCHAR(80)  NOT NULL,
    address VARCHAR(255),
    phone   VARCHAR(20),
    INDEX idx_hospitals_city (city)
);

CREATE TABLE patients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    date_of_birth   DATE,
    gender          VARCHAR(10),
    blood_group     VARCHAR(5),
    height_cm       DECIMAL(5,1),
    weight_kg       DECIMAL(5,1),
    address         VARCHAR(255),
    medical_history TEXT,
    allergies       VARCHAR(500),
    smoking_history VARCHAR(20),
    hypertension    BOOLEAN NOT NULL DEFAULT FALSE,
    heart_disease   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE doctors (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT        NOT NULL UNIQUE,
    hospital_id      BIGINT,
    specialization   VARCHAR(80)   NOT NULL,
    qualification    VARCHAR(160),
    experience_years INT           NOT NULL DEFAULT 0,
    consultation_fee DECIMAL(10,2) NOT NULL,
    bio              TEXT,
    slot_minutes     INT           NOT NULL DEFAULT 30,
    approved         BOOLEAN       NOT NULL DEFAULT FALSE,
    rating_avg       DECIMAL(3,2)  NOT NULL DEFAULT 0,
    rating_count     INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_doctors_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    INDEX idx_doctors_specialization (specialization)
);

CREATE TABLE doctor_availability (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id   BIGINT      NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    CONSTRAINT fk_availability_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT uq_availability_day UNIQUE (doctor_id, day_of_week)
);

CREATE TABLE appointments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id     BIGINT        NOT NULL,
    patient_id    BIGINT        NOT NULL,
    slot_start    DATETIME      NOT NULL,
    slot_end      DATETIME      NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    reason        VARCHAR(500),
    fee           DECIMAL(10,2) NOT NULL,
    check_in_code VARCHAR(36)   NOT NULL UNIQUE,
    checked_in_at DATETIME,
    reviewed      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       INT           NOT NULL DEFAULT 0,
    -- NULL once cancelled, so a cancelled slot can be booked again
    active_slot   DATETIME AS (CASE WHEN status <> 'CANCELLED' THEN slot_start END) STORED,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    -- the database-level guarantee against double-booking
    CONSTRAINT uq_doctor_active_slot UNIQUE (doctor_id, active_slot),
    INDEX idx_appointments_patient (patient_id, slot_start),
    INDEX idx_appointments_slot (slot_start)
);

CREATE TABLE lab_tests (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(40)   NOT NULL UNIQUE,
    name             VARCHAR(160)  NOT NULL,
    category         VARCHAR(40)   NOT NULL,
    description      VARCHAR(500),
    price            DECIMAL(10,2) NOT NULL,
    partner_price    DECIMAL(10,2),
    sample_type      VARCHAR(40),
    turnaround_hours INT           NOT NULL DEFAULT 24,
    -- result template: "name|unit|low-high;name|unit|low-high"
    parameters       VARCHAR(600),
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    INDEX idx_lab_tests_category (category)
);

CREATE TABLE lab_bookings (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id           BIGINT        NOT NULL,
    lab_test_id          BIGINT        NOT NULL,
    ordered_by_doctor_id BIGINT,
    scheduled_at         DATETIME      NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    price                DECIMAL(10,2) NOT NULL,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_bookings_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_lab_bookings_test FOREIGN KEY (lab_test_id) REFERENCES lab_tests (id),
    CONSTRAINT fk_lab_bookings_doctor FOREIGN KEY (ordered_by_doctor_id) REFERENCES doctors (id),
    INDEX idx_lab_bookings_patient (patient_id, scheduled_at)
);

CREATE TABLE lab_results (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT       NOT NULL,
    parameter       VARCHAR(80)  NOT NULL,
    value           VARCHAR(255) NOT NULL,
    unit            VARCHAR(20),
    reference_range VARCHAR(40),
    flag            VARCHAR(10),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_results_booking FOREIGN KEY (booking_id) REFERENCES lab_bookings (id) ON DELETE CASCADE
);

CREATE TABLE prescriptions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT       NOT NULL UNIQUE,
    doctor_id      BIGINT       NOT NULL,
    patient_id     BIGINT       NOT NULL,
    diagnosis      VARCHAR(255) NOT NULL,
    notes          TEXT,
    follow_up_date DATE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptions_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE TABLE prescription_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT       NOT NULL,
    medicine        VARCHAR(120) NOT NULL,
    dosage          VARCHAR(60)  NOT NULL,
    frequency       VARCHAR(60)  NOT NULL,
    duration_days   INT          NOT NULL,
    instructions    VARCHAR(255),
    CONSTRAINT fk_items_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE
);

CREATE TABLE risk_assessments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT       NOT NULL,
    gender          VARCHAR(10)  NOT NULL,
    age             INT          NOT NULL,
    hypertension    BOOLEAN      NOT NULL,
    heart_disease   BOOLEAN      NOT NULL,
    smoking_history VARCHAR(20)  NOT NULL,
    bmi             DECIMAL(5,2) NOT NULL,
    hba1c           DECIMAL(4,2) NOT NULL,
    glucose         INT          NOT NULL,
    probability     DECIMAL(5,4) NOT NULL,
    risk_band       VARCHAR(20)  NOT NULL,
    model_version   VARCHAR(40)  NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    INDEX idx_risk_patient (patient_id, created_at)
);

CREATE TABLE blood_stock (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT     NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    units       INT        NOT NULL DEFAULT 0,
    updated_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_blood_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    CONSTRAINT uq_blood_hospital_group UNIQUE (hospital_id, blood_group)
);

CREATE TABLE organ_availability (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT      NOT NULL,
    organ       VARCHAR(30) NOT NULL,
    available   INT         NOT NULL DEFAULT 0,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_organ_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    CONSTRAINT uq_organ_hospital UNIQUE (hospital_id, organ)
);

-- Transactional outbox: rows are written in the same transaction as the business
-- change, then delivered by a scheduled worker with retries.
CREATE TABLE notification_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    channel         VARCHAR(20)  NOT NULL,
    recipient       VARCHAR(160) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME     NOT NULL,
    last_error      VARCHAR(500),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME,
    INDEX idx_outbox_due (status, next_attempt_at)
);
