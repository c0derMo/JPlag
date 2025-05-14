package de.jplag.java.commentExtraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentExtractor {

    private static final List<String> NO_COMMENT_ENVIRONMENTS = List.of("\"", "'");
    private static final List<String> LINE_COMMENTS = List.of("//");
    private static final List<List<String>> BLOCK_COMMENTS = List.of(List.of("/*", "*/"));
    private static final List<Character> ESCAPE_CHARACTERS = List.of('\\');

    private String remainingContent;
    private List<String> comments;

    public CommentExtractor(String fileContent) {
        this.remainingContent = fileContent;
        this.comments = new ArrayList<>();
    }

    private void match(String expected) {
        if (remainingContent.startsWith(expected)) {
            this.advance(expected.length());
        } else {
            throw new RuntimeException("Matched incorrectly");
        }
    }

    private String advance(int length) {
        String advancedBy = remainingContent.substring(0, length);
        remainingContent = remainingContent.substring(length);
        return advancedBy;
    }

    public List<String> extract() {
        while (!remainingContent.isEmpty()) {
            this.parseAny();
        }
        return comments;
    }

    private void parseAny() {
        if (this.parseEscapedCharacter().isPresent()) {
            return;
        }

        for (String environment : NO_COMMENT_ENVIRONMENTS) {
            if (remainingContent.startsWith(environment)) {
                this.parseNoCommentEnvironment(environment);
                return;
            }
        }

        for (String lineComment : LINE_COMMENTS) {
            if (remainingContent.startsWith(lineComment)) {
                this.match(lineComment);
                this.parseLineComment();
                return;
            }
        }

        for (List<String> blockComment : BLOCK_COMMENTS) {
            if (remainingContent.startsWith(blockComment.get(0))) {
                this.parseBlockComment(blockComment);
                return;
            }
        }

        this.advance(1);
    }

    private Optional<String> parseEscapedCharacter() {
        for (char escapeCharacter : ESCAPE_CHARACTERS) {
            if (remainingContent.startsWith(String.valueOf(escapeCharacter))) {
                return Optional.of(this.advance(2));
            }
        }
        return Optional.empty();
    }

    private void parseNoCommentEnvironment(String environment) {
        parseEnvironment(environment, environment);
    }

    private void parseLineComment() {
        StringBuilder comment = new StringBuilder();
        while (!this.remainingContent.startsWith("\n") && !this.remainingContent.isEmpty()) {
            comment.append(this.remainingContent.charAt(0));
            this.advance(1);
        }
        this.comments.add("LINE COMMENT: " + comment.toString());
    }

    private void parseBlockComment(List<String> blockComment) {
        String comment = this.parseEnvironment(blockComment.get(0), blockComment.get(1));

        this.comments.add("BLOCK COMMENT: " + comment);
    }

    private String parseEnvironment(String start, String end) {
        this.match(start);
        StringBuilder environmentContent = new StringBuilder();

        while (!this.remainingContent.isEmpty()) {
            if (this.remainingContent.startsWith(end)) {
                this.match(end);
                break;
            }

            Optional<String> escaped = parseEscapedCharacter();
            if (escaped.isPresent()) {
                environmentContent.append(escaped.get());
                continue;
            }

            environmentContent.append(this.advance(1));
        }

        return environmentContent.toString();
    }
}