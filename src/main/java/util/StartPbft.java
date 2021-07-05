package util;


import config.StartConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartPbft {

    public static boolean start() {
        StartConfig startConfig = new StartConfig();
        if (startConfig.startConfig()) {
            if (new Pbft().pubView()) {
                return true;
            }
        }
        return false;
    }
}
