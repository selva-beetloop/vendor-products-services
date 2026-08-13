package com.beetloop.catalog.document;

/** Magic-byte sniffing, so a .exe renamed to .png is rejected. */
final class MimeSniffer {

    private MimeSniffer() {
    }

    static String sniff(byte[] bytes, String declared) {
        if (bytes.length >= 4) {
            if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
                return "application/pdf";
            }
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
            if (bytes[0] == 0x50 && bytes[1] == 0x4B) {
                // ZIP container: xlsx and friends.
                if (bytes.length >= 12 && new String(bytes, 8, 4).contains("xl")) {
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
                return declared != null && declared.contains("spreadsheet")
                        ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        : "application/zip";
            }
            if (bytes.length >= 12 && new String(bytes, 0, 4).equals("RIFF")
                    && new String(bytes, 8, 4).equals("WEBP")) {
                return "image/webp";
            }
        }
        return declared == null ? "application/octet-stream" : declared;
    }
}
