import java.io.*;
import java.nio.file.*;

public class FileManager {
  
  private final File outputDirectory;
  private final File inputFile;
  private String filename;
  
  public FileManager(String filepath) {
    inputFile = new File(filepath);
    if (!inputFile.exists()) throw new Error("Input file not found");
    getFilename(filepath);
    
    outputDirectory = new File("out/" + filename);
    if (!outputDirectory.exists()) createOutputDirectory();
  }
  
  private void getFilename(String filepath) {
    filename = Paths.get(filepath).getFileName().toString();
    filename = removeExtension(filename);
  }
  
  private String removeExtension(String filename) {
    if (filename.indexOf(".") > 0) {
      return filename.substring(0, filename.lastIndexOf("."));
    }
    return filename;
  }
  
  private void createOutputDirectory() {
    outputDirectory.mkdir();
    if (!outputDirectory.exists()) throw new Error("Directory creation failed");
    if (!outputDirectory.isDirectory()) throw new Error("Directory is file?");
  }
  
  public String inputFile() { return inputFile.toString(); }
  public String filename() { return filename; }
  public String outputDirectory() { return outputDirectory.toString() + "/"; }
}