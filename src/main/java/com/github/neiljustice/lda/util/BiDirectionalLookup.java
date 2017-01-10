package com.github.neiljustice.lda.util;

import java.util.*;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * A bi-directional lookup between indexes and objects.  Indexing is always
 * consecutive starting from zero.  Will not accept duplicates.
 */
public class BiDirectionalLookup<T> {
  private final TObjectIntHashMap<T> tokenToIndex;
  private final List<T> indexToToken;
  private int curr = -1;
  
  public BiDirectionalLookup() {
    tokenToIndex = new TObjectIntHashMap<T>();
    indexToToken = new ArrayList<T>();
  }
  
  public boolean contains(T token) {
    return tokenToIndex.containsKey(token);
  }
  
  /**
   * Adds a token to the dictionary and returns the ID of the token.  If the
   * token is already present, does not alter the dictionary, and returns the
   * ID of the token.
   */
  public int add(T token) {
    if (contains(token)) return tokenToIndex.get(token);
    
    curr++;
    tokenToIndex.put(token, curr);
    indexToToken.add(token);
    
    return curr;
  }
  
  public int size() {
    return curr + 1;
  }
  
  /**
   * Returns null if the index is out of bounds.
   */
  public T getToken(int index) {
    if (index < 0 || index > curr) return null;
    
    return indexToToken.get(index);
  } 
  
  /**
   * Returns null if the token is not in the dictionary.
   */
  public int getIndex(T token) {
    return tokenToIndex.get(token);
  }
}