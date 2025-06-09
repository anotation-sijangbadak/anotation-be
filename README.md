# Anotation BE
## 서비스 개요  

감정 기반 어쩌구 음악을 추천 어쩌구 소셜 어쩌구

---
## Conventions
### 1. Git Commit Convention
- feat: 기능 개발  
- docs: 문서 작업  
- refactor: 코드 최적화
  
### 2. RabbitMQ Convention
- **Exchange** : ```FROM Domain```.exchange
- **QUEUE** : ```TO Domain```.queue
- **ROUTING_KEY** : ```FROM Domain```.```TO Domain```.```TASK```

 ### 3. ErrorCode 접두어
- **A** - 인증,인가 관련 에러
- **U** - 사용자 관련 에러
- **V** - 검증 에러
- **C** - 공통 에러  
  - C999 - 예상치 못한 에러 발생
- **D** - 데이터 관련 에러
- **S** - 서버 내부 에러  


---
## 기타 사항
### SWAGGER 사용법
1) 로컬에서 서버를 돌린다. 
1) localhost:8000/swagger-ui로 접속한다.
1) API 스펙을 확인하고 테스트

