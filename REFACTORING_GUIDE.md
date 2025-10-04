# 레이어드 아키텍처 리팩토링 가이드

## 📁 새로운 패키지 구조

```
org.shop.apiserver/
│
├── presentation/                              # 프레젠테이션 계층
│   ├── controller/
│   │   ├── ProductController.java
│   │   ├── OrderController.java
│   │   ├── PaymentController.java
│   │   ├── CartController.java
│   │   └── TodoController.java
│   │
│   ├── dto/                                   # Request/Response DTO
│   │   ├── request/
│   │   │   ├── ProductCreateRequest.java
│   │   │   ├── OrderCreateRequest.java
│   │   │   └── PageRequest.java
│   │   └── response/
│   │       ├── ProductResponse.java
│   │       ├── OrderResponse.java
│   │       └── PageResponse.java
│   │
│   ├── filter/
│   │   └── JWTAuthenticationFilter.java
│   │
│   └── handler/
│       ├── GlobalExceptionHandler.java
│       ├── LoginSuccessHandler.java
│       └── LoginFailureHandler.java
│
├── application/                               # 애플리케이션 계층
│   ├── service/
│   │   ├── ProductService.java
│   │   ├── OrderService.java
│   │   ├── PaymentService.java
│   │   └── CartService.java
│   │
│   ├── dto/                                   # 계층 간 전달 DTO
│   │   ├── ProductDTO.java
│   │   ├── OrderDTO.java
│   │   └── PaymentDTO.java
│   │
│   └── usecase/                               # Use Case (선택사항)
│       ├── CreateOrderUseCase.java
│       └── ProcessPaymentUseCase.java
│
├── domain/                                    # 도메인 계층
│   ├── model/                                 # Entity
│   │   ├── product/
│   │   │   ├── Product.java
│   │   │   └── ProductImage.java
│   │   ├── order/
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── OrderStatus.java
│   │   ├── payment/
│   │   │   ├── Payment.java
│   │   │   └── PaymentStatus.java
│   │   └── member/
│   │       ├── Member.java
│   │       └── MemberRole.java
│   │
│   ├── repository/                            # Repository Interface
│   │   ├── ProductRepository.java
│   │   ├── OrderRepository.java
│   │   └── PaymentRepository.java
│   │
│   ├── vo/                                    # Value Object
│   │   ├── Money.java
│   │   ├── Address.java
│   │   └── Email.java
│   │
│   └── service/                               # Domain Service
│       ├── OrderDomainService.java
│       └── PaymentDomainService.java
│
├── infrastructure/                            # 인프라 계층
│   ├── persistence/                           # Repository 구현
│   │   ├── jpa/
│   │   │   ├── ProductJpaRepository.java
│   │   │   ├── OrderJpaRepository.java
│   │   │   └── PaymentJpaRepository.java
│   │   └── querydsl/
│   │       └── ProductQueryRepository.java
│   │
│   ├── security/
│   │   ├── jwt/
│   │   │   ├── JWTProvider.java
│   │   │   └── JWTValidator.java
│   │   └── UserDetailsServiceImpl.java
│   │
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JpaConfig.java
│   │   └── WebMvcConfig.java
│   │
│   └── external/                              # 외부 API 연동
│       ├── payment/
│       │   └── TossPaymentClient.java
│       └── storage/
│           └── S3FileStorage.java
│
└── common/                                    # 공통 계층
    ├── exception/
    │   ├── BusinessException.java
    │   ├── EntityNotFoundException.java
    │   └── ErrorCode.java
    │
    ├── response/
    │   ├── ApiResponse.java
    │   └── ErrorResponse.java
    │
    └── util/
        ├── DateUtils.java
        └── StringUtils.java
```

---

## 🔄 마이그레이션 단계

### Phase 1: Common 계층 생성
1. Exception 클래스 통합
2. 공통 Response 객체 생성
3. Util 클래스 정리

### Phase 2: Domain 계층 분리
1. Entity를 도메인별로 패키지 분리
2. Repository Interface를 Domain 계층으로 이동
3. Domain Service 추가 (필요시)

### Phase 3: Infrastructure 계층 구성
1. Repository 구현체 분리
2. Security 관련 코드 이동
3. Config 파일 정리

### Phase 4: Application 계층 정리
1. Service DTO 분리
2. Use Case 패턴 적용 (선택)
3. Mapper 클래스 추가

