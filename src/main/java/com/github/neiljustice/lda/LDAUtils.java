package com.github.neiljustice.lda;

import com.github.neiljustice.lda.topic.TermScore;
import com.github.neiljustice.lda.topic.Topic;
import com.github.neiljustice.lda.util.BiDirectionalLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * LDA utilities
 */
public class LDAUtils {

  private LDAUtils() {
    // Prevent static class instantiation.
  }

  /**
   * Find the geometric mean of a 2D matrix.
   */
  public static double[] geometricMean(double[][] matrix) {
    final int height = matrix.length;
    final int width = matrix[0].length;

    final double[] geometricMean = new double[height];

    for (int i = 0; i < height; i++) {
      double sumlog = 0d;
      for (int j = 0; j < width; j++) {
        sumlog += Math.log(matrix[i][j]);
      }
      geometricMean[i] = Math.exp(sumlog / width);
    }

    return geometricMean;
  }


  /**
   * As laid out in Blei and Lafferty, 2009.  sorts words in topics by
   * phi * log (phi) / geometric mean(phi)
   * and defines topics by their top N words.
   *
   * @param phi        2D matrix where each cell represents the probability that a word will appear in a topic.
   * @param dictionary dictionary for conversion from word indexes back to strings.
   * @param topN       the top N words to return per topic. If this is <= 0, all words are returned.
   */
  public static List<Topic> termScore(double[][] phi, BiDirectionalLookup<String> dictionary, int topN) {
    final int wordCount = phi.length;
    final int topicCount = phi[0].length;
    final List<Topic> topics = new ArrayList<>(topicCount);
    final double[] geometricMean = geometricMean(phi);
    topN = topN > 0 ? Math.min(topN, wordCount) : wordCount;

    for (int topic = 0; topic < topicCount; topic++) {
      final TreeSet<TermScore> termScores = new TreeSet<>(TermScore.COMPARATOR);
      for (int word = 0; word < wordCount; word++) {
        final double score = phi[word][topic] * Math.log(phi[word][topic] / geometricMean[word]);
        termScores.add(new TermScore(dictionary.getToken(word), score));
      }
      topics.add(new Topic(topic, termScores.stream().limit(topN).collect(Collectors.toList())));
    }

    return topics;
  }
}