package org.kiwi.console.util;

import com.github.markusbernhardt.proxy.selector.pac.PacProxySelector;
import com.github.markusbernhardt.proxy.selector.pac.UrlPacScriptSource;

import java.net.ProxySelector;

public class ProxyUtils {

    public static void setupPacProxy(String pacFileUrl) {
        var pacProxySelector = new PacProxySelector(
                new UrlPacScriptSource(pacFileUrl)
        );
        ProxySelector.setDefault(pacProxySelector);
    }

}
