package id.xyz.chatapps_graph.applications.service;

import java.io.InputStream;

public interface FileStoragePort {
  String uploadFile(String fileName, InputStream inputStream, String contentType, long size);
  void deleteFile(String fileName);
}
