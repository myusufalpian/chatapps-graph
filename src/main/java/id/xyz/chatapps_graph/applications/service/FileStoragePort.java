package id.xyz.chatapps_graph.applications.service;

import java.io.InputStream;
import java.time.Duration;

public interface FileStoragePort {
  String uploadFile(String fileName, InputStream inputStream, String contentType, long size);
  void deleteFile(String fileName);
  InputStream downloadFile(String fileName);
  String createPresignedUrl(String fileName, Duration expiry);
}
