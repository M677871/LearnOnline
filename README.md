# LearnOnline – CSIS 231 Final Project

> **University of Balamand – Faculty of Arts & Sciences**  
> **CSIS 231 – Java Technology**  
> Secure online learning platform with **Spring Boot + PostgreSQL + JavaFX 2D/3D**,  
> **JWT + OTP 2FA**, and **role-based dashboards (Admin / Instructor / Student)**.

---

## 1. Project Overview

**LearnOnline** is a modular e-learning platform built as the final project for **CSIS 231 – Java Technology / Advanced Java**.

The system provides:

- A **secure REST backend** (Spring Boot + Spring Security + JWT + OTP 2FA).
- A **desktop JavaFX client** with:
    - Modular **FXML views** and shared styling (`styles.css`).
    - **2D dashboards** using JavaFX charts (`BarChart`, `LineChart`).
    - **3D analytics** using JavaFX 3D (`SubScene`, `Box`, `PerspectiveCamera`, `RotateTransition`).
- A **PostgreSQL** database as the single source of truth for all business logic and persistence.

### Roles & flows

- **Student**
    - Register and login (with **OTP-based 2FA** when required).
    - Browse courses, enroll, and take quizzes.
    - View **recent quiz performance** and progress in **2D and 3D visualizations**.
- **Instructor**
    - Manage their courses and quizzes.
    - View enrollments and quiz statistics.
    - Open **3D analytics** for their courses and see **aggregated quiz results**.
- **Admin**
    - Global view and management of users, roles, categories, courses, enrollments.
    - Access **data visualizations (2D + 3D)** for any instructor’s courses via a **course selector**.

This project is explicitly designed to match the **CSIS 231 Final Project requirements** (see section 11) and to demonstrate:

- Real **JavaFX 2D + 3D graphics** for dashboards and analytics.
- **JWT + OTP-based security**, with OTP used both for:
    - **Two-Factor Authentication (2FA)** on login (`LOGIN_2FA`).
    - **Forgot-password** flows (`PASSWORD_RESET` OTP).

---

## 2. High-Level Architecture

**Monorepo structure:**

```text
LearnOnline/
├── learnonline-api/          # Spring Boot backend (REST API + security)
└── learnonline-desktop/      # JavaFX 17 client (FXML + 2D/3D visualizations)
```

### Backend (Spring Boot – `learnonline-api`)

- **Layered architecture**:
    - `controller` – REST endpoints.
    - `service` – business logic, validation, security checks.
    - `repository` – Spring Data JPA repositories.
    - `domain/model` – entities (User, Role, Course, Enrollment, Quiz, OTP, etc.).
    - `exception` – custom exception hierarchy.
    - `common` – shared utilities, DTOs, `GlobalExceptionHandler`, constants.
- Stateless security with **JWT**.
- Email-based **OTP** for:
    - **LOGIN_2FA** (optional 2-factor on login).
    - **PASSWORD_RESET** (forgot-password).
- Centralized **error handling** via `@ControllerAdvice`.

### Database (PostgreSQL)

- Stores:
    - Users & Roles
    - Categories & Courses
    - Enrollments
    - Quizzes, Questions, Results
    - OTP tokens (purpose + expiry)
- Uses **Spring Data JPA** for ORM.
- Enforces relationships such as:
    - `User – Enrollment – Course`
    - `Course – Quiz – Result`.

### Frontend (JavaFX – `learnonline-desktop`)

- JavaFX 17 application with:
    - **FXML views** under `learnonline-desktop/src/main/resources/com/example/demo`.
    - Controllers under `learnonline-desktop/src/main/java/com/example/demo`.
    - Shared stylesheet: `styles.css` (dark theme, buttons, cards, tabs, 2D/3D viz styles).
- Navigation:
    - `Launcher` + `HelloApplication` handle scene switching and role-based routing.
    - Dedicated controllers for **Admin**, **Instructor**, **Student**, and **Graphics** (2D/3D).

---

## 3. Technology Stack

### Backend

- Java 17+
- Spring Boot (Web, Security, Validation)
- Spring Data JPA / Hibernate
- Spring Mail / JavaMail (for OTP e-mails)
- Spring Security + **JWT**
- Jakarta Bean Validation (`jakarta.validation`)

### Database

- PostgreSQL
- Configuration via `application.yml` + environment variables.

### Frontend

