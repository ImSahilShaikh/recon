# Recon - AI Code Reviewer

Recon is an intelligent code review assistant powered by Spring AI. It automatically analyzes code changes and provides actionable feedback on code quality, security, performance, and more.

## Features

- **Automated Code Review**: Analyzes diffs and provides structured feedback.
- **Multiple AI Models**: Configurable to use different AI providers (e.g., Gemini, OpenAI, Anthropic).
- **GitHub Integration**: Capable of integrating with GitHub to fetch PRs/commits (configuration provided).
- **Structured Feedback**: Returns reviews in a consistent JSON format covering:
    - Code Quality & Best Practices
    - Security Vulnerabilities
    - Performance Issues
    - Bug Detection
    - Test Coverage

## Getting Started

### Prerequisites

- Java 21
- Gradle
- API Keys for your chosen AI provider (e.g., Gemini, OpenAI, Anthropic)
- GitHub Token (if using GitHub integration)

### Configuration

Configure the application in `src/main/resources/application.yaml` or via environment variables.

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_KEY}
        chat:
          options:
            model: gemini-2.5-flash
github:
  token: ${GITHUB_TOKEN}

app:
  ai:
    active-model: gemini # or openai, anthropic
```

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-username/recon.git
    cd recon
    ```

2.  Build the project:
    ```bash
    ./gradlew build
    ```

### Running the Application

Run the application using Gradle:

```bash
./gradlew bootRun
```

The server will start on port `8082` (default).

## Usage

Send a POST request to the review endpoint with the code diff you want to analyze.

*(Note: API endpoints and usage examples would be added here based on the `ReviewController` implementation)*

## Project Structure

-   `src/main/java/dev/scout/recon`: Main source code.
    -   `config`: Configuration classes (AI setup, App properties).
    -   `controller`: REST controllers for handling review requests.
    -   `model`: Data models for requests, responses, and comments.
    -   `service`: Business logic for AI review and SCM integration.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

[License Name]
