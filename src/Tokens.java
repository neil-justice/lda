import gnu.trove.list.array.TIntArrayList;
import java.util.*;
import tester.Tester;

public class Tokens {
  private final TIntArrayList words; 
  private final TIntArrayList docs;
  private final TIntArrayList topics;
  private final TIntArrayList docStartPoints;
  // private final ArrayList<Boolean> check;
  private int lastDoc = 0;
  private Random random;
  
  public Tokens() {
    words = new TIntArrayList();
    docs = new TIntArrayList();
    topics = new TIntArrayList();
    docStartPoints = new TIntArrayList();
    docStartPoints.add(0);
    // check = new ArrayList<Boolean>();
  }
  
  public Tokens(int capacity) {
    words = new TIntArrayList(capacity);
    docs = new TIntArrayList(capacity);
    topics = new TIntArrayList(capacity);
    docStartPoints = new TIntArrayList();
    docStartPoints.add(0);
    check = new ArrayList<Boolean>(capacity);
  }
  
  public void add(int word, int doc) {
    add(word, doc, -1);
  }
  
  public void add(int word, int doc, int topic) {
    words.add(word);
    docs.add(doc);
    topics.add(topic);
    if (doc != lastDoc) docStartPoints.add(docs.size());
    lastDoc = doc;
    // check.add(false);
  }
  
  // assumes that the list has not been shuffled.
  public int getDocStartPoint(int doc) {
    return docStartPoints.get(doc);
  }
  
  public void setTopic(int i, int topic) { 
    topics.set(i, topic);
    // check.set(i, true);
  }
  public int word(int i) { return words.get(i); }
  public int doc(int i) { return docs.get(i); }
  public int topic(int i) {return topics.get(i); }
  public int size() {return topics.size(); }
  
  // private int[] toArray(TIntArrayList a, int offset, int len) {
  //   int[] ret = new int[len];
  //   return a.toArray(ret, offset, len);
  // }
  // 
  // public int[] words(int offset, int len) { return toArray(words, offset, len); }
  // public int[] docs(int offset, int len) { return toArray(docs, offset, len); }
  // public int[] topics(int offset, int len) { return toArray(topics, offset, len); }
  
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
    int temp = list.get(i);
    list.set(i, list.get(j));
    list.set(j, temp);
  }
  
  // public int check() {
  //   int count = 0;
  //   for (Boolean b: check) {
  //     if (b.equals(false)) count++;
  //   }
  //   Collections.fill(check, Boolean.FALSE);
  //   return count;
  // }
  
  public static void main(String[] args) {
    Tester t = new Tester();
    Tokens tokens = new Tokens();
    tokens.add(0, 0);
    tokens.add(1, 0);
    tokens.add(2, 0);
    tokens.add(1, 0);
    tokens.add(3, 0);
    tokens.add(1, 1);
    tokens.add(4, 1);
    tokens.add(5, 1);
    tokens.add(1, 2);
    tokens.add(2, 2);
    tokens.add(2, 2);
    tokens.add(2, 2);
    tokens.add(2, 3);
    tokens.add(2, 3);
    
    t.is(tokens.getDocStartPoint(0), 0);
    t.is(tokens.getDocStartPoint(1), 6);
    t.is(tokens.getDocStartPoint(2), 9);
    t.is(tokens.getDocStartPoint(3), 13);
    
    t.results();
  }
}