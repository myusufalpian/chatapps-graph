package id.xyz.chatapps_graph.applications.usecase;

import java.util.List;

public record ReadReceiptResult(
    boolean receiptsUpdated,
    List<String> targetUserPhones,
    String readerUuid
) {

  public static ReadReceiptResult hidden() {
    return new ReadReceiptResult(false, List.of(), null);
  }
}
