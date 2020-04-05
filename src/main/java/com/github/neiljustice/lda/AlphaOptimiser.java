package com.github.neiljustice.lda;

import java.util.Arrays;

/**
 * implements algorithm 2.2 / eq. 2.58 from Wallach's phd thesis
 * https://people.cs.umass.edu/~wallach/theses/wallach_phd_thesis.pdf
 */
public class AlphaOptimiser {
  // [cnt] no. of topic [k] tokens
  // these are C(n) and Ck(n) respectively in the paper
  // where n is length or count
  private static final double S = 1d;       // gamma hyperprior
  private static final double C = 1.001;    // gamma hyperprior - see see sect. 2.5 in paper
  private final int topicCount;
  private final int docCount;
  private final int maxLength;       // length of longest doc. max_dN_{.|d} in alg. 2.2
  private final int[] docLengthHist; // dLH[l] == no. of docs of length [l]
  private int[][] docTopicCountHist; // dTCH[k][cnt] = no. of docs with

  public AlphaOptimiser(int[] tokensInDoc, int topicCount, int maxLength) {
    this.docCount = tokensInDoc.length;
    this.topicCount = topicCount;
    this.maxLength = maxLength;
    docLengthHist = new int[maxLength];

    for (int doc = 0; doc < docCount; doc++) {
      final int length = tokensInDoc[doc];
      docLengthHist[length]++;
    }
  }

  public double optimiseAlpha(double[] alpha, int[][] docTopicCountHist) {
    return optimiseAlpha(alpha, docTopicCountHist, 10);
  }

  // alpha is modified in-place.  variable names S, Sk and D are taken from alg.
  // 2.2 in Wallach's PhD thesis
  public double optimiseAlpha(double[] alpha, int[][] docTopicCountHist,
                              int iterations) {
    this.docTopicCountHist = docTopicCountHist;
    double alphaSum = 0d;

    for (int topic = 0; topic < topicCount; topic++) {
      alphaSum += alpha[topic];
    }

    final int[] highestCountForTopic = setHighestCountForTopic();

    for (int it = 0; it < iterations; it++) {
      double S = 0;
      double D = 0;
      for (int length = 1; length < maxLength; length++) {
        D += 1 / (length - 1 + alphaSum);
        S += docLengthHist[length] * D;
      }
      S -= 1 / AlphaOptimiser.S; // s is a component of the hyperprior

      alphaSum = 0;
      double Sk;
      for (int topic = 0; topic < topicCount; topic++) {
        Sk = 0d;
        D = 0;
        for (int count = 1; count <= highestCountForTopic[topic]; count++) {
          D += 1 / (count - 1 + alpha[topic]);
          Sk += docTopicCountHist[topic][count] * D;
        }
        alpha[topic] *= (Sk + C) / S; // c is a component of the hyperprior
        alphaSum += alpha[topic];
      }
    }

    // TODO test and find out why this happens...
    if (Arrays.stream(alpha).anyMatch(d -> d < 0)) {
      for (int topic = 0; topic < topicCount; topic++) {
        alpha[topic] = 0.1;
        alphaSum += alpha[topic];
      }
    }
    return alphaSum;
  }

  // highestCountForTopic[topic] = the no. of times that topic appears in the
  // doc in which it appears most.
  private int[] setHighestCountForTopic() {
    final int[] highestCountForTopic = new int[topicCount];
    Arrays.fill(highestCountForTopic, -1);

    for (int topic = 0; topic < topicCount; topic++) {
      for (int length = 0; length < maxLength; length++) {
        if (docTopicCountHist[topic][length] > 0) {
          highestCountForTopic[topic] = length;
        }
      }
    }
    return highestCountForTopic;
  }
}