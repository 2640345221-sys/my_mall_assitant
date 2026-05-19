package assitant.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class VectorsUtils {

    public static String encode(float[] vec) {
        ByteBuffer buf = ByteBuffer.allocate(vec.length * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : vec) buf.putFloat(f);
        return new String(buf.array(), StandardCharsets.ISO_8859_1);
    }

    public static float[] decode(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        float[] vec = new float[bytes.length / 4];
        for (int i = 0; i < vec.length; i++) vec[i] = buf.getFloat();
        return vec;
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
