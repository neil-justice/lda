package com.github.neiljustice.lda.util;

public class ArrayUtils {
  private ArrayUtils() {
    // Disable instantiation
  }

  public static int max(int[] array) {
    int max = 0;
    for (final int value : array) {
      if (value > max) {
        max = value;
      }
    }
    return max;
  }

  public static int avg(int[] array) {
    int avg = 0;
    for (int value : array) {
      avg += value;
    }
    return avg / array.length;
  }

  public static double sum(double[] array) {
    double sum = 0;
    for (double value : array) {
      sum += value;
    }
    return sum;
  }

  public static double sumColumn(double[][] matrix, int index) {
    double sum = 0;
    for (double[] row : matrix) {
      sum += row[index];
    }
    return sum;
  }

  public static double[] getColumn(double[][] matrix, int index) {
    final double[] column = new double[matrix.length];
    for (int i = 0; i < matrix.length; i++) {
      column[i] = matrix[i][index];
    }
    return column;
  }
}
