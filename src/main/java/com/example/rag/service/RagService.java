package com.example.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final String STORE_FILE_NAME = "vector_store.json";
    private static final Path STORE_PATH = Paths.get(STORE_FILE_NAME);
    private static final Path FILES_PATH = Paths.get("uploaded_files.txt");

    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    private ChatLanguageModel chatModel;
    private String groqApiKey;
    private List<String> uploadedFilesList = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Load API Key from .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        groqApiKey = dotenv.get("GROQ_API_KEY");
        if (groqApiKey == null || groqApiKey.trim().isEmpty() || "your_groq_api_key_here".equals(groqApiKey)) {
            groqApiKey = System.getenv("GROQ_API_KEY");
        }

        // Initialize Local Embedding Model (all-MiniLM-L6-v2)
        // This runs completely locally in Java via ONNX runtime
        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

        // Initialize persistent local embedding store
        loadEmbeddingStore();

        // Initialize Chat LLM
        initializeChatModel();
    }

    private void initializeChatModel() {
        if (groqApiKey == null || groqApiKey.trim().isEmpty() || "your_groq_api_key_here".equals(groqApiKey)) {
            // Placeholder model to avoid startup crash if API key is not configured yet
            chatModel = new ChatLanguageModel() {
                @Override
                public Response<AiMessage> generate(List<dev.langchain4j.data.message.ChatMessage> messages) {
                    return Response.from(AiMessage.from("Error: GROQ_API_KEY is not configured. Please add your key to the .env file."));
                }
            };
        } else {
            chatModel = OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(groqApiKey)
                    .modelName("llama-3.1-8b-instant")
                    .temperature(0.0)
                    .build();
        }
    }

    /**
     * Loads the embedding store from disk if it exists, otherwise creates a new one.
     */
    @SuppressWarnings("unchecked")
    private void loadEmbeddingStore() {
        if (Files.exists(STORE_PATH)) {
            try {
                String json = Files.readString(STORE_PATH);
                embeddingStore = InMemoryEmbeddingStore.fromJson(json);
                
                // Load uploaded filenames cache
                uploadedFilesList.clear();
                if (Files.exists(FILES_PATH)) {
                    uploadedFilesList.addAll(Files.readAllLines(FILES_PATH));
                }
            } catch (Exception e) {
                System.err.println("Could not load embedding store, creating new one: " + e.getMessage());
                embeddingStore = new InMemoryEmbeddingStore<>();
            }
        } else {
            embeddingStore = new InMemoryEmbeddingStore<>();
        }
    }

    /**
     * Saves the current embedding store state to disk.
     */
    private void saveEmbeddingStore() {
        try {
            String json = embeddingStore.serializeToJson();
            Files.writeString(STORE_PATH, json);
            
            // Save uploaded filenames cache
            Files.writeString(FILES_PATH, String.join("\n", uploadedFilesList));
        } catch (IOException e) {
            System.err.println("Failed to save embedding store: " + e.getMessage());
        }
    }

    /**
     * Re-initializes the chat model when a new API key is provided at runtime.
     */
    public void updateApiKey(String newApiKey) {
        this.groqApiKey = newApiKey;
        initializeChatModel();
    }

    /**
     * Clears all embeddings and resets the database.
     */
    public void clearDatabase() {
        embeddingStore = new InMemoryEmbeddingStore<>();
        uploadedFilesList.clear();
        try {
            Files.deleteIfExists(STORE_PATH);
            Files.deleteIfExists(FILES_PATH);
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Adds PDF text chunks to the embedding store and embeds them.
     */
    public void indexSegments(List<TextSegment> segments, String fileName) {
        if (segments == null || segments.isEmpty()) {
            return;
        }

        // Generate embeddings and store them
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        if (!uploadedFilesList.contains(fileName)) {
            uploadedFilesList.add(fileName);
        }

        // Save progress to disk
        saveEmbeddingStore();
    }

    /**
     * Answers a user query by retrieving relevant text segments from the PDFs.
     */
    public String askQuestion(String question) {
        if (groqApiKey == null || groqApiKey.trim().isEmpty() || "your_groq_api_key_here".equals(groqApiKey)) {
            return "Error: GROQ_API_KEY is missing. Please configure your API key in the sidebar.";
        }

        if (uploadedFilesList.isEmpty()) {
            return "No documents uploaded yet. Please upload PDF files to start.";
        }

        // 1. Vectorize the question
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 2. Query the vector store for top 5 matches
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(questionEmbedding, 5, 0.4);

        if (matches == null || matches.isEmpty()) {
            return "I couldn't find any relevant sections in the uploaded PDFs to answer your question.";
        }

        // 3. Assemble context from matches
        String context = matches.stream()
                .map(match -> {
                    String src = match.embedded().metadata().getString("file_name");
                    return "[" + src + "]:\n" + match.embedded().text();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. Construct the prompt with strict instruction
        String systemPrompt = "You are a helpful assistant. You must answer the user's question strictly based on the provided PDF context below. " +
                "If the context is not sufficient to answer, say 'I cannot find the answer in the uploaded documents.' " +
                "Do not make up facts or use outside knowledge. Cite the source files (e.g. [filename.pdf]) where appropriate.\n\n" +
                "Context:\n" + context;

        // 5. Generate response using Groq Chat Model
        try {
            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(question)
            );
            return response.content().text();
        } catch (Exception e) {
            return "Error calling Groq API: " + e.getMessage();
        }
    }

    /**
     * Summarizes the PDF text directly without RAG retrieval.
     */
    public String summarizeText(String text) {
        if (groqApiKey == null || groqApiKey.trim().isEmpty() || "your_groq_api_key_here".equals(groqApiKey)) {
            return "Error: GROQ_API_KEY is missing. Please configure your API key in the sidebar.";
        }

        if (text == null || text.trim().isEmpty()) {
            return "No text available to summarize.";
        }

        // Truncate to protect model limits if too large (approx 80k characters is safe)
        String contentToSummarize = text;
        if (text.length() > 80_000) {
            contentToSummarize = text.substring(0, 80_000) + "\n\n[Content truncated due to size limits]";
        }

        String systemPrompt = "Summarize the following document content clearly and concisely in bullet points. Focus on key themes, summaries, and take-aways.";
        
        try {
            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(contentToSummarize)
            );
            return response.content().text();
        } catch (Exception e) {
            return "Error generating summary: " + e.getMessage();
        }
    }

    public List<String> getUploadedFilesList() {
        return uploadedFilesList;
    }

    public boolean isApiKeyConfigured() {
        return groqApiKey != null && !groqApiKey.trim().isEmpty() && !"your_groq_api_key_here".equals(groqApiKey);
    }
}
