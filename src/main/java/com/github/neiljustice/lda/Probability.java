package com.github.neiljustice.lda;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Probability {
  private Probability() {
    // Disable instantiation
  }

  public static int sampleFromMultinomialDist(double[] dist, double sum) {
    int result = -1;
    double sample = ThreadLocalRandom.current().nextDouble() * sum;

    while (sample > 0.0) {
      result++;
      sample -= dist[result];
    }

    if (result == -1) {
      throw new IllegalStateException("Sampling failure. Sample: " + sample + " sum: " + sum);
    }

    return result;
  }

  public static int sampleFromMultinomialDist(double[][] dist, int col, double sum) {
    int result = -1;
    double sample = ThreadLocalRandom.current().nextDouble() * sum;

    while (sample > 0.0) {
      result++;
      sample -= dist[result][col];
    }

    if (result == -1) {
      throw new IllegalStateException("Sampling failure. Sample: " + sample + " sum: " + sum);
    }

    return result;
  }

  public static int sampleFromPoissonDist(int[] series) {
    return sampleFromPoissonDist(Arrays.stream(series).average().orElse(0d));
  }

  public static int sampleFromPoissonDist(double mean) {
    final double L = Math.exp(-mean);
    int k = 0;
    double p = 1.0;
    do {
      p = p * ThreadLocalRandom.current().nextDouble();
      k++;
    } while (p > L);
    return k - 1;
  }
}
