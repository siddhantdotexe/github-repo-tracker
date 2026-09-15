# GitHub Repo Tracker

GitHub Repo Tracker is a Spring Boot REST API that integrates with the GitHub public API to track and store statistics about user repositories. It allows users to fetch a GitHub user's repositories, store them in an in-memory database, query the tracked repositories by language or sort them, and keep them refreshed.

## Tech Stack

* **Java 17**
* **Spring Boot 3.3.4** (Spring Web, Spring Data JPA, Validation, Scheduling)
* **H2 Database** (In-memory database)
* **Lombok** (Boilerplate reduction)
* **Maven** (Dependency management)

## Architecture Flow

The application follows a standard Controller-Service-Repository architecture:
1. **GitHub API (External):** Provides the raw repository data via REST.
2. **GitHubClient (Service Layer):** A Spring service using `RestTemplate` that securely fetches data from the GitHub public API, passing appropriate headers (e.g. `User-Agent`), and parsing JSON responses into internal Data Transfer Objects (DTOs). It gracefully catches errors and translates HTTP statuses (e.g., 404 User Not Found, 403 Rate Limit) into custom runtime exceptions.
3. **RepoTrackingService (Business Logic):** Orchestrates the data flow. It accepts a username, invokes the `GitHubClient`, maps the returned DTOs to the `TrackedRepo` JPA entities, and performs an upsert (insert or update) in the database.
4. **H2 Database (Persistence):** Stores `TrackedRepo` entities via Spring Data JPA.
5. **RepoController (REST API):** Exposes endpoints to clients, formats the service layer output into clean `TrackedRepoResponseDto` objects, and handles global exceptions using a `@ControllerAdvice` for robust error reporting.

An automated `@Scheduled` job runs every 24 hours to automatically query GitHub for updates and refresh all stored repository metrics (such as star counts and last updated dates).

## How to Run Locally

1. **Prerequisites:** Ensure you have Java 17+ and Maven 3.6+ installed.
2. **Clone/Navigate** to the project directory.
3. **Build the project:**
   ```bash
   mvn clean package
   ```
4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```
   The API will be available at `http://localhost:8080`.
5. **Access H2 Console:** Navigate to `http://localhost:8080/h2-console` in your browser. 
   * **JDBC URL:** `jdbc:h2:mem:githubdb`
   * **Username:** `sa`
   * **Password:** *(leave blank)*

## API Endpoints

### 1. Track User Repositories
Fetches all public repositories for a specific GitHub user, saves/updates them in the database, and returns the tracked list.
* **URL:** `/api/repos/track/{username}`
* **Method:** `POST`
* **Example:**
  ```bash
  curl -X POST http://localhost:8080/api/repos/track/torvalds
  ```
* **Success Response (200 OK):**
  ```json
  [
      {
          "id": 1,
          "username": "torvalds",
          "repoName": "linux",
          "stars": 249087,
          "language": "C",
          "lastUpdated": "2026-09-15T15:24:50",
          "trackedSince": "2026-09-15T21:06:52"
      }
  ]
  ```

### 2. Get All Tracked Repositories (with filters)
Retrieves all tracked repositories. Supports optional filtering by programming language and sorting by `stars` or `lastUpdated`.
* **URL:** `/api/repos`
* **Method:** `GET`
* **Query Parameters:** `language` (optional string), `sortBy` (optional string: "stars" or "lastUpdated")
* **Example:**
  ```bash
  curl "http://localhost:8080/api/repos?language=C&sortBy=stars"
  ```
* **Success Response (200 OK):**
  ```json
  [
      {
          "id": 1,
          "username": "torvalds",
          "repoName": "linux",
          "stars": 249087,
          "language": "C",
          "lastUpdated": "2026-09-15T15:24:50",
          "trackedSince": "2026-09-15T21:06:52"
      }
  ]
  ```

### 3. Get Tracked Repositories by Username
Retrieves all tracked repositories specifically for a single username.
* **URL:** `/api/repos/{username}`
* **Method:** `GET`
* **Example:**
  ```bash
  curl http://localhost:8080/api/repos/torvalds
  ```
* **Success Response (200 OK):** Array of repository objects.

### 4. Delete Tracked Repositories by Username
Deletes all stored repositories for the specified username.
* **URL:** `/api/repos/{username}`
* **Method:** `DELETE`
* **Example:**
  ```bash
  curl -X DELETE http://localhost:8080/api/repos/torvalds
  ```
* **Success Response (204 No Content)**

### Example Error Response
If a non-existent user is queried (e.g. `POST /api/repos/track/fake_user_that_does_not_exist_9999`), a **404 Not Found** response is returned:
```json
{
    "timestamp": "2026-09-15T21:06:53",
    "status": 404,
    "error": "Not Found",
    "message": "GitHub user not found: fake_user_that_does_not_exist_9999",
    "path": "/api/repos/track/fake_user_that_does_not_exist_9999"
}
```

## Deploying to Render

This application is ready to be deployed to Render as a Web Service.

1. Go to [render.com](https://render.com) and sign in with GitHub.
2. Click **New +** > **Web Service** and select this repository.
3. Render should auto-detect the `render.yaml` configuration. If not, manually set:
   * **Build Command:** `./mvnw clean package -DskipTests`
   * **Start Command:** `java -jar target/github-repo-tracker-0.0.1-SNAPSHOT.jar`
4. Add `GITHUB_TOKEN` as an environment variable in Render's dashboard. 
   *(Note: You can generate a personal access token in GitHub with no scopes needed for public read-only API access. See [GitHub Docs](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) for details.)*
5. Deploy and note the live URL Render assigns (e.g., `https://github-repo-tracker-xxxx.onrender.com`).
6. Test the live URL with the example curl commands above, swapping `http://localhost:8080` for your Render URL.

## Known Limitations

* **GitHub Rate Limits:** Unauthenticated requests to the GitHub API are limited to 60 requests per hour per IP address. If you exceed this, you will receive a 429 Rate Limit Exceeded error. Set the `GITHUB_TOKEN` environment variable in production to increase this limit.
* **Pagination:** The `/users/{username}/repos` endpoint only fetches the first page of results (up to 30 repositories by default). Repositories beyond the first page are currently not tracked.
* **In-Memory Storage:** The H2 database stores data in-memory. If the application restarts, all tracked repositories will be cleared.

