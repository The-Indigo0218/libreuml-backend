com.libreuml.backend
├── domain (EL NÚCLEO - PURO JAVA)
│   └── model
│       ├── User.java (Abstract)
│       ├── Student.java
│       ├── RoleEnum.java
│       └── ...
│
├── application (LA LÓGICA - ORQUESTACIÓN)
│   ├── port
│   │   ├── in (ENTRADA)
│   │   │   ├── CreateUserUseCase.java (Interfaz)
│   │   │   ├── GetUserUseCase.java (Interfaz)
│   │   │   └── CreateUserCommand.java (DTO interno)
│   │   └── out (SALIDA)
│   │       ├── UserRepository.java (Interfaz DB)
│   │       └── PasswordEncoderPort.java (Interfaz Seguridad)
│   ├── service
│   │   └── UserService.java (Implementación de UseCases)
│   └── mapper
│       └── UserFactory.java (Crea instancias de dominio)
│
└── infrastructure (EL MUNDO REAL - SPRING BOOT)
├── in.rest (ADAPTADOR WEB)
│   ├── UserRestController.java
│   └── dto
│       ├── RegisterRequest.java
│       └── UserWebResponse.java
└── out.persistence (ADAPTADOR DB)
├── UserPersistenceAdapter.java (Implementa UserRepository)
├── repository
│   └── SpringDataUserRepository.java (JPA)
├── entity
│   └── UserEntity.java (Tabla DB)
└── mapper
└── UserMapper.java (Entity <-> Domain)