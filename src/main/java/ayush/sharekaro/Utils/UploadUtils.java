package ayush.sharekaro.Utils;

import java.util.Random;

public class UploadUtils {
    public static int generateport() {
        // need to create a port in a range
        // we will use port as per IANA standards
        int DYANAMIC_STARTING_PORT = 49152;
        int DYANAMIC_ENDING_PORT = 65535;
        Random random = new Random();
        return random.nextInt(DYANAMIC_ENDING_PORT-DYANAMIC_STARTING_PORT)+DYANAMIC_STARTING_PORT;
        //explanation
        // 0 + 49152 = 49152
        // 1 + 49152 = 49153
        //...
        //16382 + 49152 = 65534
        // 49152 to 65534 (inclusive)
    }
}