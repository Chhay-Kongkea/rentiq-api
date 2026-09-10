package co.istad.rentiq_api.features.imageUpload.validation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Raw byte fixtures shared by every image-upload-surface test (backend audit P0-3): real
 * magic-byte prefixes for the three supported raster formats, plus SVG/script content used to
 * prove SVG is rejected regardless of what filename or Content-Type header a client attaches.
 */
public final class ImageFixtures {

    private ImageFixtures() {}

    public static final byte[] JPEG_BYTES =
            concat(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, filler(200));

    public static final byte[] PNG_BYTES =
            concat(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, filler(200));

    public static final byte[] WEBP_BYTES = buildWebp();

    public static final byte[] SVG_XSS_BYTES = (
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                    + "<script>alert(document.domain)</script>"
                    + "</svg>"
    ).getBytes(StandardCharsets.UTF_8);

    private static byte[] buildWebp() {
        byte[] header = new byte[]{
                'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'
        };
        return concat(header, filler(100));
    }

    private static byte[] filler(int size) {
        byte[] result = new byte[size];
        Arrays.fill(result, (byte) 0x00);
        return result;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
