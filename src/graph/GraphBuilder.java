import java.io.*;
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class GraphBuilder
{
  private Translator translator;
  private SQLConnector c;
  private SparseIntMatrix matrix;
  private TIntArrayList[] adjList;
  private int[] degrees;
  private int order = 0;
  private int sizeDbl  = 0;
  private int layer = 0;
  
  public GraphBuilder fromFile(String filename) {
    try {
      readAll(new File(filename), ",");
    } catch(NumberFormatException e) {
      throw new Error("invalid file format");
    } catch (FileNotFoundException e) {
      throw new Error("file not found");
    } catch (IOException e) {
      throw new Error("IO error");
    }
    return this;
  }

  // uses a translator object attached to a db to translate node labels into
  // the same integer values used in LDA
  public GraphBuilder fromFileAndDB(String filename, SQLConnector c){
    this.c = c;
    translator = new Translator(c);
    try {
      readAllWithTranslator(new File(filename), ",");
    } catch(NumberFormatException e) {
      throw new Error("invalid file format");
    } catch (FileNotFoundException e) {
      throw new Error("file not found");
    } catch (IOException e) {
      throw new Error("IO error");
    }
    
    return this;
  }
  
  private void readAll(File file, String delimiter) 
  throws NumberFormatException, FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    order = getOrder(file, delimiter);
    initialise();
    
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      int n1 = Integer.parseInt(splitLine[0]);
      int n2 = Integer.parseInt(splitLine[1]);
      int weight = Integer.parseInt(splitLine[2]);

      if (matrix.get(n1, n2) == 0 && matrix.get(n2, n1) == 0) insertEdgeSym(n1, n2, weight);
    }
    reader.close();
    if (!matrix.isSymmetric()) throw new Error("constructed asymmetric matrix");
  }
  
  // gets the no. of nodes from the file
  private int getOrder(File file, String delimiter)
  throws NumberFormatException, FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    int max = 0;
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      int n1 = Integer.parseInt(splitLine[0]);
      int n2 = Integer.parseInt(splitLine[1]);
      if (n1 > max) max = n1;
      if (n2 > max) max = n2;
    }
    max++;
    reader.close();
    return max;
  }

  private void readAllWithTranslator(File file, String delimiter) 
  throws NumberFormatException, FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(file));
    // BufferedWriter writer = new BufferedWriter(new FileWriter(new File("newNodes.txt")));
    // int cnt = 0;
    int err = 0;
    String line;
    order = c.getCount("Doc");
    initialise();
    
    while ((line = reader.readLine()) != null) {
      if (!processLine(line, delimiter)) err++;
      // cnt++;
      // String outString = n1 + " " + n2 + " " + weight;
      // writer.write(outString);
      // writer.newLine();
      // if (cnt % 100 == 0) writer.flush();
      
    }
    // writer.flush();
    if (err > 0) throw new Error(err + " nodes not in DB.");
    if (!matrix.isSymmetric()) throw new Error("constructed asymmetric matrix");
    reader.close();
  }
  
  private boolean processLine(String line, String delimiter) {
    String[] splitLine = line.split(delimiter);
    long srcId = Long.parseLong(splitLine[0]);
    long dstId = Long.parseLong(splitLine[1]);
    int weight = Integer.parseInt(splitLine[2]);
    int n1 = translator.getDocIndex(srcId);
    int n2 = translator.getDocIndex(dstId);
    if (n1 == -1) return false;
    if (n2 == -1) return false;
    
    if (matrix.get(n1, n2) != 0 || matrix.get(n2, n1) != 0) {
      throw new Error("duplicate val at " + srcId + " " + dstId);
    }
    insertEdgeSym(n1, n2, weight);
    return true;
  }
  
  private void initialise() {
    matrix = new SparseIntMatrix(order);
    degrees = new int[order];
    adjList = new TIntArrayList[order];
    for (int i = 0; i < order; i++) {
      adjList[i] = new TIntArrayList();
    }
  }

  //inserts symmetrical edge
  private void insertEdgeSym(int n1, int n2, int weight) {
    insertEdge(n1, n2, weight);
    if (n1 != n2) insertEdge(n2, n1, weight);
  }
  
  private void insertEdge(int n1, int n2, int weight) {
    matrix.set(n1, n2, weight);
    adjList[n1].add(n2);
    degrees[n1] += weight;
    sizeDbl += weight;
  }
  
  public GraphBuilder setSize(int order) {
    this.order = order;
    initialise();
    
    return this;
  }
  
  public GraphBuilder addEdge(int n1, int n2, int weight) {
    if (matrix.get(n1, n2) != 0) throw new Error("already exists");
    if (matrix == null) throw new Error("initialise first");
    insertEdgeSym(n1, n2, weight);
    
    return this;
  }
  
  public GraphBuilder coarseGrain(Graph g, TIntIntHashMap map) {
    this.order = g.numComms();
    this.layer = g.layer() + 1;
    initialise();
    int sum = 0;
    
    for ( SparseIntMatrix.Iterator it = g.commWeightIterator(); it.hasNext(); ) {
      it.advance();
      int weight = it.value();
      if (weight != 0) {
        int n1 = map.get((int) it.x());
        int n2 = map.get((int) it.y());
        insertEdge(n1, n2, weight);
        sum += weight;
      }
    }
    
    if (!matrix.isSymmetric()) throw new Error("asymmetric matrix");
    if (sum != g.size() * 2) throw new Error("builder recieved wrong weights: " + sum + " " + (g.size() * 2));
    if (sum != sizeDbl) throw new Error("Coarse-grain error: " + sum + " != " + sizeDbl);
    return this;
  }
  
  public SparseIntMatrix matrix() { return matrix; }
  public TIntArrayList[] adjList() { return adjList; }
  public int[] degrees() { return degrees; }
  public int sizeDbl() { return sizeDbl; }
  public int order() { return order; }
  public int layer() { return layer; }
  
  public Graph build() {
    return new Graph(this);
  }  
}