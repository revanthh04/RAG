package com.example.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class PdfService {

    /**
     * Extracts text from a PDF input stream and splits it into semantic chunks (segments).
     * @param inputStream The input stream of the uploaded PDF file.
     * @param fileName The name of the file (added as metadata).
     * @return List of TextSegment chunks.
     * @throws IOException If parsing the PDF fails.
     */
    public List<TextSegment> extractAndSplit(InputStream inputStream, String fileName) throws IOException {
        // Read the bytes from InputStream
        byte[] bytes = inputStream.readAllBytes();
        
        String extractedText;
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("No readable text found in PDF: " + fileName);
        }

        // Create LangChain4j Document object with metadata
        Metadata metadata = Metadata.from("file_name", fileName);
        Document langchainDocument = Document.from(extractedText, metadata);

        // Split document into chunks (recursive splitter: 1000 characters chunk size, 100 characters overlap)
        DocumentSplitter splitter = DocumentSplitters.recursive(1000, 100);
        return splitter.split(langchainDocument);
    }
}
