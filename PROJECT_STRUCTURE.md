# Project Structure

This document outlines the directory structure of the RAG (Retrieval-Augmented Generation) Java Application.

```text
RAG/
├── .git/                        # Git version control files
├── .mvn/                        # Maven wrapper configuration files
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── rag/
│   │   │               ├── service/
│   │   │               │   ├── PdfService.java    # PDF text parsing & chunking service
│   │   │               │   └── RagService.java    # Embedding generation, vector database & chat LLM integration
│   │   │               ├── ui/
│   │   │               │   └── ChatView.java      # Vaadin UI interface layout & reactive interactions
│   │   │               └── RagApplication.java    # Spring Boot application entry point
│   │   └── resources/
│   │       ├── static/
│   │       │   └── styles.css                     # Custom CSS (Claude Ivory design tokens)
│   │       └── application.properties             # Spring Boot properties (port mapping, Vaadin options)
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── rag/
│                       └── RagApplicationTests.java # Core application test suite
├── Dockerfile                   # Multi-stage production build configuration
├── pom.xml                      # Maven project dependencies and plugin configuration
├── README.md                    # Main project documentation & Hugging Face deployment metadata
├── CONTRIBUTING.md              # Guidelines for contributing to the repository
├── CHANGELOG.md                 # Log of changes and project history
├── CODE_OF_CONDUCT.md           # Code of conduct guidelines for contributors
├── .env                         # Local environment configuration (API keys, gitignored)
├── .gitignore                   # Files and directories ignored by Git
└── .gitattributes               # Repository-wide git settings
```

## Key Files Summary

1. **[`RagApplication.java`](file:///Users/revanthh/Desktop/RAG/src/main/java/com/example/rag/RagApplication.java)**:
   The standard Spring Boot entry point. Bootstraps the application context.

2. **[`PdfService.java`](file:///Users/revanthh/Desktop/RAG/src/main/java/com/example/rag/service/PdfService.java)**:
   Extracts text using Apache PDFBox 3.x and processes them into chunks of 1000 characters with a 100-character overlap using LangChain4j's `DocumentSplitters.recursive()`.

3. **[`RagService.java`](file:///Users/revanthh/Desktop/RAG/src/main/java/com/example/rag/service/RagService.java)**:
   Manages embedding generation via quantized local models (`AllMiniLmL6V2QuantizedEmbeddingModel`), stores embeddings locally in an `InMemoryEmbeddingStore` persisted to `vector_store.json`, caches uploaded files in `uploaded_files.txt`, and interfaces with the Groq API (using OpenAiChatModel endpoints) to generate RAG-infused responses.

4. **[`ChatView.java`](file:///Users/revanthh/Desktop/RAG/src/main/java/com/example/rag/ui/ChatView.java)**:
   A reactive Vaadin frontend that binds UI elements (file uploader, input box, API key password field, clear database actions) directly to the PDF and RAG services.

5. **[`styles.css`](file:///Users/revanthh/Desktop/RAG/src/main/resources/static/styles.css)**:
   A bespoke custom style sheet that overrides default Vaadin themes to offer a Claude-inspired elegant dark-bordered ivory letterpress aesthetic.
