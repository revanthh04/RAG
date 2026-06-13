# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-06-13

### Changed
- Updated the main application header title in the top-left sidebar from "Claude PDF Workspace" to **"RAG Assist"** to match repository refactoring guidelines.

## [1.0.0] - 2026-06-13

### Added
- Complete migration of Python-based PDF RAG system to a **100% Java** application.
- Standardized Spring Boot 3.5.0 and Vaadin 24.x web framework integration.
- Integrated **LangChain4j** for LLM orchestration and embedding storage.
- Added **`AllMiniLmL6V2QuantizedEmbeddingModel`** to compute text embeddings locally within the JVM using an ONNX runtime.
- Integrated **Apache PDFBox 3.x** for fast, local PDF text parsing.
- Created **`InMemoryEmbeddingStore`** persisted as `vector_store.json` on disk to avoid external database dependencies.
- Styled custom layout with an ivory-based **"Claude Warm Letterpress"** custom stylesheet.
- Configured dynamic port binding to run on port `7860` for Docker environment mapping.
- Added `Dockerfile` with multi-stage compilation + Maven production profile configurations.
- Pushed code and metadata configured for deployment on **Hugging Face Spaces**.
