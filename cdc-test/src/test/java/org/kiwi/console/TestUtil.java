package org.kiwi.console;

import org.kiwi.console.util.Utils;

public class TestUtil {

    public static <T> T createKiwiFeignClient(Class<T> clazz) {
        return Utils.createKiwiFeignClient(Config.MANUL_HOST, clazz, Config.APP_ID);
    }

}
