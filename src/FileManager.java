import java.io.*;
import java.nio.file.*;

public class FileManager {
  
  private final String filename;
  private final String filenameWithExt;
  private final String filepath;
  private String outputDirectory;
  private final String inputDirectory;
  private final String ext;
  
  public FileManager(String[] args) {
    Path p = checkFilename(args);
    filenameWithExt = p.getFileName().toString();
    inputDirectory = p.getParent().toString() + "/";
    
    filepath = args[1];
    
    if (filenameWithExt.indexOf(".") > 0) {
      filename = filenameWithExt.substring(0, filenameWithExt.lastIndexOf("."));
      ext = filenameWithExt.substring(filenameWithExt.lastIndexOf(".") + 1);
    }
    else {
      filename = filenameWithExt;
      ext = "";
    }
  }
  
  public void loadOutputDirectory() {
    outputDirectory = inputDirectory;
  }
  
  public void createOutputDirectory() {
    outputDirectory = "out/" + filename + "/";
    File file = new File(outputDirectory);

    file.mkdir();
    if (!file.isDirectory()) throw new Error("Directory creation failed");    
  }
  
  private Path checkFilename(String[] args) {
    if (args.length < 2) throw new Error("Please provide a file name");
    Path p = Paths.get(args[1]);
    if (!p.toFile().exists()) throw new Error("Missing input file");
    
    return p;
  }
 
 public String dir() { return outputDirectory; }
 public String ext() { return ext; }
 public String filename() { return filename; }
 public String filenameWithExt() { return filenameWithExt; }
 public String filepath() { return filepath; }
}