### Phase 5: Presentation 계층 정리
1. Controller DTO 분리 (Request/Response)
2. Exception Handler 통합
3. Filter/Handler 정리

---

## 📊 계층별 의존성 규칙

```
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  (Controller, Filter, Handler)      │
└────────────┬────────────────────────┘
             │ depends on
             ↓
┌─────────────────────────────────────┐
│      Application Layer              │
│     (Service, UseCase)              │
└────────────┬────────────────────────┘
             │ depends on
             ↓
┌─────────────────────────────────────┐
│        Domain Layer                 │
│  (Entity, Repository Interface)     │
└─────────────────────────────────────┘
             ↑
             │ implements
┌────────────┴────────────────────────┐
│     Infrastructure Layer            │
│  (Repository Impl, Config, Util)    │
└─────────────────────────────────────┘
```

**핵심 규칙:**
1. Domain 계층은 어떤 계층에도 의존하지 않음 (순수 비즈니스 로직)
2. Application 계층은 Domain 계층에만 의존
3. Presentation 계층은 Application 계층에만 의존
4. Infrastructure 계층은 Domain 인터페이스를 구현
5. 모든 계층은 Common 계층 사용 가능

---

## 🎯 각 계층의 상세 역할

### 1. Presentation Layer
**역할:** 사용자 인터페이스 처리
- HTTP 요청/응답 처리
- 요청 데이터 검증 (Validation)
- DTO 변환 (Request → Application DTO)
- 인증/인가 처리

**금지사항:**
- ❌ 비즈니스 로직 작성
- ❌ 데이터베이스 직접 접근
- ❌ Entity 직접 반환

### 2. Application Layer
**역할:** 비즈니스 유스케이스 조율
- 트랜잭션 관리
- Domain 객체 조율
- DTO 변환 (Application DTO ↔ Domain Entity)
- 외부 서비스 호출 조율

**금지사항:**
- ❌ HTTP 관련 코드
- ❌ 데이터베이스 기술 의존

### 3. Domain Layer
**역할:** 핵심 비즈니스 로직
- Entity 정의
- 비즈니스 규칙 구현
- Repository 인터페이스 정의
- Domain Service (복잡한 비즈니스 로직)

**금지사항:**
- ❌ 프레임워크 의존성
- ❌ Infrastructure 의존성

### 4. Infrastructure Layer
**역할:** 기술적 구현
- Repository 구현 (JPA, QueryDSL)
- 외부 API 연동
- 파일 처리
- 보안 구현
- 설정 관리

---

## 📝 코딩 컨벤션

### DTO 네이밍
```java
// Presentation Layer
ProductCreateRequest
ProductUpdateRequest
ProductResponse
PageResponse<ProductResponse>

// Application Layer
ProductDTO
OrderDTO
PaymentDTO
```

### Service 네이밍
```java
// Application Service
ProductService
OrderService

// Domain Service
OrderDomainService
PaymentDomainService
```

### Repository 네이밍
```java
// Domain (Interface)
ProductRepository
OrderRepository

// Infrastructure (Implementation)
ProductJpaRepository extends JpaRepository
ProductRepositoryImpl implements ProductRepository
```

---

## ⚠️ 주의사항

1. **순환 참조 방지**
   - 계층 간 의존성 방향을 명확히 유지
   - 필요시 Event/Message 사용

2. **DTO 변환 위치**
   - Controller: Request/Response ↔ Application DTO
   - Service: Application DTO ↔ Domain Entity

3. **트랜잭션 경계**
   - Application Layer (Service)에서 관리
   - Domain Layer는 트랜잭션 무관

4. **예외 처리**
   - Domain: Domain Exception
   - Application: Application Exception
   - Presentation: HTTP Exception (GlobalExceptionHandler에서 변환)

---

## 🚀 마이그레이션 우선순위

### 우선순위 높음 (즉시 적용)
1. Common 계층 생성 (Exception, Response)
2. Domain Entity 패키지 분리
3. Repository Interface 이동

### 우선순위 중간 (점진적 적용)
4. DTO 분리 (Request/Response/Application)
5. Service 리팩토링
6. Infrastructure 분리

### 우선순위 낮음 (선택적 적용)
7. UseCase 패턴 적용
8. Domain Service 추가
9. Value Object 도입

---

## 📚 참고 자료

- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- Layered Architecture Pattern
- Hexagonal Architecture
