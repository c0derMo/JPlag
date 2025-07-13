package de.jplag.commenthandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.jplag.*;

public class CommentComparer {
    private record CommentTuple(CommentTokenMapper.CommentWithTokenInfo left, CommentTokenMapper.CommentWithTokenInfo right) {
        public CommentTuple swap() {
            return new CommentTuple(right, left);
        }
    }

    private void compareSubmissionsToBaseCode(SubmissionSet submissions, CommentTokenMapper commentTokenMapper) {
        Submission baseCodeSubmission = submissions.getBaseCode();
        for (Submission currentSubmission : submissions.getSubmissions()) {
            double threshold = 0.0;
            if (currentSubmission.hasBaseCodeComparison()) {
                threshold = currentSubmission.getBaseCodeComparison().similarity();
            }

            List<CommentTuple> similarComments = this.findSimilarComments(currentSubmission, baseCodeSubmission, commentTokenMapper, threshold);

            List<Match> baseCodeMatches = new ArrayList<>();
            for (CommentTuple tuple : similarComments) {
                commentTokenMapper.ignoreComment(tuple.left());
                baseCodeMatches.add(new Match(tuple.left().startIndex(), tuple.right().startIndex(), tuple.left().length(), tuple.right().length()));
            }

            if (currentSubmission.hasBaseCodeComparison()) {
                JPlagComparison oldBaseCodeComparison = currentSubmission.getBaseCodeComparison();
                JPlagComparison newBaseCodeComparison = new JPlagComparison(oldBaseCodeComparison.firstSubmission(),
                        oldBaseCodeComparison.secondSubmission(), oldBaseCodeComparison.matches(), oldBaseCodeComparison.ignoredMatches(),
                        baseCodeMatches);
                currentSubmission.setBaseCodeComparison(newBaseCodeComparison);
            } else {
                JPlagComparison newBaseCodeComparison = new JPlagComparison(currentSubmission, baseCodeSubmission, Collections.emptyList(),
                        Collections.emptyList(), baseCodeMatches);
                currentSubmission.setBaseCodeComparison(newBaseCodeComparison);
            }
        }
    }

    private List<CommentTuple> findSimilarComments(Submission firstSubmission, Submission secondSubmission, CommentTokenMapper commentTokenMapper,
            double threshold) {
        List<CommentTuple> similarComments = new ArrayList<>();
        List<CommentTokenMapper.CommentWithTokenInfo> firstIterator = commentTokenMapper.getComments(firstSubmission);
        List<CommentTokenMapper.CommentWithTokenInfo> secondIterator = commentTokenMapper.getComments(secondSubmission);

        boolean swapped = secondIterator.size() < firstIterator.size();
        if (swapped) {
            List<CommentTokenMapper.CommentWithTokenInfo> temp = firstIterator;
            firstIterator = secondIterator;
            secondIterator = temp;
        }

        for (CommentTokenMapper.CommentWithTokenInfo leftComment : firstIterator) {
            double highestSimilarity = 0;
            CommentTuple bestMatch = null;
            CommentTokenMapper.CommentWithTokenInfo matchingRightComment = null;
            for (CommentTokenMapper.CommentWithTokenInfo rightComment : secondIterator) {
                double similarity = 0.0;

                if (similarity >= threshold && similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                    matchingRightComment = rightComment;
                    bestMatch = new CommentTuple(leftComment, rightComment);
                }
            }

            if (bestMatch != null) {
                similarComments.add(bestMatch);
                secondIterator.remove(matchingRightComment);
            }
        }

        if (swapped) {
            return similarComments.stream().map(CommentTuple::swap).toList();
        } else {
            return similarComments;
        }
    }

    private List<Match> compareSubmissions(Submission firstSubmission, Submission secondSubmission, CommentTokenMapper commentTokenMapper,
            double threshold) {
        List<CommentTuple> similarComments = this.findSimilarComments(firstSubmission, secondSubmission, commentTokenMapper, threshold);

        List<Match> matches = similarComments.stream().map((tuple) -> {
            return new Match(tuple.left().startIndex(), tuple.right().startIndex(), tuple.left().length(), tuple.right().length());
        }).toList();

        return matches;
    }

    public JPlagResult compareCommentsAndMergeMatches(JPlagResult result) {
        long timeBeforeStartInMillis = System.currentTimeMillis();

        CommentTokenMapper commentTokenMapper = new CommentTokenMapper(result.getSubmissions(), Submission::getComments);

        boolean withBaseCode = result.getSubmissions().hasBaseCode();
        if (withBaseCode) {
            compareSubmissionsToBaseCode(result.getSubmissions(), commentTokenMapper);
        }

        List<JPlagComparison> fixedComparisons = result.getAllComparisons().stream().parallel().map(oldComparison -> {
            List<Match> commentMatches = this.compareSubmissions(oldComparison.firstSubmission(), oldComparison.secondSubmission(),
                    commentTokenMapper, oldComparison.similarity());
            return new JPlagComparison(oldComparison.firstSubmission(), oldComparison.secondSubmission(), oldComparison.matches(),
                    oldComparison.ignoredMatches(), commentMatches);
        }).toList();

        long durationInMillis = System.currentTimeMillis() - timeBeforeStartInMillis;

        return new JPlagResult(fixedComparisons, result.getSubmissions(), result.getDuration() + durationInMillis, result.getOptions());
    }
}
