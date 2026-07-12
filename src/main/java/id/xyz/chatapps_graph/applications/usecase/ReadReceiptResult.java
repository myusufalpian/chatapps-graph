package id.xyz.chatapps_graph.applications.usecase;

import java.util.List;

public record ReadReceiptResult(boolean receiptsUpdated, List<Long> senderIds) {

  public static ReadReceiptResult hidden() {
    return new ReadReceiptResult(false, List.of());
  }
}
