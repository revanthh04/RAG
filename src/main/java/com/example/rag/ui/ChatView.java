package com.example.rag.ui;

import com.example.rag.service.PdfService;
import com.example.rag.service.RagService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route("")
@StyleSheet("styles.css")
public class ChatView extends HorizontalLayout {

    private final PdfService pdfService;
    private final RagService ragService;

    private VerticalLayout chatFeed;
    private Scroller chatScroller;
    private TextField messageInput;
    private Button sendButton;
    private VerticalLayout fileListContainer;
    private PasswordField apiKeyField;
    private Button clearDbButton;

    // Cache uploaded files text content for fast summarization in session
    private final Map<String, String> documentTexts = new HashMap<>();

    @Autowired
    public ChatView(PdfService pdfService, RagService ragService) {
        this.pdfService = pdfService;
        this.ragService = ragService;

        // Configure full screen main layout
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        // Build UI Components
        VerticalLayout sidebar = buildSidebar();
        VerticalLayout chatArea = buildChatArea();

        add(sidebar, chatArea);

        // Load initially indexed files
        updateFileList();

        // Print initial greeting
        addBotMessage("Hello! I am your Java PDF Chatbot. Upload PDF documents in the sidebar, and I'll answer questions based on their content.");
    }

    private VerticalLayout buildSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("340px");
        sidebar.setHeightFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(true);
        sidebar.addClassName("claude-sidebar");

        // Header Title
        H3 title = new H3("Claude PDF Workspace");
        Span subtitle = new Span("Retrieval-Augmented Generation (RAG)");
        subtitle.addClassName("subtitle");
        sidebar.add(title, subtitle);

        // Groq API Key Setup
        apiKeyField = new PasswordField("Groq API Key");
        apiKeyField.setWidthFull();
        apiKeyField.setPlaceholder("gsk_...");
        apiKeyField.addClassName("claude-input");
        if (ragService.isApiKeyConfigured()) {
            apiKeyField.setValue("••••••••••••••••••••");
        }
        apiKeyField.addValueChangeListener(event -> {
            String val = event.getValue();
            if (val != null && !val.trim().isEmpty() && !val.contains("•")) {
                ragService.updateApiKey(val.trim());
                showNotification("API Key updated!", NotificationVariant.LUMO_SUCCESS);
            }
        });
        sidebar.add(apiKeyField);

        // PDF Upload component
        H4 uploadTitle = new H4("Upload PDFs");
        
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/pdf");
        upload.setWidthFull();
        upload.setMaxFiles(10);
        
