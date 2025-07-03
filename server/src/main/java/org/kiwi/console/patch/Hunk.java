package org.kiwi.console.patch;

record Hunk(HunkHeader header, String content) {

    Operation op() {
        return header.op();
    }

    int startLine() {
        return header.startLine();
    }

    int endLine() {
        return header.endLine();
    }

}
