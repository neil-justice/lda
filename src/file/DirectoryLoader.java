/* tracks which directories are available to load from the output dir */
import java.io.*;
import java.nio.file.*;

public class DirectoryLoader {
  private final String root = "out" + File.separator;
  private final File[] directories;
  private final String[] directoryNames;
  private File dir;
  
  public DirectoryLoader() {
    File rootdir = new File(root);
    if (!rootdir.exists()) rootdir.mkdir();
    
    directories = new File(root).listFiles(File::isDirectory);
    directoryNames = new String[directories.length];
    
    for (int i = 0; i < directories.length; i++) {
      directoryNames[i] = directories[i].toString().substring(root.length());
    }
  }
  
  public void setDirectory(String choice) {
    for (int i = 0; i < directoryNames.length; i++) {
      if (directoryNames[i].equals(choice)) {
        dir = directories[i];
      }
    }
  }
  
  public void printDirectories() {
    for (int i = 0; i < directoryNames.length; i++) {
      System.out.println(i + " : " + directoryNames[i]);
    }    
  }
  
  public void setDirectory(int i) {
    dir = directories[i];
  }
  
  public String[] directories() { return directoryNames; }
  public String dir() { return dir.toString() + File.separator; }
  
  public static void main(String[] args) {
    DirectoryLoader dl = new DirectoryLoader();
    System.out.println("Directories found in output dir:");
    
    dl.printDirectories();
  }
}