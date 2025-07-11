package org.kiwi.console;

import java.nio.file.Path;

public class Lab {

    public static void main(String[] args) {
        var p1 = Path.of("/root");
        var p2 = Path.of("/root/workdir");
        System.out.println(p1.relativize(p2));
    }

}
