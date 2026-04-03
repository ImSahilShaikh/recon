# Recon - AI Code Reviewer

Recon is an intelligent code review assistant powered by Spring AI. It automatically analyzes code changes and provides actionable feedback on code quality, security, performance, and more.

## 🚀 Features

- **Automated Code Review**: Analyzes diffs and provides structured feedback.
- **GitHub Integration**: Automatically fetches PR diffs and posts inline comments directly on the specific lines where issues occur.
- **Multiple AI Models**: Support for Gemini, OpenAI, and Anthropic.
- **Batch & Individual Posting**: Robust review posting that ensures summary feedback is always delivered, even if some inline comments have invalid line numbers.
- **Swagger/OpenAPI**: Built-in interactive documentation for easy API testing.

## 🛠 Prerequisites

- **Java 21**
- **Gradle**
- **API Keys**: Gemini (default), OpenAI, or Anthropic.
- **GitHub Token**: Personal Access Token (PAT) with `repo` scope to post comments.

## ⚙️ Configuration

Configure the application in `src/main/resources/application.yaml` or via environment variables:

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_KEY}
github:
  token: ${GITHUB_TOKEN}

app:
  ai:
    active-model: gemini # options: gemini, openai, anthropic
```

## 📦 Installation & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/recon.git
   cd recon
   ```
2. Build and Run:
   ```bash
   ./gradlew bootRun
   ```

The server will start on port `8082`.

## 📖 Documentation & API

### Interactive API (Swagger UI)
Once the app is running, you can access the interactive Swagger documentation to test the API directly:
👉 [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

### Key Endpoints

#### POST `/api/review`
Triggers a new code review.

**Request Body (GitHub PR):**
```json
{
  "githubPrUrl": "https://github.com/owner/repo/pull/123"
}
```

**Request Body (Direct Diff):**
```json
{
  "diff": "--- a/Main.java\n+++ b/Main.java\n@@ -1,1 +1,1 @@\n-old line\n+new line"
}
```

## 🏗 Project Structure

- `src/main/java/dev/scout/recon/config`: AI and Application configuration.
- `src/main/java/dev/scout/recon/controller`: REST API endpoints.
- `src/main/java/dev/scout/recon/service`: 
    - `ReviewService`: Orchestrates the AI analysis.
    - `GitHubScmService`: Handles GitHub API interaction (diff fetching & comment posting).
- `src/main/java/dev/scout/recon/model`: POJOs for API requests and GitHub responses.

## 📄 License

This project is licensed under the [MIT License](LICENSE).
