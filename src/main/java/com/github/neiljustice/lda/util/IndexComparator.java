package com.github.neiljustice.lda.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Sorts the indexes of an array of doubles  according to the values of the
 * doubles, making the old indexes available via indexArray()
 */
public class IndexComparator implements Comparator<Integer>, Serializable {
  private final double[] array;

  public IndexComparator(double[] array) {
    this.array = array;
  }

  // TODO should this be boxed?
  public Integer[] indexArray() {
    final Integer[] indexes = new Integer[array.length];
    for (int i = 0; i < array.length; i++) {
      indexes[i] = i;
    }
    return indexes;
  }

  @Override
  public int compare(Integer index1, Integer index2) {
    return Double.compare(array[index1], array[index2]); // Autounbox
  }
}