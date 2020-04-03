package com.github.neiljustice.lda.util;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtils {

  public static File loadResource(String filename) {
    final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
    return new File(url.getPath());
  }

  public static void loadResourceToCollection(String filename, Collection<String> coll) {
    loadResourceToCollection(filename, coll, StandardCharsets.UTF_8);
  }

  public static void loadResourceToCollection(String filename, Collection<String> coll, Charset charset) {
    String line;

    try (FileInputStream fis = new FileInputStream(loadResource(filename));
         InputStreamReader isr = new InputStreamReader(fis, charset);
         BufferedReader reader = new BufferedReader(isr)) {
      while ((line = reader.readLine()) != null) {
        coll.add(line);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<String> readFile(String in) {
    return readFile(new File(in), StandardCharsets.UTF_8);
  }

  public static List<String> readFile(File in) {
    return readFile(in, StandardCharsets.UTF_8);
  }

  public static List<String> readFile(File in, Charset charset) {
    final List<String> list = new ArrayList<>();
    String line;

    try (FileInputStream fis = new FileInputStream(in);
         InputStreamReader isr = new InputStreamReader(fis, charset);
         BufferedReader reader = new BufferedReader(isr)) {
      while ((line = reader.readLine()) != null) {
        list.add(line);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return list;
  }
}