- JavaFX 17 (controls, FXML, 2D + 3D)
- JavaFX charts:
    - `BarChart<String, Number>`
    - `LineChart<String, Number>`
- JavaFX 3D:
    - `SubScene`, `Group`, `Box`, `PhongMaterial`, `PerspectiveCamera`
    - Animations using `RotateTransition`
- Shared CSS (`styles.css`) with:
    - Buttons: `.primary-button`, `.secondary-button`, `.ghost-pill`, …
    - Layout helpers: cards, sections, chips, toolbars
    - Styled `TabPane` and chart backgrounds

### Build & Tools

- Maven (multi-module)
- IntelliJ IDEA
- pgAdmin (for DB management)

---

## 4. Core Features by Role

### Student

- Register / login with **JWT auth** and optional **OTP 2FA**.
- Browse available courses and enroll.
- Take quizzes and see their results.
- Open a **“Visualize Progress”** screen with:
    - **3D bars** for quiz scores using JavaFX 3D.
    - **2D charts** (Bar/Line) for recent quiz performance by quiz name.

### Instructor

- Manage owned courses (create, update, archive).
- Manage quizzes attached to their courses.
- View enrollments & quiz statistics.
- Open **“3D Analytics”**:
    - 3D analytics per course (enrollments, scores).
    - 2D chart: **average quiz score per quiz** for the selected course.

### Admin

- Manage:
    - Users & roles
    - Categories
    - Courses
    - Enrollments
- Open **“Data Visualizations”**:
    - Select any instructor’s course from a **ComboBox**.
    - See **3D enrollment bar(s)** for that course.
    - See **2D quiz averages** by quiz name.

---

## 5. Security & Authentication
### JWT + OTP for 2FA and Forgot Password

### Login + 2FA flow

1. User sends `POST /api/auth/login` with username/password.
2. Backend validates credentials:
    - If **2FA not required** → returns `AuthResponse` with JWT.
    - If **2FA required** → throws `OtpRequiredException`, and:
        - Returns HTTP **202 Accepted** with `otpRequired = true`.
        - Sends **LOGIN_2FA OTP** to the user’s e-mail.
3. JavaFX client opens **`otp.fxml`** and calls `POST /api/auth/otp/verify`.
4. Backend verifies OTP:
    - On success → returns normal `AuthResponse` with JWT.
    - On failure → `401 Unauthorized` with unified error JSON.
5. Client stores JWT in **TokenStore / SessionStore** and includes it in `Authorization: Bearer <jwt>` for all calls.

### Forgot password (OTP-based `PASSWORD_RESET`)

1. User clicks **“Forgot password?”** in the JavaFX client.
2. Client calls `POST /api/auth/password/forgot` with e-mail.
3. Backend sends a **PASSWORD_RESET OTP** to this e-mail.
4. User enters OTP + new password; client calls `POST /api/auth/password/reset`.
5. Backend validates OTP and updates the password.

### OTP endpoints

- `POST /api/auth/otp/verify`  
  Verify OTP for both **LOGIN_2FA** and **PASSWORD_RESET**.
- `POST /api/auth/otp/request`  
  Resend OTP when the user did not receive it or it expired.

### Authorization & errors

- Role-based access enforced via **Spring Security**.
- UI hides/disables actions that are not allowed for Student/Instructor/Admin.
- `GlobalExceptionHandler` converts exceptions into a unified `ErrorResponse` with:
    - timestamp
    - HTTP status
    - error code
    - message
    - validation details (if any)

---

## 6. 2D & 3D Visualizations

**Location:**

- `learnonline-desktop/src/main/java/com/example/demo/graphics/GraphicsPlaygroundController.java`
- `learnonline-desktop/src/main/resources/com/example/demo/graphics/graphics_playground.fxml`

### Layout

- **Top bar**
    - “↩ Back” ghost-pill button routing back to the correct dashboard (Student/Instructor/Admin).
    - Title + chips for selected course, last refresh, role.
- **TabPane** with two tabs:
    1. **3D Analytics**
        - `SubScene` containing a 3D `Group` of `Box` bars.
        - `PerspectiveCamera` and simple materials (`PhongMaterial`).
        - `RotateTransition` for continuous rotation / highlighting.
        - Metrics:
            - Enrollments per course (Instructor/Admin).
            - Student quiz scores in 3D (Student view).
    2. **2D Progress**
        - `BarChart<String, Number>` or `LineChart<String, Number>` inside a “viz card”.
        - For students: recent quiz results (quiz name vs score).
        - For instructors/admin: **average scores per quiz** for the selected course.

