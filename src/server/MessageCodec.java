package server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageCodec {

    public String decode(ByteBuffer buffer) {
        int start = buffer.position();
        int limit = buffer.limit();
        for (int i = start; i < limit; i++) {
            if (buffer.get(i) == (byte) '\n') {
                int length = i - start;
                byte[] lineBytes = new byte[length];
                buffer.position(start);
                buffer.get(lineBytes, 0, length);
                buffer.position(i + 1);
                return new String(lineBytes, StandardCharsets.UTF_8);
            }
        }
        buffer.position(start);
        return null;
    }
}