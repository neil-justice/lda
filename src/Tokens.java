import gnu.trove.list.array.TIntArrayList;
import java.util.*;

public class Tokens {
  private final TIntArrayList words  = new TIntArrayList();
  private final TIntArrayList docs   = new TIntArrayList();
  private final TIntArrayList topics = new TIntArrayList();
  private Random random;
  
  public void add(int word, int doc) { 
    words.add(word);
    docs.add(doc);
    topics.add(-1);
  }
  
  public void setTopic(int i, int topic) { topics.set(i, topic); }
  public int word(int i) { return words.get(i); }
  public int doc(int i) { return docs.get(i); }
  public int topic(int i) {return topics.get(i); }
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
    int temp = list.get(i);
    list.set(i, list.get(j));
    list.set(j, temp);
  }
}