package com.github.neiljustice.lda.util;

import java.util.*;

/**
 * Sorts the indexes of an array of doubles  according to the values of the
 * doubles.
 */
public class IndexComparator implements Comparator<Integer> {
  private final double[] array;

  public IndexComparator(double[] array) {
    this.array = array;
  }

  public Integer[] indexArray() {
    Integer[] indexes = new Integer[array.length];
    for (int i = 0; i < array.length; i++) {
      indexes[i] = i; // Autobox
    }
    return indexes;
  }

  @Override
  public int compare(Integer index1, Integer index2) {
    return Double.compare(array[index1], array[index2]); // Autounbox
  }
}