### Backend data used for 2D/3D

- `GET /api/student/dashboard`  
  Student courses & recent quiz results.
- `GET /api/instructor/dashboard`  
  Instructor courses + stats.
- `GET /api/instructors/{userId}/courses`  
  Course selector for admin/instructor.
- `GET /api/courses/{courseId}/enrollments`  
  Enrollment count per course → **3D bars**.
- `GET /api/statistics/courses/{courseId}/quiz-averages`  
  Average quiz scores → **2D charts**.

---

## 7. Project Structure (Detailed)

### Backend – `learnonline-api`

```text
learnonline-api/
├── src/main/java/com/csis231/api
│   ├── auth/           # Login, register, forgot/reset password, JWT
│   ├── otp/            # OTP verify/request endpoints
│   ├── user/           # User & Role domain, services, controllers
│   ├── category/       # Course categories
│   ├── course/         # Courses & their details
│   ├── coursematerial/ # Course materials
│   ├── enrollment/     # Enrollments
│   ├── quiz/           # Quizzes, questions, results
│   ├── dashboard/      # Student & Instructor dashboards
│   ├── statistics/     # Quiz averages & analytics
│   ├── common/         # DTOs, helpers, GlobalExceptionHandler, constants
│   └── exception/      # Custom exception hierarchy
└── src/main/resources
    └── application.yml
```

### JavaFX Client – `learnonline-desktop`

```text
learnonline-desktop/
├── src/main/java/com/example/demo
│   ├── auth/          # Login, register, forgot password, OTP, reset controllers
│   ├── common/        # ApiClient, ApiException, ErrorDialog, AlertUtils, SessionStore, TokenStore
│   ├── course/        # Course catalog, details, editor, enrollments
│   ├── dashboard/     # Admin dashboard controller
│   ├── instructor/    # InstructorDashboardController
│   ├── student/       # StudentDashboardController
│   ├── quiz/          # Quiz creation + quiz-taker screens
│   ├── graphics/      # GraphicsPlaygroundController (2D/3D visualizations)
│   ├── Launcher.java  # Central navigation helper
│   └── HelloApplication.java # JavaFX entry point
└── src/main/resources/com/example/demo
    ├── fxml/          # login.fxml, register.fxml, otp.fxml, forgot_password.fxml, dashboard.fxml, ...
    ├── graphics/      # graphics_playground.fxml
    ├── styles.css     # shared JavaFX CSS
    └── icons/         # images/icons (if any)
```

---

## 8. REST API Reference (Actual Endpoints)

Below are the main mappings from:

- `AuthController`
- `OtpController`
- `UserController`
- `CategoryController`
- `CourseController`
- `CourseMaterialController`
- `EnrollmentController`
- `DashboardController`
- `StatisticsController`
- `QuizController`

### 8.1 Auth & OTP

#### Auth (`/api/auth`)

| HTTP   | Path                         | Description                                         |
|--------|------------------------------|-----------------------------------------------------|
| POST   | `/api/auth/login`           | Login with username/password (with optional 2FA).   |
| POST   | `/api/auth/register`        | Register a new user.                               |
| POST   | `/api/auth/password/forgot` | Start **forgot-password** OTP flow.                |
| POST   | `/api/auth/password/reset`  | Reset password using a **PASSWORD_RESET** OTP.     |

#### OTP (`/api/auth/otp`)

| HTTP   | Path                         | Description                                     |
|--------|------------------------------|-------------------------------------------------|
| POST   | `/api/auth/otp/verify`      | Verify OTP (`LOGIN_2FA` or `PASSWORD_RESET`).   |
| POST   | `/api/auth/otp/request`     | Resend OTP for a given username.                |

---

### 8.2 Users (`UserController` – `/api/csis-users`)

| HTTP   | Path                         | Description                          |
|--------|------------------------------|--------------------------------------|
| GET    | `/api/csis-users`           | List users (paged).                  |
| GET    | `/api/csis-users/{id}`      | Get a single user by id.            |
| POST   | `/api/csis-users`           | Create a new user.                  |
| PUT    | `/api/csis-users/{id}`      | Update an existing user.            |
| DELETE | `/api/csis-users/{id}`      | Delete a user.                      |
| GET    | `/api/csis-users/me`        | Get the authenticated user profile. |

