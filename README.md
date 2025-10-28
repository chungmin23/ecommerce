# E-Commerce API Server

> Spring Boot 3.5.6 기반의 현대적인 E-Commerce API 서버

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 목차

- [소개](#소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [프로젝트 구조](#프로젝트-구조)
- [테스트](#테스트)
- [성능 최적화](#성능-최적화)

---

## 소개

현대적인 E-Commerce 백엔드 시스템으로, **분산 락을 활용한 동시성 제어**, **AI 기반 상품 추천**, **선착순 쿠폰 시스템** 등 엔터프라이즈급 기능을 제공합니다.

### 핵심 특징

- **Redis 분산 락**으로 재고 관리 및 쿠폰 발급 시 동시성 제어
- **Spring AI + OpenAI**를 활용한 AI 기반 상품 추천
- **선착순 쿠폰 시스템** (중복 방지 + 재고 관리)
- **JWT 인증/인가**로 보안 강화
- **계층형 아키텍처** (Presentation → Application → Domain → Infrastructure)
- **K6 부하 테스트**로 검증된 성능

---

## 주요 기능

### 1. 쿠폰 시스템
- 선착순 쿠폰 발급 (재고 관리 + 중복 방지)
- 고정 금액 / 퍼센트 할인 타입
- 최소 주문 금액 설정
- 주문 시 자동 할인 적용

### 2. 주문/결제
- 장바구니 기반 주문 생성
- 재고 자동 감소/복구
- 주문 생명주기 관리 (PENDING → PAID → SHIPPED → DELIVERED)
- 다양한 결제 방법 지원 (CARD, CASH, BANK_TRANSFER)

### 3. 상품 관리
- 분산 락을 사용한 안전한 재고 관리
- AI 기반 상품 추천 (OpenAI GPT-4o-mini)
- QueryDSL 기반 동적 검색
- 상품 이미지 관리

### 4. 회원 관리
- JWT 기반 인증/인가
- BCrypt 비밀번호 암호화
- Access Token / Refresh Token
- Spring Security 통합

### 5. 장바구니
- 상품 추가/수정/삭제
- 장바구니 기반 주문 생성

---

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 17
- **ORM**: Spring Data JPA + QueryDSL 5.0.0
- **Security**: Spring Security + JWT
- **AI**: Spring AI 1.0.0-M4 (OpenAI)

### Database & Cache
- **Database**: MariaDB 11.4
- **Cache & Lock**: Redis 7 + Redisson 3.24.3

### Build & Test
- **Build Tool**: Gradle
- **Test**: JUnit 5, Spring Boot Test
- **Load Test**: K6

### Infrastructure
- **Container**: Docker, Docker Compose
- **Connection Pool**: HikariCP

---

## 시작하기

### 사전 요구사항

- Java 17 이상
- Docker & Docker Compose
- Gradle (또는 내장된 Gradle Wrapper 사용)
- (선택) K6 (부하 테스트용)

### 1. 저장소 클론

```bash
git clone <repository-url>
cd apiserver
```

### 2. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 다음 내용을 입력하세요:


### 3. 인프라 시작 (MariaDB, Redis)

```bash
docker-compose up -d
```

컨테이너 상태 확인:
```bash
docker-compose ps
```

### 4. 애플리케이션 실행

#### Linux/Mac:
```bash
./gradlew bootRun
```

#### Windows:
```bash
gradlew.bat bootRun
```

애플리케이션이 실행되면 http://localhost:8080 에서 API에 접근할 수 있습니다.

### 5. API 테스트

#### 헬스 체크:
```bash
curl http://localhost:8080/actuator/health
```

---

## API 문서

### 인증 (Authentication)

#### 회원 가입
```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

#### 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 상품 (Products)

#### 상품 목록 조회
```http
GET /api/products
Authorization: Bearer {accessToken}
```

#### 상품 상세 조회
```http
GET /api/products/{pno}
Authorization: Bearer {accessToken}
```

#### 상품 추천 (AI)
```http
POST /api/products/recommend
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "query": "여름에 입을만한 시원한 옷 추천해줘"
}
```

### 쿠폰 (Coupons)

#### 활성 쿠폰 목록
```http
GET /api/coupons/active
Authorization: Bearer {accessToken}
```

#### 쿠폰 발급
```http
POST /api/coupons/issue
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "couponCode": "SUMMER2024"
}
```

#### 내 쿠폰 목록
```http
GET /api/coupons/my
Authorization: Bearer {accessToken}
```

### 장바구니 (Cart)

#### 장바구니 조회
```http
GET /api/cart
Authorization: Bearer {accessToken}
```

#### 장바구니에 상품 추가
```http
POST /api/cart/items
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "pno": 1,
  "qty": 2
}
```

### 주문 (Orders)

#### 주문 생성
```http
POST /api/orders
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "orderItems": [
    {
      "pno": 1,
      "qty": 2
    }
  ],
  "delivery": {
    "receiverName": "홍길동",
    "receiverPhone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "zipCode": "12345",
    "deliveryMessage": "문 앞에 놓아주세요"
  },
  "paymentMethod": "CARD",
  "memberCouponId": 1
}
```

#### 내 주문 목록
```http
GET /api/orders/my
Authorization: Bearer {accessToken}
```

---

## 프로젝트 구조

```
apiserver/
├── src/
│   ├── main/
│   │   ├── java/org/shop/apiserver/
│   │   │   ├── application/          # 애플리케이션 계층
│   │   │   │   ├── dto/              # 데이터 전송 객체
│   │   │   │   ├── facade/           # 파사드 패턴 (복잡한 비즈니스 로직 조율)
│   │   │   │   └── service/          # 서비스 계층
│   │   │   │
│   │   │   ├── common/               # 공통 모듈
│   │   │   │   └── exception/        # 예외 처리
│   │   │   │
│   │   │   ├── domain/               # 도메인 계층 (핵심 비즈니스 로직)
│   │   │   │   └── model/            # 엔티티 (Product, Order, Coupon 등)
│   │   │   │
│   │   │   ├── infrastructure/       # 인프라 계층
│   │   │   │   ├── config/           # 설정 (Security, Redis, QueryDSL)
│   │   │   │   ├── lock/             # 분산 락 구현
│   │   │   │   ├── persistence/jpa/  # JPA 리포지토리
│   │   │   │   └── security/         # 보안 (JWT 필터 등)
│   │   │   │
│   │   │   └── presentation/         # 프레젠테이션 계층
│   │   │       └── controller/       # REST API 컨트롤러
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/                          # 테스트 코드
│
├── k6-tests/                          # K6 부하 테스트 스크립트
│   └── inventory-distributed-lock.js
│
├── docker-compose.yml                 # Docker 인프라 설정
├── build.gradle                       # Gradle 빌드 설정
└── README.md
```

### 계층형 아키텍처

이 프로젝트는 **계층형 아키텍처**를 따릅니다:

1. **Presentation Layer**: REST API 컨트롤러 (HTTP 요청/응답)
2. **Application Layer**: 비즈니스 로직 서비스 + 파사드
3. **Domain Layer**: 엔티티 및 핵심 도메인 로직
4. **Infrastructure Layer**: 데이터베이스, 보안, 외부 API 연동

---

## 테스트

### 단위 테스트 실행

```bash
./gradlew test
```

### K6 부하 테스트

분산 락을 사용한 재고 관리 동시성 테스트:

```bash
k6 run k6-tests/inventory-distributed-lock.js
```

**테스트 시나리오:**
- 10명의 가상 사용자가 동시에 상품 주문
- 초기 재고 50개
- 재고 정합성 검증 (판매량 ≤ 초기 재고)

**성능 목표:**
- HTTP 응답 시간: p95 < 2000ms, p99 < 3000ms
- HTTP 에러율: < 10%

---

## 성능 최적화

### 동시성 제어

프로젝트는 **Redisson 분산 락**을 사용하여 동시성 문제를 해결합니다:

1. **재고 감소/복구**: 분산 락으로 데이터 정합성 보장
2. **쿠폰 발급**: 중복 발급 방지 및 재고 관리
3. **Redis 원자적 연산**: INCR/DECR로 락 없는 카운팅

### 커넥션 풀 설정

- **HikariCP**: 30-150 커넥션 풀
- **Tomcat**: 최대 200 스레드, 1000 커넥션
- **Task Executor**: 코어 20, 최대 50 스레드

### 데이터베이스 최적화

- **트랜잭션 격리 수준**: READ_COMMITTED
- **JPA 페치 전략**: Lazy Loading 기본
- **인덱스**: FK 및 검색 필터 컬럼

---

## 환경별 설정

### 개발 환경
- JPA DDL: `hibernate.ddl-auto=update`
- SQL 로깅: `show-sql=true`
- CORS: 모든 Origin 허용

### 프로덕션 환경 (권장)
- JPA DDL: `hibernate.ddl-auto=validate`
- SQL 로깅: `show-sql=false`
- CORS: 특정 도메인만 허용
- JWT Secret: 강력한 비밀키 사용

---

