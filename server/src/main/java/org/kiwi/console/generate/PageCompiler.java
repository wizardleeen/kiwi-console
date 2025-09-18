package org.kiwi.console.generate;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface PageCompiler extends Compiler {

    @Nullable Path getSourceMapPath(String projectName);

}