        Span uploadHint = new Span("Drag PDFs here or click to upload");
        uploadHint.getStyle().set("font-size", "0.85rem").set("color", "var(--color-stone-gray)");
        Button selectFilesBtn = new Button("Select Files", new Icon(VaadinIcon.UPLOAD));
        selectFilesBtn.addClassName("claude-btn-secondary");
        upload.setUploadButton(selectFilesBtn);
        upload.setDropLabel(uploadHint);

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            try {
                InputStream is = buffer.getInputStream(fileName);
                
                // Show uploading message
                showNotification("Indexing " + fileName + "...", NotificationVariant.LUMO_PRIMARY);
                
                // Extract and Split
                List<TextSegment> segments = pdfService.extractAndSplit(is, fileName);
                
                // Index inside RAG Service
                ragService.indexSegments(segments, fileName);

                // Cache the full text for summarization
                StringBuilder fullTextBuilder = new StringBuilder();
                for (TextSegment seg : segments) {
                    fullTextBuilder.append(seg.text()).append("\n");
                }
                documentTexts.put(fileName, fullTextBuilder.toString());

                showNotification("Successfully indexed " + fileName, NotificationVariant.LUMO_SUCCESS);
                
                // Refresh Sidebar Files List
                updateFileList();
            } catch (Exception e) {
                showNotification("Error parsing PDF " + fileName + ": " + e.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        sidebar.add(uploadTitle, upload);

        // Document List Header
        H4 docListTitle = new H4("Indexed Documents");
        sidebar.add(docListTitle);

        // Document List container
        fileListContainer = new VerticalLayout();
        fileListContainer.setPadding(false);
        fileListContainer.setSpacing(true);
        fileListContainer.setWidthFull();
        fileListContainer.getStyle().set("max-height", "220px").set("overflow-y", "auto");
        sidebar.add(fileListContainer);

        // Clear Database Button
        clearDbButton = new Button("Clear Database", new Icon(VaadinIcon.TRASH));
        clearDbButton.addClassName("claude-btn-secondary");
        clearDbButton.setWidthFull();
        clearDbButton.getStyle().set("margin-top", "auto");
        clearDbButton.addClickListener(event -> {
            ragService.clearDatabase();
            documentTexts.clear();
            updateFileList();
            chatFeed.removeAll();
            addBotMessage("Database cleared. Please upload PDFs to chat.");
            showNotification("Vector Database cleared successfully.", NotificationVariant.LUMO_SUCCESS);
        });
        sidebar.add(clearDbButton);

        return sidebar;
    }

    private VerticalLayout buildChatArea() {
        VerticalLayout chatArea = new VerticalLayout();
        chatArea.setSizeFull();
        chatArea.setPadding(true);
        chatArea.setSpacing(true);
        chatArea.addClassName("claude-main-area");

        // Top bar details
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.addClassName("claude-top-bar");
        Span headerText = new Span("Chat Session");
        topBar.add(headerText);
        chatArea.add(topBar);

        // Chat Feed
        chatFeed = new VerticalLayout();
        chatFeed.setWidthFull();
        chatFeed.setPadding(false);
        chatFeed.setSpacing(true);

        // Scrollable Chat Feed Wrapper
        chatScroller = new Scroller(chatFeed);
        chatScroller.setSizeFull();
        chatArea.add(chatScroller);

        // Message Input Row
        HorizontalLayout inputRow = new HorizontalLayout();
        inputRow.setWidthFull();
        inputRow.setSpacing(true);
        inputRow.getStyle().set("padding-top", "10px");

        messageInput = new TextField();
        messageInput.setWidthFull();
        messageInput.setPlaceholder("Ask a question about your documents...");
        messageInput.addKeyPressListener(Key.ENTER, event -> sendMessage());
        messageInput.addClassName("claude-input");

        sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addClassName("claude-btn-primary");
        sendButton.addClickListener(event -> sendMessage());

        inputRow.add(messageInput, sendButton);
        chatArea.add(inputRow);

        return chatArea;
    }

    private void sendMessage() {
        String query = messageInput.getValue();
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        // Add user message to feed
        addUserMessage(query);
        messageInput.clear();

        // Disable input during processing
        toggleLoading(true);

        // Run chat response asynchronously in background
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                String response = ragService.askQuestion(query);
                ui.access(() -> {
                    addBotMessage(response);
                    toggleLoading(false);
                });
            } catch (Exception e) {
                ui.access(() -> {
                    addBotMessage("Error: " + e.getMessage());
                    toggleLoading(false);
                });
            }
        }));
    }

    private void toggleLoading(boolean loading) {
        messageInput.setEnabled(!loading);
        sendButton.setEnabled(!loading);
    }

    private void addUserMessage(String message) {
        Div bubble = new Div();
        bubble.setText(message);
        bubble.addClassName("chat-bubble-user");

        chatFeed.add(bubble);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        Div bubble = new Div();
        // Handle markdown/newlines nicely in chat bubbles
        Div content = new Div();
        content.getStyle().set("white-space", "pre-wrap");
        content.setText(message);
        bubble.add(content);
        bubble.addClassName("chat-bubble-bot");

        chatFeed.add(bubble);
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatScroller.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    private void updateFileList() {
        fileListContainer.removeAll();
        List<String> files = ragService.getUploadedFilesList();

        if (files == null || files.isEmpty()) {
            Span empty = new Span("No documents indexed yet.");
            empty.getStyle().set("font-size", "0.85rem").set("color", "var(--color-stone-gray)");
            fileListContainer.add(empty);
            return;
        }

        for (String file : files) {
            HorizontalLayout item = new HorizontalLayout();
            item.setWidthFull();
            item.setJustifyContentMode(JustifyContentMode.BETWEEN);
            item.setAlignItems(Alignment.CENTER);
            item.addClassName("claude-file-item");

            Span name = new Span(file);
            name.getStyle()
                    .set("font-size", "0.8rem")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("white-space", "nowrap")
                    .set("max-width", "180px");

            Button summarizeBtn = new Button(new Icon(VaadinIcon.FILE_TEXT));
            summarizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            summarizeBtn.setTooltipText("Summarize this document");
            summarizeBtn.getStyle().set("color", "var(--color-charcoal)");
            
            summarizeBtn.addClickListener(event -> {
                String fullText = documentTexts.get(file);
                if (fullText == null || fullText.isEmpty()) {
                    showNotification("Document content not cached in session. Summarization needs re-uploading.", NotificationVariant.LUMO_WARNING);
                    return;
                }
                
                addUserMessage("Summarize document: " + file);
                toggleLoading(true);
                
                getUI().ifPresent(ui -> ui.access(() -> {
                    try {
                        String summary = ragService.summarizeText(fullText);
                        ui.access(() -> {
                            addBotMessage("**Summary of " + file + "**:\n\n" + summary);
                            toggleLoading(false);
                        });
                    } catch (Exception e) {
                        ui.access(() -> {
                            addBotMessage("Summarization error: " + e.getMessage());
                            toggleLoading(false);
                        });
                    }
                }));
            });

            item.add(name, summarizeBtn);
            fileListContainer.add(item);
        }
    }

    private void showNotification(String text, NotificationVariant variant) {
        Notification notification = Notification.show(text, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
