//package org.kiwi.console.patch;
//
//import lombok.SneakyThrows;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//public class PatchApply {
//
//    public static String apply(String text, String diff) {
//        var hunks = new PatchReader(diff).read().files();
//        var buf = new StringBuilder();
//        var line = 1;
//        var skipEnd = -1;
//        for (int i = 0, j = 0; i < text.length(); i++) {
//            while (j < hunks.size() && hunks.get(j).startLine() == line) {
//                var hunk = hunks.get(j++);
//                switch (hunk.op()) {
//                    case insert -> buf.append(hunk.content());
//                    case delete -> skipEnd = hunk.endLine();
//                    case replace -> {
//                        buf.append(hunk.content());
//                        skipEnd = hunk.endLine();
//                    }
//                }
//            }
//            var c = text.charAt(i);
//            if (line > skipEnd)
//                buf.append(c == '\r' ? '\n' : c);
//            switch (c) {
//                case '\n' -> line++;
//                case '\r' -> {
//                    if (i + 1 < text.length() && text.charAt(i + 1) == '\n')
//                        i++;
//                    line++;
//                }
//            }
//        }
//        return buf.toString();
//    }
//
//
//    @SneakyThrows
//    public static void main(String[] args) {
//        var text = Files.readString(Path.of("/tmp/kiwi-works/1000023883/src/main.kiwi"));
//        var diff = Files.readString(Path.of("/Users/leen/workspace/kiwi-console/wad.txt"));
//        Files.writeString(
//                Path.of("/Users/leen/workspace/kiwi-console/server/src/test/resources/main.kiwi"),
//                apply(text, diff)
//        );
//    }
//
//}
