import gnu.trove.list.array.TIntArrayList;
import java.util.*;

public class Tokens {
  private final TIntArrayList words; 
  private final TIntArrayList docs;
  private final TIntArrayList topics;
  private Random random;
  
  public Tokens() {
    words = new TIntArrayList();
    docs = new TIntArrayList();
    topics = new TIntArrayList();
  }
  
  public Tokens(int capacity) {
    words = new TIntArrayList(capacity);
    docs = new TIntArrayList(capacity);
    topics = new TIntArrayList(capacity);
  }
  
  public void add(int word, int doc) { 
    add(word, doc, -1);
  }
  
  public void add(int word, int doc, int topic) {
    words.add(word);
    docs.add(doc);
    topics.add(topic);
  }
  
  public void setTopic(int i, int topic) { topics.setQuick(i, topic); }
  public int word(int i) { return words.getQuick(i); }
  public int doc(int i) { return docs.getQuick(i); }
  public int topic(int i) {return topics.getQuick(i); }
  public int size() {return topics.size(); }
  
  // shuffles the lists without losing info about their shared indices.
  // This is a modified version of Collections.shuffle()
  public void shuffle() {
    if (random == null) random = new Random();
    int count = size();
    for (int i = count; i > 1; i--) {
      int r = random.nextInt(i);
      swap(words , i - 1, r);
      swap(docs  , i - 1, r);
      swap(topics, i - 1, r);
    }
  }

  private static void swap(TIntArrayList list, int i, int j) {
    int temp = list.getQuick(i);
    list.set(i, list.getQuick(j));
    list.set(j, temp);
  }
}