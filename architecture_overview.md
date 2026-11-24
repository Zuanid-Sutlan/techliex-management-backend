# Project Architecture Overview

Based on the structure of the `techliex-management` project, you are following a highly modular, decoupled architecture for both your backend and frontend. 

This document explains the structural patterns and architectural principles you are using.

---

## 1. Backend Architecture (Ktor)
Your backend strictly adheres to **Clean Architecture** (often associated with Domain-Driven Design). The core principle here is the **Separation of Concerns** through layered architecture, where dependencies always point inwards toward the Domain layer.

### Folder Structure Breakdown:
* **`domain/` (The Core Layer)**
  * *Purpose:* Contains your core business logic and rules. It is completely isolated and does not depend on any framework, database, or UI details.
  * **`model/` (e.g., `AppVersion.kt`, `User.kt`):** These are your Enterprise Business Rules. They are plain Kotlin data classes representing the core entities of your application.
  * **`repository/`:** Contains interfaces (contracts) for data access. The domain layer dictates *what* data operations it needs, but not *how* they are implemented.
  * **`usecase/`:** Contains application-specific business rules. Each use case (like `auth`, `user`, `order`) orchestrates the flow of data to and from the entities, and directs those entities to use their Enterprise Business Rules to achieve the goal of the use case.

* **`data/` (The Data Layer)**
  * *Purpose:* Implements the repository interfaces defined in the domain layer. This is where your application interacts with the database or external APIs.
  * **`repository/` (e.g., `UserRepositoryImpl.kt`):** Concrete implementations of the domain repository interfaces. 
  * **`db/`:** Contains database configuration, table definitions, and `DatabaseFactory.kt` to manage connections (likely using an ORM/query builder like Exposed).

* **`presentation/` (The Interface/Delivery Layer)**
  * *Purpose:* Handles incoming network requests, parses parameters, and sends responses. It translates between the external world (HTTP) and your internal domain.
  * **`routes/`:** Define the API endpoints (e.g., `UserRoutes.kt`). They receive the HTTP requests and delegate the logic to the appropriate `usecase`.
  * **`dto/` (Data Transfer Objects):** Defines the exact structure of the JSON payloads you expect to receive (Requests) and send (Responses). This prevents exposing your internal domain models directly to the API clients.

* **`plugins/` & `security/` (The Framework Layer)**
  * *Purpose:* Ktor-specific setup. 
  * `Plugins.kt` & `Routing.kt` initialize your server features.
  * `security/` handles authentication flows like JWT generation and validation logic.

### Architectural Pattern Flow:
1. **Client** makes an HTTP Request.
2. **`presentation/routes/`** intercepts the request, validates the payload using a **`dto`**.
3. The Route calls a **`domain/usecase/`** to perform a specific action.
4. The Use Case executes business logic and calls a **`domain/repository/`** interface to fetch/save data.
5. The **`data/repository/`** implementation executes the query via **`data/db/`** and returns a **`domain/model/`** back up the chain.
6. The Route maps the resulting Model back into a Response **`dto`** and sends it to the Client.

---

## 2. Frontend Architecture (Vanilla Web)
Your frontend structure uses a **Modular Vanilla Web Architecture**. Instead of relying on heavy UI frameworks (like React or Angular), you rely on clear separation of files based on their functional role (View, Network, Logic).

### Folder Structure Breakdown:
* **`pages/` (The View Layer)**
  * *Purpose:* Contains your HTML files (`login.html`, `dashboard.html`). These act as your view templates. By splitting them into separate files, you keep your UI components isolated and manageable.
* **`css/` (The Presentation Styling)**
  * *Purpose:* Holds `styles.css`. This separates your visual logic from your HTML structure and JavaScript behavior.
* **`js/` (The Logic & Network Layer)**
  * *Purpose:* The brains of your frontend, split into specialized responsibilities.
  * **`api.js` (The Service / Network Pattern):** Centralizes all `fetch` or `XHR` calls to your Ktor backend. By keeping API calls in one file, if your backend endpoints or authentication methods change, you only have to update this single file rather than hunting through your UI code.
  * **`app.js` (The Core App Logic):** Handles global application state, routing logic, navigation bars, and initialization routines that are common across multiple pages.
  * **`modules/` (The Feature/Controller Pattern):** Contains specific logic tied to individual pages (e.g., `login.js`). This script is only loaded/executed when its corresponding page is active. It binds events to the DOM elements of that page and coordinates with `api.js`.

### Architectural Pattern Flow:
1. User navigates to **`pages/login.html`**.
2. The page loads **`app.js`** (for global setup) and **`js/modules/login.js`** (for specific logic).
3. The user submits a form. **`login.js`** captures the DOM event and extracts the data.
4. **`login.js`** calls a function inside **`js/api.js`** (e.g., `loginUser(credentials)`).
5. **`api.js`** makes the HTTP request to the backend.
6. Upon a successful response, **`api.js`** returns the data (like a JWT token) back to **`login.js`**.
7. **`login.js`** updates the DOM or asks **`app.js`** to redirect the user to the Dashboard.

## Summary of Benefits
By following these patterns in your projects, you achieve:
1. **High Testability:** Business logic (Use Cases) can be tested entirely independently of the database or HTTP framework.
2. **Easy Refactoring:** Changing the database structure only affects the `data` layer. Changing the API payload only affects the `presentation` layer.
3. **Maintainability:** New developers can easily find where things live. If there is a database issue, they look in `data`. If there is a business rule bug, they look in `domain`. If there is an API parsing issue, they look in `presentation`.