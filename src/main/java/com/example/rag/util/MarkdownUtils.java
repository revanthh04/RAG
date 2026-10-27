package com.example.rag.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownUtils {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    /**
     * Converts markdown text to clean, sanitized HTML for rich text rendering in the chat UI.
     * @param markdown The raw markdown response from the LLM.
     * @return Formatted HTML string.
     */
    public static String toHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        try {
            Node document = PARSER.parse(markdown);
            return RENDERER.render(document);
        } catch (Exception e) {
            // Safe fallback
            return markdown
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
                    .replaceAll("\\*(.*?)\\*", "<em>$1</em>")
                    .replace("\n", "<br>");
        }
    }
}
