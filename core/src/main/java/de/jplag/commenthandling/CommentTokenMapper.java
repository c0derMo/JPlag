package de.jplag.commenthandling;

import java.util.*;

import de.jplag.SharedTokenType;
import de.jplag.Submission;
import de.jplag.SubmissionSet;
import de.jplag.Token;
import de.jplag.comparison.TokenListSupplier;

public class CommentTokenMapper {
    public record CommentWithTokenInfo(String content, int startIndex, int length, Submission submission) {
    }

    private final Map<Submission, List<CommentWithTokenInfo>> comments;
    private final TokenListSupplier tokenListSupplier;

    public CommentTokenMapper(SubmissionSet submissionSet, TokenListSupplier tokenListSupplier) {
        this.comments = new HashMap<>();
        this.tokenListSupplier = tokenListSupplier;

        for (Submission submission : submissionSet.getSubmissions()) {
            this.mergeCommentTokens(submission);
        }
        if (submissionSet.hasBaseCode()) {
            this.mergeCommentTokens(submissionSet.getBaseCode());
        }
    }

    private void mergeCommentTokens(Submission submission) {
        List<CommentWithTokenInfo> commentsOfThisSubmission = new ArrayList<>();
        List<Token> tokens = this.tokenListSupplier.getTokenList(submission);

        StringJoiner currentComment = new StringJoiner(" ");
        int lastStart = -1;
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.getType() == SharedTokenType.FILE_END || token.getType() == CommentTokenType.COMMENT_END) {
                if (currentComment.length() > 0 && index > lastStart) {
                    commentsOfThisSubmission.add(new CommentWithTokenInfo(currentComment.toString(), lastStart, index - lastStart, submission));
                }
                lastStart = -1;
                currentComment = new StringJoiner(" ");
            } else {
                if (lastStart == -1) {
                    lastStart = index;
                }
                currentComment.add(token.getType().getDescription());
            }
        }

        this.comments.put(submission, commentsOfThisSubmission);
    }

    public List<CommentWithTokenInfo> getComments(Submission submission) {
        return new ArrayList<>(this.comments.get(submission));
    }

    public void ignoreComment(CommentWithTokenInfo comment) {
        this.comments.get(comment.submission).remove(comment);
    }
}
