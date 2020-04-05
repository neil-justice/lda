package com.github.neiljustice.lda;

import com.github.neiljustice.lda.topic.TermScore;
import com.github.neiljustice.lda.topic.Topic;
import com.github.neiljustice.lda.util.BiDirectionalLookup;
import com.github.neiljustice.lda.util.IndexComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * class for static LDA utilities
 */
public class LDAUtils {

  // finds the geometric mean of a matrix
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

  // as laid out in Blei and Lafferty, 2009.  sorts words in topics by
  // phi * log (phi) / geometric mean(phi)
  // and defines topics by their top 10 words.
  public static List<Topic> termScore(double[][] phi, BiDirectionalLookup<String> dictionary, int topN) {
    final int wordCount = phi.length;
    final int topicCount = phi[0].length;
    final double[] geometricMean = geometricMean(phi);
    topN = Math.min(topN, wordCount);

    // TODO should this be boxed?
    final Integer[][] output = new Integer[topicCount][topN];
    final double[][] temp = new double[topicCount][wordCount];
    final List<Topic> topics = new ArrayList<>(topicCount);

    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic]
            * Math.log(phi[word][topic]
            / geometricMean[word]);
      }
      final IndexComparator comp = new IndexComparator(temp[topic]);
      final Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, topN);
      final List<TermScore> termScores = new ArrayList<>();
      for (int i = 0; i < topN; i++) {
        termScores.add(new TermScore(dictionary.getToken(output[topic][i]), temp[topic][indexes[i]]));
      }
      topics.add(new Topic(topic, termScores));
    }

    return topics;
  }

  public static List<Topic> mostCommon(double[][] phi, BiDirectionalLookup<String> dictionary, int topN) {
    final int wordCount = phi.length;
    final int topicCount = phi[0].length;

    // TODO should this be boxed?
    final Integer[][] output = new Integer[topicCount][topN];
    final double[][] temp = new double[topicCount][wordCount];
    final List<Topic> topics = new ArrayList<>(topicCount);

    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic];
      }
      final IndexComparator comp = new IndexComparator(temp[topic]);
      final Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, topN);
      final List<TermScore> termScores = new ArrayList<>();
      for (int i = 0; i < topN; i++) {
        termScores.add(new TermScore(dictionary.getToken(output[topic][i]), temp[topic][indexes[i]]));
      }
      topics.add(new Topic(topic, termScores));
    }
    return topics;
  }
}