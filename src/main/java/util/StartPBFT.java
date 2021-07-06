package util;


import config.StartConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class StartPBFT {

    public static boolean start() throws IOException {
        StartConfig startConfig = new StartConfig();
        if (startConfig.startConfig()) {
            if (new PBFT().pubView()) {
                return true;
            }
        }
        return false;
    }
}
