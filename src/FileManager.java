import java.io.*;
import java.nio.file.*;

public class FileManager {
  
  private final String filename;
  private final String filepath;
  private final String dir;
  
  public FileManager(String[] args) {
    Path p = checkFilename(args);
    String filenameWithExt = p.getFileName().toString();
    filepath = args[1];
    
    if (filenameWithExt.indexOf(".") > 0) {
      filename = filenameWithExt.substring(0, filenameWithExt.lastIndexOf("."));
    }
    else filename = filenameWithExt;
    
    dir = "out/" + filename + "/";
    File file = new File(dir);

    file.mkdir();
    if (!file.isDirectory()) throw new Error("Directory creation failed");
  }
  
  private Path checkFilename(String[] args) {
    if (args.length < 2) throw new Error("Please provide a file name");
    Path p = Paths.get(args[1]);
    if (!p.toFile().exists()) throw new Error("Missing input file");
    
    return p;
  }
 
 public String dir() { return dir; }
 public String filename() { return filename; }
 public String filepath() { return filepath; }
}