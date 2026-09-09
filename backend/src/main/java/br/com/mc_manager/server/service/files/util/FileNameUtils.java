package br.com.mc_manager.server.service.files.util;

import java.util.Set;

public class FileNameUtils {

  private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
          "jar", "class", "exe", "dll", "so", "mca", "dat", "zip", "gz", "sh"
  );

  private FileNameUtils() {
  }

  public static String extractExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');

    return dotIndex >= 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
  }

  public static boolean hasBlockedExtension(String extension) {
    return BLOCKED_EXTENSIONS.contains(extension);
  }

  public static String normalizeEntryName(String fileName) {
    return fileName.replace('\\', '/');
  }

}
