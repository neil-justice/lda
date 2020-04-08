package com.github.neiljustice.lda.util;

public class MatrixUtils {
  private MatrixUtils() {
    // Disable instantiation
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
