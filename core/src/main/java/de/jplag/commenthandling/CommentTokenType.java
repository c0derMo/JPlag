package de.jplag.commenthandling;

import de.jplag.TokenType;

public enum CommentTokenType implements TokenType {
    COMMENT_END("END_OF_COMMENT");

    private final String description;

    CommentTokenType(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean isExcludedFromMatching() {
        return true;
    }
}