import java.io.*;
import java.nio.file.*;

public class FileManager {
  
  public static String prepareDirectory(String[] args) {
    Path p = checkFilename(args);

    String filename = p.getFileName().toString();
    if (filename.indexOf(".") > 0) {
      filename = filename.substring(0, filename.lastIndexOf("."));
    }
    
    String dir = "out/" + filename + "/";
    File file = new File(dir);

    file.mkdir();
    if (!file.isDirectory()) throw new Error("Directory creation failed");
    
    return dir;
  }
  
  private static Path checkFilename(String[] args) {
    if (args.length < 2) throw new Error("Please provide a file name");
    Path p = Paths.get(args[1]);
    if (!p.toFile().exists()) throw new Error("Missing input file");
    
    return p;
  }
}