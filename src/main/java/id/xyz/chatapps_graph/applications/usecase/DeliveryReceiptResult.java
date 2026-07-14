package id.xyz.chatapps_graph.applications.usecase;

import java.util.List;

public record DeliveryReceiptResult(
    boolean receiptsUpdated,
    List<String> targetUserPhones,
    String readerUuid
) {

  public static DeliveryReceiptResult hidden() {
    return new DeliveryReceiptResult(false, List.of(), null);
  }
}
