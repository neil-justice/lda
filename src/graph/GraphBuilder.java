import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class GraphBuilder
{
  private Translator translator;
  private SQLConnector c;
  private int[][] matrix;
  private TIntArrayList[] adjList;
  private int[] degrees;
  private int order = 0;
  private int size  = 0;
  
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
    
    matrix = new int[order][order];
    degrees = new int[order];
    adjList = new TIntArrayList[order];
    for (int i = 0; i < order; i++) {
      adjList[i] = new TIntArrayList();
    }
    
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      int n1 = Integer.parseInt(splitLine[0]);
      int n2 = Integer.parseInt(splitLine[1]);
      int weight = Integer.parseInt(splitLine[2]);

      if (matrix[n1][n2] == 0 && matrix[n2][n1] == 0) insertEdge(n1, n2, weight);
    }
  }
  
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
    String line;
    order = c.getCount("Doc");
    matrix = new int[order][order];
    degrees = new int[order];
    adjList = new TIntArrayList[order];
    for (int i = 0; i < order; i++) {
      adjList[i] = new TIntArrayList();
    }
    
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      long srcId = Long.parseLong(splitLine[0]);
      long dstId = Long.parseLong(splitLine[1]);
      int weight = Integer.parseInt(splitLine[2]);
      int n1 = translator.getDocIndex(srcId);
      int n2 = translator.getDocIndex(dstId);
      if (matrix[n1][n2] != 0) throw new Error("duplicate val at " + srcId + " " + dstId);
      if (matrix[n2][n1] != 0) throw new Error("duplicate val at " + srcId + " " + dstId);
      insertEdge(n1, n2, weight);
    }
  }
  
  private void insertEdge(int n1, int n2, int weight) {
    matrix[n1][n2] = weight;
    matrix[n2][n1] = weight;
    adjList[n1].add(n2);
    adjList[n2].add(n1);
    degrees[n1] += weight;
    degrees[n2] += weight;
    size += weight;
  }
  
  public GraphBuilder setSize(int order) {
    matrix = new int[order][order];
    degrees = new int[order];
    this.order = order;
    adjList = new TIntArrayList[order];
    for (int i = 0; i < order; i++) {
      adjList[i] = new TIntArrayList();
    }
    
    return this;
  }
  
  public GraphBuilder addEdge(int n1, int n2, int weight) {
    if (matrix[n1][n2] != 0) throw new Error("already exists");
    if (matrix == null) throw new Error("initialise first");
    insertEdge(n1, n2, weight);
    
    return this;
  }  
  
  public int[][] matrix() { return matrix; }
  public TIntArrayList[] adjList() { return adjList; }
  public int[] degrees() { return degrees; }
  public int size() { return size; }
  public int order() { return order; }
  
  public Graph build() {
    return new Graph(this);
  }  
}