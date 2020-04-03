package com.github.neiljustice.lda;

import com.github.neiljustice.lda.util.BiDirectionalLookup;
import com.github.neiljustice.lda.util.IndexComparator;

import java.util.Arrays;

/**
 * class for static LDA utilities
 */
public class LDAUtils {
  private static final int TOP = 10; // the top <TOP> words are returned. e.g. top 10, top 20

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
  public static void termScore(double[][] phi, BiDirectionalLookup<String> dictionary) {
    final int wordCount = phi.length;
    final int topicCount = phi[0].length;
    final double[] geometricMean = geometricMean(phi);

    // TODO should this be boxed?
    final Integer[][] output = new Integer[topicCount][TOP];
    final double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions

    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic]
            * Math.log(phi[word][topic]
            / geometricMean[word]);
      }
      final IndexComparator comp = new IndexComparator(temp[topic]);
      final Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, TOP);
    }

    System.out.println();
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.printf(" %d : ", topic);
      for (int i = 0; i < TOP; i++) {
        System.out.print(dictionary.getToken(output[topic][i]) + " ");
      }
      System.out.println();
    }
  }

  public static void mostCommon(double[][] phi, BiDirectionalLookup<String> dictionary) {
    final int wordCount = phi.length;
    final int topicCount = phi[0].length;

    final Integer[][] output = new Integer[topicCount][TOP];
    final double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions

    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic];
      }
      final IndexComparator comp = new IndexComparator(temp[topic]);
      final Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, TOP);
    }

    System.out.println();
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.printf(" %d : ", topic);
      for (int i = 0; i < TOP; i++) {
        System.out.print(dictionary.getToken(output[topic][i]) + " ");
      }
      System.out.println();
    }
  }
}