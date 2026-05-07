-- Campus Market initial schema
-- File name for Flyway: src/main/resources/db/migration/V1__init_schema.sql
-- Target DB: MySQL 8.4+ / InnoDB / utf8mb4

SET NAMES utf8mb4;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  email_hash BINARY(32) NULL COMMENT '이메일 SHA-256 해시. 원문 이메일은 저장하지 않음',
  phone_hash BINARY(32) NULL COMMENT '전화번호 SHA-256 해시. 선택 수집',
  nickname VARCHAR(40) NOT NULL COMMENT '서비스 내 닉네임',
  profile_image_url VARCHAR(500) NULL COMMENT '프로필 이미지 URL',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '사용자 상태',
  trust_score INT NOT NULL DEFAULT 500 COMMENT '기본 신뢰 점수',
  trade_completed_count INT NOT NULL DEFAULT 0 COMMENT '거래 완료 수',
  no_show_count INT NOT NULL DEFAULT 0 COMMENT '노쇼 기록 수',
  report_received_count INT NOT NULL DEFAULT 0 COMMENT '받은 신고 수',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 시각',
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uk_users_email_hash UNIQUE (email_hash),
  CONSTRAINT uk_users_phone_hash UNIQUE (phone_hash),
  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED', 'DELETED')),
  CONSTRAINT chk_users_trust_score CHECK (trust_score BETWEEN 0 AND 1000),
  CONSTRAINT chk_users_counts CHECK (
    trade_completed_count >= 0 AND no_show_count >= 0 AND report_received_count >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='서비스 사용자. 개인정보 원문 대신 해시와 최소 프로필만 저장한다.';

CREATE TABLE refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '리프레시 토큰 ID',
  user_id BIGINT NOT NULL COMMENT '토큰 소유 사용자 ID',
  token_hash BINARY(32) NOT NULL COMMENT '리프레시 토큰 SHA-256 해시',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '토큰 상태',
  expires_at DATETIME(6) NOT NULL COMMENT '만료 시각',
  last_used_at DATETIME(6) NULL COMMENT '마지막 사용 시각',
  revoked_at DATETIME(6) NULL COMMENT '폐기 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
  CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
  CONSTRAINT chk_refresh_tokens_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED')),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_refresh_tokens_user_status (user_id, status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='JWT refresh token rotation과 강제 로그아웃을 위한 토큰 저장소.';

CREATE TABLE schools (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '학교 ID',
  name VARCHAR(100) NOT NULL COMMENT '학교명',
  region VARCHAR(50) NULL COMMENT '지역명',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '학교 사용 상태',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_schools PRIMARY KEY (id),
  CONSTRAINT uk_schools_name UNIQUE (name),
  CONSTRAINT chk_schools_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='서비스가 지원하는 학교 마스터.';

CREATE TABLE school_email_domains (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '학교 이메일 도메인 ID',
  school_id BIGINT NOT NULL COMMENT '학교 ID',
  domain VARCHAR(120) NOT NULL COMMENT '학교 이메일 도메인. 예: example.ac.kr',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '도메인 사용 상태',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_school_email_domains PRIMARY KEY (id),
  CONSTRAINT uk_school_email_domains_domain UNIQUE (domain),
  CONSTRAINT chk_school_email_domains_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT fk_school_email_domains_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_school_email_domains_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='학교별 허용 이메일 도메인 목록.';

CREATE TABLE school_email_verification_codes (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '학교 이메일 인증 코드 ID',
  user_id BIGINT NULL COMMENT '가입 전 인증이면 NULL 가능',
  school_id BIGINT NOT NULL COMMENT '인증 대상 학교 ID',
  email_hash BINARY(32) NOT NULL COMMENT '인증 대상 이메일 SHA-256 해시',
  code_hash BINARY(32) NOT NULL COMMENT '인증 코드 SHA-256 해시',
  purpose VARCHAR(30) NOT NULL DEFAULT 'SCHOOL_SIGNUP' COMMENT '인증 목적',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '인증 코드 상태',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '검증 시도 횟수',
  expires_at DATETIME(6) NOT NULL COMMENT '코드 만료 시각',
  verified_at DATETIME(6) NULL COMMENT '인증 완료 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_school_email_verification_codes PRIMARY KEY (id),
  CONSTRAINT chk_school_email_verification_codes_purpose CHECK (purpose IN ('SCHOOL_SIGNUP', 'SCHOOL_REVERIFY')),
  CONSTRAINT chk_school_email_verification_codes_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'CANCELED')),
  CONSTRAINT chk_school_email_verification_codes_attempt_count CHECK (attempt_count >= 0),
  CONSTRAINT fk_school_email_verification_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_school_email_verification_codes_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_school_email_codes_lookup (school_id, email_hash, status, expires_at),
  KEY idx_school_email_codes_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Redis 없이 MySQL로 시작하기 위한 학교 이메일 인증 코드 저장소.';

CREATE TABLE user_school_verifications (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 학교 인증 ID',
  user_id BIGINT NOT NULL COMMENT '사용자 ID',
  school_id BIGINT NOT NULL COMMENT '학교 ID',
  method VARCHAR(20) NOT NULL COMMENT '인증 방식',
  verified_email_hash BINARY(32) NULL COMMENT '인증된 학교 이메일 해시',
  student_number_hash BINARY(32) NULL COMMENT '학번 해시. 원문 저장 금지',
  admission_year SMALLINT NULL COMMENT '입학연도. 학번 원문 대체',
  department_name VARCHAR(100) NULL COMMENT '학과명. 초기에는 마스터 테이블로 분리하지 않음',
  status VARCHAR(20) NOT NULL DEFAULT 'VERIFIED' COMMENT '학교 인증 상태',
  verified_at DATETIME(6) NULL COMMENT '인증 완료 시각',
  expires_at DATETIME(6) NULL COMMENT '인증 만료 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_user_school_verifications PRIMARY KEY (id),
  CONSTRAINT uk_user_school_verifications_user_school UNIQUE (user_id, school_id),
  CONSTRAINT chk_user_school_verifications_method CHECK (method IN ('EMAIL', 'DOCUMENT', 'ADMIN')),
  CONSTRAINT chk_user_school_verifications_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED', 'REVOKED')),
  CONSTRAINT fk_user_school_verifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_user_school_verifications_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_user_school_verifications_school_status (school_id, status),
  KEY idx_user_school_verifications_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자와 학교의 인증 관계. 사용자별 학교 접근권한의 기준이 된다.';

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
  parent_id BIGINT NULL COMMENT '상위 카테고리 ID',
  code VARCHAR(50) NOT NULL COMMENT '카테고리 코드',
  name VARCHAR(50) NOT NULL COMMENT '카테고리명',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_categories PRIMARY KEY (id),
  CONSTRAINT uk_categories_code UNIQUE (code),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_categories_parent_sort (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='판매글 카테고리. 교재, 전자기기, 자취/기숙사 등 탐색 축.';

CREATE TABLE listings (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '판매글 ID',
  school_id BIGINT NOT NULL COMMENT '판매글이 노출될 학교 ID',
  seller_id BIGINT NOT NULL COMMENT '판매자 사용자 ID',
  category_id BIGINT NOT NULL COMMENT '카테고리 ID',
  title VARCHAR(120) NOT NULL COMMENT '제목',
  description TEXT NOT NULL COMMENT '본문 설명',
  price INT NOT NULL COMMENT '가격. 0원 나눔 허용',
  condition_grade VARCHAR(20) NOT NULL COMMENT '상품 상태 등급',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '판매글 상태',
  pickup_place_text VARCHAR(120) NULL COMMENT '희망 픽업 위치 자유 입력',
  view_count INT NOT NULL DEFAULT 0 COMMENT '조회수',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '찜 수',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 시각',
  CONSTRAINT pk_listings PRIMARY KEY (id),
  CONSTRAINT chk_listings_price CHECK (price >= 0),
  CONSTRAINT chk_listings_counts CHECK (view_count >= 0 AND favorite_count >= 0),
  CONSTRAINT chk_listings_condition_grade CHECK (condition_grade IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR')),
  CONSTRAINT chk_listings_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RESERVED', 'SOLD', 'HIDDEN', 'DELETED')),
  CONSTRAINT fk_listings_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_listings_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_listings_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_listings_school_status_created (school_id, status, created_at, id),
  KEY idx_listings_seller_created (seller_id, created_at, id),
  KEY idx_listings_category_created (category_id, created_at, id),
  FULLTEXT KEY ft_listings_title_description (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='학교 단위 중고거래 판매글.';

CREATE TABLE listing_images (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '판매글 이미지 ID',
  listing_id BIGINT NOT NULL COMMENT '판매글 ID',
  image_url VARCHAR(500) NOT NULL COMMENT '이미지 URL',
  sort_order INT NOT NULL COMMENT '노출 순서',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_listing_images PRIMARY KEY (id),
  CONSTRAINT uk_listing_images_listing_order UNIQUE (listing_id, sort_order),
  CONSTRAINT chk_listing_images_sort_order CHECK (sort_order >= 0),
  CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE ON UPDATE CASCADE,
  KEY idx_listing_images_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='판매글 이미지. 실제 바이너리는 object storage에 저장한다.';

CREATE TABLE listing_textbook_details (
  listing_id BIGINT NOT NULL COMMENT '판매글 ID이자 PK',
  isbn VARCHAR(20) NULL COMMENT 'ISBN',
  course_code VARCHAR(40) NULL COMMENT '과목 코드',
  course_name VARCHAR(120) NULL COMMENT '과목명',
  professor_name VARCHAR(80) NULL COMMENT '교수명',
  semester VARCHAR(20) NULL COMMENT '학기. 예: 2026-1',
  CONSTRAINT pk_listing_textbook_details PRIMARY KEY (listing_id),
  CONSTRAINT fk_listing_textbook_details_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE ON UPDATE CASCADE,
  KEY idx_listing_textbook_details_isbn (isbn),
  KEY idx_listing_textbook_details_course (course_code, course_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='교재 카테고리 전용 상세 정보. 모든 판매글에 nullable 컬럼을 두지 않기 위해 분리.';

CREATE TABLE listing_favorites (
  user_id BIGINT NOT NULL COMMENT '찜한 사용자 ID',
  listing_id BIGINT NOT NULL COMMENT '찜한 판매글 ID',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_listing_favorites PRIMARY KEY (user_id, listing_id),
  CONSTRAINT fk_listing_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_listing_favorites_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE ON UPDATE CASCADE,
  KEY idx_listing_favorites_listing (listing_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자의 판매글 찜 관계. 중복 찜 방지를 위해 복합 PK 사용.';

CREATE TABLE chat_rooms (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅방 ID',
  listing_id BIGINT NOT NULL COMMENT '대상 판매글 ID',
  seller_id BIGINT NOT NULL COMMENT '판매자 ID',
  buyer_id BIGINT NOT NULL COMMENT '구매 희망자 ID',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '채팅방 상태',
  last_message_at DATETIME(6) NULL COMMENT '마지막 메시지 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_chat_rooms PRIMARY KEY (id),
  CONSTRAINT uk_chat_rooms_listing_buyer UNIQUE (listing_id, buyer_id),
  CONSTRAINT chk_chat_rooms_status CHECK (status IN ('ACTIVE', 'CLOSED', 'BLOCKED')),
  CONSTRAINT chk_chat_rooms_participants CHECK (seller_id <> buyer_id),
  CONSTRAINT fk_chat_rooms_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_chat_rooms_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  CONSTRAINT fk_chat_rooms_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  KEY idx_chat_rooms_seller_last_message (seller_id, last_message_at, id),
  KEY idx_chat_rooms_buyer_last_message (buyer_id, last_message_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='판매글 기준 1:1 거래 채팅방.';

CREATE TABLE chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅 메시지 ID',
  room_id BIGINT NOT NULL COMMENT '채팅방 ID',
  sender_id BIGINT NOT NULL COMMENT '발신자 ID',
  message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '메시지 유형',
  body TEXT NULL COMMENT '텍스트 본문',
  attachment_url VARCHAR(500) NULL COMMENT '첨부 URL',
  receiver_read_at DATETIME(6) NULL COMMENT '상대방 읽음 시각. 1:1 채팅이라 단일 컬럼으로 처리',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_chat_messages PRIMARY KEY (id),
  CONSTRAINT chk_chat_messages_type CHECK (message_type IN ('TEXT', 'IMAGE', 'SYSTEM')),
  CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_chat_messages_room_created (room_id, created_at, id),
  KEY idx_chat_messages_sender_created (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='채팅방 메시지. 신고/분쟁 대응을 위해 즉시 hard delete하지 않는다.';

CREATE TABLE meeting_places (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '약속 장소 ID',
  school_id BIGINT NOT NULL COMMENT '학교 ID',
  name VARCHAR(100) NOT NULL COMMENT '장소명',
  description VARCHAR(255) NULL COMMENT '장소 설명',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_meeting_places PRIMARY KEY (id),
  CONSTRAINT fk_meeting_places_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_meeting_places_school_sort (school_id, sort_order),
  CONSTRAINT uk_meeting_places_school_name UNIQUE (school_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='학교별 추천 직거래 약속 장소.';

CREATE TABLE trades (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '거래 ID',
  listing_id BIGINT NOT NULL COMMENT '판매글 ID',
  seller_id BIGINT NOT NULL COMMENT '판매자 ID',
  buyer_id BIGINT NOT NULL COMMENT '구매자 ID',
  chat_room_id BIGINT NOT NULL COMMENT '거래가 시작된 채팅방 ID',
  meeting_place_id BIGINT NULL COMMENT '선택된 학교 약속 장소 ID',
  scheduled_at DATETIME(6) NULL COMMENT '약속 시각',
  final_price INT NOT NULL COMMENT '최종 거래 가격',
  safety_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT' COMMENT '거래 안전 모드',
  status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT '거래 상태',
  version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_trades PRIMARY KEY (id),
  CONSTRAINT chk_trades_participants CHECK (seller_id <> buyer_id),
  CONSTRAINT chk_trades_final_price CHECK (final_price >= 0),
  CONSTRAINT chk_trades_version CHECK (version >= 0),
  CONSTRAINT chk_trades_safety_mode CHECK (safety_mode IN ('DIRECT', 'SAFE_PAYMENT')),
  CONSTRAINT chk_trades_status CHECK (status IN ('REQUESTED', 'RESERVED', 'CANCELED', 'COMPLETED', 'DISPUTED')),
  CONSTRAINT fk_trades_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_trades_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  CONSTRAINT fk_trades_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT fk_trades_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_trades_meeting_place FOREIGN KEY (meeting_place_id) REFERENCES meeting_places(id) ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_trades_listing_status (listing_id, status),
  KEY idx_trades_seller_updated (seller_id, updated_at, id),
  KEY idx_trades_buyer_updated (buyer_id, updated_at, id),
  KEY idx_trades_chat_room (chat_room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='구매 의사, 예약, 완료, 분쟁까지 이어지는 거래 단위.';

CREATE TABLE trade_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '거래 상태 이력 ID',
  trade_id BIGINT NOT NULL COMMENT '거래 ID',
  from_status VARCHAR(20) NULL COMMENT '이전 상태',
  to_status VARCHAR(20) NOT NULL COMMENT '변경 후 상태',
  changed_by BIGINT NOT NULL COMMENT '상태 변경 사용자 ID',
  reason VARCHAR(255) NULL COMMENT '변경 사유',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_trade_status_history PRIMARY KEY (id),
  CONSTRAINT chk_trade_status_history_to_status CHECK (to_status IN ('REQUESTED', 'RESERVED', 'CANCELED', 'COMPLETED', 'DISPUTED')),
  CONSTRAINT chk_trade_status_history_from_status CHECK (from_status IS NULL OR from_status IN ('REQUESTED', 'RESERVED', 'CANCELED', 'COMPLETED', 'DISPUTED')),
  CONSTRAINT fk_trade_status_history_trade FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_trade_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_trade_status_history_trade_created (trade_id, created_at, id),
  KEY idx_trade_status_history_changed_by (changed_by, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='거래 상태 전이 이력. 분쟁/운영 대응과 디버깅에 사용한다.';

CREATE TABLE reviews (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '후기 ID',
  trade_id BIGINT NOT NULL COMMENT '거래 ID',
  reviewer_id BIGINT NOT NULL COMMENT '후기 작성자 ID',
  reviewee_id BIGINT NOT NULL COMMENT '후기 대상자 ID',
  rating TINYINT NOT NULL COMMENT '평점 1~5',
  tags_json JSON NULL COMMENT '후기 태그 JSON 배열',
  comment VARCHAR(500) NULL COMMENT '후기 본문',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_reviews PRIMARY KEY (id),
  CONSTRAINT uk_reviews_trade_reviewer UNIQUE (trade_id, reviewer_id),
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT chk_reviews_participants CHECK (reviewer_id <> reviewee_id),
  CONSTRAINT fk_reviews_trade FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id),
  KEY idx_reviews_reviewee_created (reviewee_id, created_at, id),
  KEY idx_reviews_reviewer_created (reviewer_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='거래 완료 후 상호 후기와 신뢰 지표 산출 근거.';

CREATE TABLE reports (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 ID',
  reporter_id BIGINT NOT NULL COMMENT '신고자 ID',
  target_type VARCHAR(30) NOT NULL COMMENT '신고 대상 유형',
  target_id BIGINT NOT NULL COMMENT '신고 대상 ID. 다형성 참조라 FK 없음',
  reason VARCHAR(50) NOT NULL COMMENT '신고 사유 코드',
  detail VARCHAR(1000) NULL COMMENT '상세 내용',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '신고 처리 상태',
  assigned_admin_id BIGINT NULL COMMENT '담당 관리자 ID',
  resolved_at DATETIME(6) NULL COMMENT '처리 완료 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_reports PRIMARY KEY (id),
  CONSTRAINT chk_reports_target_type CHECK (target_type IN ('USER', 'LISTING', 'CHAT_ROOM', 'CHAT_MESSAGE', 'TRADE', 'REVIEW')),
  CONSTRAINT chk_reports_status CHECK (status IN ('PENDING', 'ASSIGNED', 'RESOLVED', 'REJECTED')),
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_reports_assigned_admin FOREIGN KEY (assigned_admin_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_reports_status_created (status, created_at, id),
  KEY idx_reports_reporter_created (reporter_id, created_at, id),
  KEY idx_reports_target (target_type, target_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자/판매글/채팅/거래/후기 신고 큐. target은 다형성 참조로 애플리케이션에서 검증한다.';

CREATE TABLE user_blocks (
  blocker_id BIGINT NOT NULL COMMENT '차단한 사용자 ID',
  blocked_id BIGINT NOT NULL COMMENT '차단된 사용자 ID',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_user_blocks PRIMARY KEY (blocker_id, blocked_id),
  CONSTRAINT chk_user_blocks_not_self CHECK (blocker_id <> blocked_id),
  CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users(id),
  CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users(id),
  KEY idx_user_blocks_blocked (blocked_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자 간 차단 관계. 채팅 시작/전송 권한 검증에 사용한다.';

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
  user_id BIGINT NOT NULL COMMENT '수신 사용자 ID',
  type VARCHAR(50) NOT NULL COMMENT '알림 유형',
  channel VARCHAR(20) NOT NULL COMMENT '발송 채널',
  payload_json JSON NOT NULL COMMENT '알림 페이로드',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태',
  scheduled_at DATETIME(6) NOT NULL COMMENT '발송 예정 시각',
  sent_at DATETIME(6) NULL COMMENT '발송 완료 시각',
  failed_reason VARCHAR(500) NULL COMMENT '발송 실패 사유',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_notifications PRIMARY KEY (id),
  CONSTRAINT chk_notifications_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH')),
  CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'CANCELED')),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_notifications_status_scheduled (status, scheduled_at, id),
  KEY idx_notifications_user_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='인앱/이메일/SMS/푸시 알림 발송 큐. Kafka 없이 MySQL 기반 scheduled sender로 시작한다.';

CREATE TABLE admin_audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '관리자 감사 로그 ID',
  admin_id BIGINT NOT NULL COMMENT '관리자 사용자 ID',
  action VARCHAR(80) NOT NULL COMMENT '관리자 행위 코드',
  target_type VARCHAR(30) NOT NULL COMMENT '대상 유형',
  target_id BIGINT NOT NULL COMMENT '대상 ID',
  reason VARCHAR(500) NULL COMMENT '처리 사유',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_admin_audit_logs PRIMARY KEY (id),
  CONSTRAINT fk_admin_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_admin_audit_logs_target (target_type, target_id, created_at, id),
  KEY idx_admin_audit_logs_admin (admin_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='게시글 숨김, 신고 처리, 사용자 정지 등 모든 관리자 조치 이력.';

CREATE TABLE idempotency_keys (
  idempotency_key VARCHAR(100) NOT NULL COMMENT '클라이언트가 보낸 멱등키',
  user_id BIGINT NOT NULL COMMENT '요청 사용자 ID',
  request_method VARCHAR(10) NOT NULL COMMENT 'HTTP 메서드',
  request_path VARCHAR(255) NOT NULL COMMENT '요청 경로',
  request_hash BINARY(32) NOT NULL COMMENT '요청 본문 해시',
  response_status INT NULL COMMENT '저장된 응답 HTTP 상태',
  response_body_hash BINARY(32) NULL COMMENT '응답 본문 해시',
  status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '멱등 처리 상태',
  expires_at DATETIME(6) NOT NULL COMMENT '멱등키 만료 시각',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_idempotency_keys PRIMARY KEY (idempotency_key),
  CONSTRAINT chk_idempotency_keys_method CHECK (request_method IN ('POST', 'PUT', 'PATCH', 'DELETE')),
  CONSTRAINT chk_idempotency_keys_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
  CONSTRAINT fk_idempotency_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  KEY idx_idempotency_keys_user_created (user_id, created_at),
  KEY idx_idempotency_keys_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='거래 예약/상태 변경/결제 등 중복 요청 방지를 위한 멱등키 저장소.';
