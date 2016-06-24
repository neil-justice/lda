import java.io.*;
import java.util.*;

public class ListLoader {
  
  public static void load(String filename, Collection<String> coll) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
      String line;
      
      while ((line = reader.readLine()) != null) {
        coll.add(line.toLowerCase());
      }
    } catch(Exception e) {
      throw new Error("file not found");
    }
  }
  
  public static void process(String in, String out, LineOperator op) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(in)));
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)));
      String line;
      int cnt = 0;
      
      while ((line = reader.readLine()) != null) {
        cnt++;
        String outString = op.operate(line);
        writer.write(outString);
        writer.newLine();
        if (cnt % 100 == 0) writer.flush();
      }
      writer.flush();
    } catch (FileNotFoundException e) {
      throw new Error("input file not found at " + in);
    } catch (IOException e) {
      throw new Error("IO error");
    }      
  }
  
  public interface LineOperator {
    public String operate(String in);
  }
}