---

### 8.3 Categories (`CategoryController` – `/api/categories`)

| HTTP   | Path                         | Description                   |
|--------|------------------------------|-------------------------------|
| GET    | `/api/categories`           | List all categories.         |
| GET    | `/api/categories/{id}`      | Get category by id.          |
| POST   | `/api/categories`           | Create category.             |
| PUT    | `/api/categories/{id}`      | Update category.             |
| DELETE | `/api/categories/{id}`      | Delete category.             |

---

### 8.4 Courses & Materials

#### Courses (`CourseController` – `/api/courses`)

| HTTP   | Path                         | Description                               |
|--------|------------------------------|-------------------------------------------|
| GET    | `/api/courses`              | List courses (with filters/paging).      |
| GET    | `/api/courses/{id}`         | Get course details by id.                |
| POST   | `/api/courses`              | Create a new course.                     |
| PUT    | `/api/courses/{id}`         | Update an existing course.               |
| DELETE | `/api/courses/{id}`         | Delete/archive a course.                 |

#### Course Materials (`CourseMaterialController` – `/api`)

| HTTP   | Path                                      | Description                                      |
|--------|-------------------------------------------|--------------------------------------------------|
| GET    | `/api/courses/{courseId}/materials`      | List materials for a course.                     |
| POST   | `/api/courses/{courseId}/materials`      | Add a new material to a course.                  |
| DELETE | `/api/materials/{id}`                    | Delete a material by id.                         |

---

### 8.5 Enrollments (`EnrollmentController` – `/api`)

| HTTP   | Path                                      | Description                                      |
|--------|-------------------------------------------|--------------------------------------------------|
| POST   | `/api/enrollments/enroll`                | Enroll a student in a course.                    |
| GET    | `/api/students/{userId}/enrollments`     | List a student’s enrollments.                    |
| GET    | `/api/courses/{courseId}/enrollments`    | List enrollments for a course (for stats/viz).   |

---

### 8.6 Dashboards & Statistics

#### Dashboards (`DashboardController`)

| HTTP   | Path                                | Description                           |
|--------|-------------------------------------|---------------------------------------|
| GET    | `/api/student/dashboard`           | Student dashboard data.              |
| GET    | `/api/instructor/dashboard`        | Instructor dashboard data.           |
| GET    | `/api/instructors/{userId}/courses`| Courses for a specific instructor.   |

#### Statistics (`StatisticsController` – `/api/statistics`)

| HTTP   | Path                                                | Description                                   |
|--------|-----------------------------------------------------|-----------------------------------------------|
| GET    | `/api/statistics/courses/{courseId}/quiz-averages` | Average score per quiz for a given course.    |

---

### 8.7 Quizzes (`QuizController` – `/api/quizzes`)

| HTTP   | Path                                 | Description                                          |
|--------|--------------------------------------|------------------------------------------------------|
| POST   | `/api/quizzes`                      | Create a new quiz.                                   |
| POST   | `/api/quizzes/{quizId}/questions`   | Add questions to a quiz.                             |
| GET    | `/api/quizzes/{quizId}`             | Get quiz definition (questions).                     |
| POST   | `/api/quizzes/{quizId}/submit`      | Submit answers to a quiz.                            |
| GET    | `/api/quizzes/{quizId}/results`     | Aggregated results for the quiz (instructor/admin).  |
| GET    | `/api/quizzes/{quizId}/my-result`   | Current user’s result for that quiz.                 |
| DELETE | `/api/quizzes/{quizId}`             | Delete a quiz.                                       |

---

## 9. Running the Project

### Prerequisites

- **JDK 17+**
- **Maven 3+**
- **PostgreSQL** (local or Docker)

### 1) Clone the repository

```bash
git clone https://github.com/M677871/LearnOnline.git
cd LearnOnline
```

### 2) Create the database

```sql
CREATE DATABASE csis_231_db;
```

### 3) Configure `application.yml`

