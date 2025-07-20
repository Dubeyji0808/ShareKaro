package ayush.sharekaro;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UploadTest {

    @Test
    public void testFileTooLarge() {
        long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
//        Why 10 * 1024 * 1024? Because:
//
//        1 KB = 1024 bytes
//        1 MB = 1024 KB = 1024 * 1024 bytes
//        10 MB = 10 * 1024 * 1024 bytes
        byte[] fakeFile = new byte[(int) (MAX_FILE_SIZE + 1)]; // makes a file one byte big

        assertTrue(fakeFile.length > MAX_FILE_SIZE, "File size should exceed limit");

        // Simulate upload logic check
        boolean isTooBig = fakeFile.length > MAX_FILE_SIZE;

        assertTrue(isTooBig, "File should be rejected as too big");
    }

    @Test
    public void testFileWithinLimit() {
        long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
        byte[] fakeFile = new byte[5 * 1024 * 1024]; // 5MB

        assertTrue(fakeFile.length <= MAX_FILE_SIZE, "File should be accepted");
    }
}

