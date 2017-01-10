package com.github.neiljustice.lda;

import java.util.*;
import com.github.neiljustice.lda.util.BiDirectionalLookup;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

public class Corpus {
  private final TIntArrayList words; 
  private final TIntArrayList docs;
  private final TIntArrayList topics;
  private final TIntArrayList docStartPoints;
  private int lastDoc = 0;
  private Random random;
  
  private final int docCount;
  private final int wordCount;
  private final BiDirectionalLookup<String> dictionary;
  
  public Corpus(List<List<String>> processedDocuments) {
    dictionary = new BiDirectionalLookup<String>();
    
    words = new TIntArrayList();
    docs = new TIntArrayList();
    topics = new TIntArrayList();
    docStartPoints = new TIntArrayList();
    docStartPoints.add(0);
    
    int doc = 0;
    
    for (List<String> document: processedDocuments) {
      for (String token: document) {
        int wordIndex = dictionary.add(token);
        add(wordIndex, doc);
      }
      doc++;
    }
    
    docCount = processedDocuments.size();
    wordCount = dictionary.size();
    
  }
  
  private void add(int word, int doc) {
    words.add(word);
    docs.add(doc);
    topics.add(-1);
    if (doc != lastDoc) docStartPoints.add(docs.size());
    lastDoc = doc;
  }
  
  // assumes that the list has not been shuffled.
  public int getDocStartPoint(int doc) {
    return docStartPoints.get(doc);
  }
  
  public void setTopic(int i, int topic) { 
    topics.setQuick(i, topic);
  }
  public int word(int i) { return words.getQuick(i); }
  
  public int doc(int i) { return docs.getQuick(i); }
  
  public int topic(int i) {return topics.getQuick(i); }
  
  public int size() {return topics.size(); }
  
  public BiDirectionalLookup<String> dictionary() { return dictionary; }

  // shuffles the lists without losing info about their shared indices.
  // This is a modified version of Collections.shuffle()
  // public void shuffle() {
  //   if (random == null) random = new Random();
  //   int count = size();
  //   for (int i = count; i > 1; i--) {
  //     int r = random.nextInt(i);
  //     swap(words , i - 1, r);
  //     swap(docs  , i - 1, r);
  //     swap(topics, i - 1, r);
  //   }
  // }
  // 
  // private static void swap(TIntArrayList list, int i, int j) {
  //   int temp = list.getQuick(i);
  //   list.set(i, list.getQuick(j));
  //   list.set(j, temp);
  // }
  
  public int wordCount() { return wordCount; }
  
  public int docCount() { return docCount; }
  
  // public static void main(String[] args) {
  //   Tester t = new Tester();
  //   Corpus corpus = new Corpus();
  //   corpus.add(0, 0);
  //   corpus.add(1, 0);
  //   corpus.add(2, 0);
  //   corpus.add(1, 0);
  //   corpus.add(3, 0);
  //   corpus.add(1, 1);
  //   corpus.add(4, 1);
  //   corpus.add(5, 1);
  //   corpus.add(1, 2);
  //   corpus.add(2, 2);
  //   corpus.add(2, 2);
  //   corpus.add(2, 2);
  //   corpus.add(2, 3);
  //   corpus.add(2, 3);
  //   
  //   t.is(corpus.getDocStartPoint(0), 0);
  //   t.is(corpus.getDocStartPoint(1), 6);
  //   t.is(corpus.getDocStartPoint(2), 9);
  //   t.is(corpus.getDocStartPoint(3), 13);
  //   
  //   t.results();
  // }
}