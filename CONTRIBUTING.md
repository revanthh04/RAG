# Contributing to RAG Assist

We welcome and appreciate contributions to RAG Assist! Whether you are reporting a bug, proposing a new feature, or submitting code changes, please follow these guidelines to ensure a smooth and productive process.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](file:///Users/revanthh/Desktop/RAG/CODE_OF_CONDUCT.md).

## Getting Started

1. **Fork the Repository**: Create your own fork of the project on GitHub.
2. **Clone the Fork**:
   ```bash
   git clone https://github.com/your-username/RAG.git
   cd RAG
   ```
3. **Set Up the Environment**:
   - Install **Java 21** or later.
   - Set up **Maven 3.9+** (or use the included `./mvnw` wrapper).
   - Create a local `.env` file containing your `GROQ_API_KEY`.
4. **Run Locally**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Development Guidelines

- **Code Style**: Follow standard Java coding conventions. Maintain clear documentation, comments, and JavaDoc where appropriate.
- **Testing**: Ensure that all unit tests pass before making a pull request. Write new test cases in `src/test/java` for any new business logic or service enhancements.
- **Commit Messages**: Write meaningful, concise commit messages. Prefer the prefix convention, e.g., `feat: Add support for DOCX indexing` or `fix: Correct memory leak during file parser initialization`.

## Submitting Pull Requests

1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/cool-new-feature
   ```
2. Implement your changes, test them, and commit them.
3. Push the branch to your fork:
   ```bash
   git push origin feature/cool-new-feature
   ```
4. Open a Pull Request (PR) against the `master` branch of the main repository.
5. Provide a detailed summary of your changes, what problem they solve, and how they were tested.

## Questions and Support

For bugs, feedback, or feature requests, feel free to open a GitHub Issue in the repository.
