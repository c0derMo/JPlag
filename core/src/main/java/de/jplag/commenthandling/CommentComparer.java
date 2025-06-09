package de.jplag.commenthandling;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.*;
import de.jplag.comparison.GreedyStringTiling;
import de.jplag.comparison.SubmissionTuple;
import de.jplag.comparison.TokenValueMapper;
import de.jplag.options.JPlagOptions;

public class CommentComparer {
    private final Logger logger = LoggerFactory.getLogger(CommentComparer.class);

    private final JPlagOptions options;

    public CommentComparer(JPlagOptions options) {
        this.options = options;
    }

    private Optional<JPlagComparison> compareSubmissions(GreedyStringTiling algorithm, SubmissionTuple tuple) {
        JPlagComparison comp = algorithm.compare(tuple.left(), tuple.right());
        return Optional.of(comp);
    }

    private List<SubmissionTuple> extractComparisonTuples(List<JPlagComparison> comparisons) {
        List<SubmissionTuple> tuples = new ArrayList<>();

        for (JPlagComparison comparison : comparisons) {
            tuples.add(new SubmissionTuple(comparison.firstSubmission(), comparison.secondSubmission()));
        }

        return tuples;
    }

    public JPlagResult compareAndMergeCommentsInto(JPlagResult oldResult) {
        long timeBeforeStartInMillis = System.currentTimeMillis();

        // TODO: Base code?

        oldResult.getSubmissions().getSubmissions().stream().parallel().forEach(Submission::mergeCommentsIntoTokenList);

        TokenValueMapper tokenValueMapper = new TokenValueMapper(oldResult.getSubmissions(), Submission::getComments);
        GreedyStringTiling algorithm = new GreedyStringTiling(options, tokenValueMapper, Submission::getComments);

        List<SubmissionTuple> tuples = this.extractComparisonTuples(oldResult.getAllComparisons());

        Map<SubmissionTuple, JPlagComparison> comparisons = tuples.stream().parallel().flatMap(tuple -> {
            return this.compareSubmissions(algorithm, tuple).stream().map(comparison -> new AbstractMap.SimpleEntry<>(tuple, comparison));
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        List<JPlagComparison> mergedComparisons = new ArrayList<>();
        for (JPlagComparison oldComparison : oldResult.getAllComparisons()) {
            JPlagComparison commentComparison = comparisons
                    .get(new SubmissionTuple(oldComparison.firstSubmission(), oldComparison.secondSubmission()));
            if (commentComparison == null) {
                logger.warn("Could not find comment comparison for {}", oldComparison);
                mergedComparisons.add(oldComparison);
                continue;
            }

            List<Match> mergedMatches = new ArrayList<>();
            mergedMatches.addAll(oldComparison.matches());
            mergedMatches.addAll(commentComparison.matches().stream()
                    .map(match -> new Match(match.startOfFirst() + oldComparison.firstSubmission().getNumberOfTokens(),
                            match.startOfSecond() + oldComparison.secondSubmission().getNumberOfTokens(), match.length()))
                    .toList());

            JPlagComparison mergedComparison = new JPlagComparison(oldComparison.firstSubmission(), oldComparison.secondSubmission(), mergedMatches,
                    oldComparison.ignoredMatches(), commentComparison.getNumberOfMatchedTokens());

            mergedComparisons.add(mergedComparison);
        }

        long durationInMillis = System.currentTimeMillis() - timeBeforeStartInMillis;

        return new JPlagResult(mergedComparisons, oldResult.getSubmissions(), oldResult.getDuration() + durationInMillis, oldResult.getOptions());
    }
}
