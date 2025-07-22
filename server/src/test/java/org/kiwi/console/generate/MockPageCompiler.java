package org.kiwi.console.generate;

import java.util.Map;

public class MockPageCompiler extends MockCompiler {

    @Override
    protected void initWorkdir(Map<String, String> workdir) {
        workdir.put("package.json", """
                {
                    "version": "1.0
                }
                """);
    }
}
