package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

  Attachment validateAndUpload(MultipartFile file, String attachmentType, Long uploaderId, String uploaderUuid);

  void deleteFile(String filePath);
}
