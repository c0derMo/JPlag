package de.jplag.commenthandling;

import java.io.File;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.ParsingException;
import de.jplag.SharedTokenType;
import de.jplag.Token;
import de.jplag.commentextraction.Comment;
import de.jplag.text.ParserAdapter;

public class CommentPreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(CommentPreprocessor.class);

    private final List<Comment> comments;
    private final Map<Integer, Comment> lineToComment;
    private final Map<Comment, Integer> commentStartingLines;

    public CommentPreprocessor(List<Comment> comments) {
        this.comments = comments;
        this.lineToComment = new HashMap<>();
        this.commentStartingLines = new HashMap<>();
    }

    public List<Token> processToToken() {
        ParserAdapter textParser = new ParserAdapter();
        try {
            return fixTokenPositions(textParser.parseStrings(Set.of(this.buildCommentString())));
        } catch (ParsingException e) {
            logger.error("Could not parse comments: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Token> fixTokenPositions(List<Token> tokens) {
        List<Token> fixedTokens = new ArrayList<>();
        File lastFile = null;
        Comment lastComment = null;
        for (Token token : tokens) {
            if (token.getType() == SharedTokenType.FILE_END) {
                fixedTokens.add(Token.fileEnd(lastFile));
                continue;
            }

            Comment originalComment = this.lineToComment.get(token.getLine());
            if (originalComment == null) {
                logger.warn("Original comment not found for token {}!", token);
                fixedTokens.add(token);
                continue;
            }

            if (lastFile != null && !lastFile.equals(originalComment.file())) {
                fixedTokens.add(Token.fileEnd(lastFile));
            }
            lastFile = originalComment.file();

            if (lastComment != null && !lastComment.equals(originalComment)) {
                fixedTokens.add(new Token(CommentTokenType.COMMENT_END, originalComment.file(), -1, -1, -1));
            }
            lastComment = originalComment;

            int line = originalComment.line() + token.getLine() - commentStartingLines.get(originalComment);
            int column = token.getColumn();

            if (commentStartingLines.get(originalComment) == token.getLine()) {
                column += originalComment.column() - 1;
            }

            Token newToken = new Token(token.getType(), originalComment.file(), line, column, token.getLength(), token.getSemantics());
            fixedTokens.add(newToken);
        }
        return fixedTokens;
    }

    private String buildCommentString() {
        this.commentStartingLines.clear();
        this.lineToComment.clear();
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        int line = 1;
        for (Comment comment : comments) {
            int lines = comment.content().split(System.lineSeparator(), -1).length;
            commentStartingLines.put(comment, line);
            for (int i = 0; i < lines; i++) {
                lineToComment.put(line + i, comment);
            }
            joiner.add(comment.content());
            line += lines;
        }
        return joiner.toString();
    }
}
