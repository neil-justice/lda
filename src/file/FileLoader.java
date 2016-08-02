import java.io.*;
import java.util.*;

public class FileLoader {
  
  public static void loadList(String in, Collection<String> coll) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(in)));
      String line;
      
      while ((line = reader.readLine()) != null) {
        coll.add(line.toLowerCase());
      }
    } catch (FileNotFoundException e) {
      throw new Error("input file not found at " + in);
    } catch (IOException e) {
      throw new Error("IO error");
    } 
  }
  
  public static void processFile(String in, String out, LineOperator op) {
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
  
  public static List<String> readFile(String in, LineOperator op) {
    try {
      List<String> list = new ArrayList<String>();
      BufferedReader reader = new BufferedReader(new FileReader(new File(in)));
      String line;
      while ((line = reader.readLine()) != null) {
        String outString = op.operate(line);
        list.add(outString);
      }
      return list;
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