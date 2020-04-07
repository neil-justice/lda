package com.github.neiljustice.lda;

import com.github.neiljustice.lda.util.BiDirectionalLookup;
import gnu.trove.list.array.TIntArrayList;

import java.util.List;

public class Corpus {
  private final TIntArrayList words;
  private final TIntArrayList docs;
  private final TIntArrayList topics;
  private final int docCount;
  private final int wordCount;
  private final BiDirectionalLookup<String> dictionary;

  public Corpus(List<List<String>> processedDocuments) {
    dictionary = new BiDirectionalLookup<>();

    words = new TIntArrayList();
    docs = new TIntArrayList();
    topics = new TIntArrayList();

    int doc = 0;

    for (List<String> document : processedDocuments) {
      for (String token : document) {
        final int wordIndex = dictionary.add(token);
        add(wordIndex, doc);
      }
      doc++;
    }

    docCount = processedDocuments.size();
    wordCount = dictionary.size();

  }

  public Corpus(Corpus other) {
    dictionary = new BiDirectionalLookup<>();

    words = new TIntArrayList(other.words);
    docs = new TIntArrayList(other.docs);
    topics = new TIntArrayList(other.topics);

    for (int i = 0; i < other.dictionary.size(); i++) {
      dictionary.add(other.dictionary.getToken(i));
    }

    docCount = other.docCount;
    wordCount = other.wordCount;
  }

  private void add(int word, int doc) {
    words.add(word);
    docs.add(doc);
    topics.add(-1);
  }

  public void setTopic(int i, int topic) {
    topics.setQuick(i, topic);
  }

  public int word(int i) {
    return words.getQuick(i);
  }

  public int doc(int i) {
    return docs.getQuick(i);
  }

  public int topic(int i) {
    return topics.getQuick(i);
  }

  public int size() {
    return topics.size();
  }

  public BiDirectionalLookup<String> dictionary() {
    return dictionary;
  }

  public int wordCount() {
    return wordCount;
  }

  public int docCount() {
    return docCount;
  }

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