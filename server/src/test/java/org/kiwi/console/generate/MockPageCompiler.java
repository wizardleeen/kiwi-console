package org.kiwi.console.generate;

import java.nio.file.Path;

public class MockPageCompiler extends MockCompiler {

    @Override
    public void reset(String projectName, String templateRepo, String branch) {
        var empty = isEmpty();
        super.reset(projectName, templateRepo, branch);
        if (empty) {
            addFile(projectName, new SourceFile(Path.of("package.json"), """
                    {
                        "version": "1.0
                    }
                    """));
            commit(projectName, "initial commit");
        }
    }

}
