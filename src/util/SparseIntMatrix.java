/* Sparse square matrix using hashmap. */
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.iterator.TLongIntIterator;
import tester.Tester;

public class SparseIntMatrix {
  private final TLongIntHashMap map;
  private final long size;
  
  public SparseIntMatrix(int size) {
    this.size = (long) size;
    map = new TLongIntHashMap();
  }
  
  public SparseIntMatrix(SparseIntMatrix m) {
    this.size = (long) m.size();
    map = m.copyMap();
  }
  
  public int get (int x, int y) { 
    return map.get((long) x * size + (long) y);
  }
  
  public void set (int x, int y, int val) { 
    map.put((long) x * size + (long) y, val);
  }
  
  public void add (int x, int y, int val) {
    set(x, y, get(x, y) + val);
  }
  
  private boolean isNonZero(long key, int val) {
    if (val == 0) return false;
    return true;
  }
  
  public void compress() {
    map.retainEntries(this::isNonZero);
  }
  
  public int size() { return (int) size; }
  
  public TLongIntIterator iterator() { return map.iterator(); }
  
  public TLongIntHashMap copyMap() { return new TLongIntHashMap(map); }
  
  public static void main(String[] args) {
    Tester t = new Tester();
    SparseIntMatrix m = new SparseIntMatrix(3);
    
    for (int i = 0; i < m.size(); i++) {
      for (int j = 0; j < m.size(); j++) {
        m.set(i, j, i + j);
      }
    }
    
    for (int i = 0; i < m.size(); i++) {
      for (int j = 0; j < m.size(); j++) {
        t.is(m.get(i, j), i + j);
      }
    }
    
    t.results();
  }
}