Create/edit: `learnonline-api/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/csis_231_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate.format_sql: true

  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:your-gmail@example.com}
    password: ${MAIL_PASSWORD:your-gmail-app-password}  # Use a Gmail App Password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
      mail.smtp.auth.mechanisms: LOGIN PLAIN
      mail.smtp.ssl.trust: smtp.gmail.com
      mail.smtp.connectiontimeout: 10000
      mail.smtp.timeout: 10000
      mail.smtp.writetimeout: 10000
      mail.debug: false

management:
  health:
    mail:
      enabled: false

jwt:
  secret: ${JWT_SECRET:ChangeThisSecretForProductionUseALongRandomString}
  expiration: ${JWT_EXPIRATION:900000}  # 15 minutes

mail:
  from: ${MAIL_FROM:your-gmail@example.com}
```

> **Important:** never commit real passwords or secrets. Use environment variables in production.

### Environment variables (optional, override defaults)

- `DB_URL`, `DB_USER`, `DB_PASS`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `JWT_SECRET`, `JWT_EXPIRATION`

### 4) Run the backend

From `learnonline-api`:

```bash
mvn clean install
mvn spring-boot:run
```

Backend will start on `http://localhost:8080` (unless overridden).

### 5) Run the JavaFX client

From `learnonline-desktop`:

```bash
mvn clean install
mvn javafx:run
```

Or run `HelloApplication` from your IDE.  
The JavaFX app will handle login (with OTP 2FA), dashboards, and the **2D/3D analytics playground**.

---

## 10. State Management, Error Handling & Validation

### State & communication (JavaFX)

- **SessionStore / TokenStore** track:
    - Current user
    - JWT token(s)
    - Role
- All HTTP calls go through a shared **ApiClient**:
    - Adds `Authorization: Bearer <jwt>` header.
    - Deserializes JSON into DTOs.
    - Wraps errors in `ApiException`.

### Error handling (Frontend)

- Errors are displayed using:
    - `ErrorDialog.showError(ex)` or
    - `AlertUtils.showError(...)`
- Async calls use `CompletableFuture` + `Platform.runLater` to keep UI responsive.

### Validation (Backend)

- DTOs annotated with `@NotBlank`, `@Email`, `@Size`, etc.
- Validation errors are handled in `GlobalExceptionHandler` and returned in a structured JSON format that the client shows nicely.

---

## 11. How This Project Meets CSIS 231 Final Requirements

**Spring Boot Backend**

- Proper **layered architecture** (Controller / Service / Repository / Domain).
- PostgreSQL schema mapped via JPA entities.
- Business logic pushed into service layer, not controllers.
- Full, documented REST API (see section 8).

**Advanced Features & JavaFX 2D/3D**

- **Real 3D graphics**:
    - JavaFX 3D (`SubScene`, `Box`, `PerspectiveCamera`, `RotateTransition`).
    - 3D bars for course enrollments and student quiz scores.
- **2D charts**:
    - `BarChart`/`LineChart` for quiz performance and averages.
- Visualizations fed from real backend endpoints:
    - `/api/student/dashboard`
    - `/api/instructor/dashboard`
    - `/api/courses/{courseId}/enrollments`
    - `/api/statistics/courses/{courseId}/quiz-averages`

**JavaFX UI**

- Multiple FXML screens: login, register, forgot password, OTP, dashboards, courses, quizzes, graphics.
- Shared `styles.css` with reusable style classes.
- Clear role-based dashboards (Student / Instructor / Admin).
- Dedicated **2D/3D visualization playground** with back navigation and course selector.

**Security, OTP 2FA & Forgot Password**

- **JWT + Spring Security** for authentication and authorization.
- **OTP 2FA on login** via `/api/auth/login` + `/api/auth/otp/verify`.
- **OTP-based forgot-password** via `/api/auth/password/forgot` + `/api/auth/password/reset`.
- Centralized error handling and validation.

**Documentation & API Docs**

- This README includes:
    - Setup and configuration via `application.yml`.
    - Architecture & role-based feature overview.
    - Detailed description of **2D/3D** visualizations.
    - Full REST API reference.
    - Explanation of **OTP 2FA** and **forgot-password OTP** flows.
- Code is structured and ready for Javadoc / Swagger (springdoc) if extended.

---

## 12. Possible Future Improvements

- Add **springdoc-openapi** for Swagger UI.
- Add **WebSockets** for real-time quiz/enrollment updates.
- More analytics (engagement, retention, completion rate).
- Multi-metric 3D scenes (e.g., 2–3 bars per course).
- Audit logging and admin activity reports.

---

## 13. Credits

**Author:** Miled Issa  
**Course:** CSIS 231 – Java Technology  
**Institution:** University of Balamand – Faculty of Arts & Sciences  
