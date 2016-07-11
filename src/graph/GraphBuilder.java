// Reads from comma-separated file (CSV).
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class GraphBuilder
{
  private final Translator translator;
  private final int[][] matrix;
  private int order = 0;
  private int sizeDbl  = 0;

  public GraphBuilder(Translator translator) {
    this.translator = translator;
  }
  
  public GraphBuilder fromFile(String filename) {
    fromFile(filename, ",");
    
    return this;
  }

  // Allows overriding the default comma delimitation
  public GraphBuilder fromFile(String filename, String delimiter){
    try {
      readAll(new File(filename), delimiter);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    
    return this;
  }

  // Loops to the end of the file
  private void readAll(File file, String delimiter) 
  throws NumberFormatException, FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      long srcId = Long.parseLong(splitLine[0]);
      long dstId = Long.parseLong(splitLine[1]);
      int weight = Integer.parseInt(splitLine[2]);
      int n1 = translator.get(srcId);
      int n2 = translator.get(dstId);
      matrix[n1][n2] = weight;
      matrix[n2][n1] = weight;
      order++;
      sizeDbl += weight;
    }
  }
  
  public int[][] matrix() { return matrix; }
  public int size { return sizeDbl / 2; }
  public int order { return order; }
  
  public Graph build() {
    return new Graph(this);
  }  
}