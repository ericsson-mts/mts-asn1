/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.plugin;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ConstantsGenerator {
    private JavaFile javaFile;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    ConstantsGenerator(String className, List<String> grammarFiles, String generatedPackageName) throws IOException {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for (String grammarFile : grammarFiles) {
            builder = new IdentifierVisitor(builder).beginVisit(new FileInputStream(new File(grammarFile)));
        }
        this.javaFile = JavaFile.builder(generatedPackageName, builder.build())
                .build();
    }

    public void writeFile(Path filePath) throws IOException {
        javaFile.writeTo(filePath);
    }

    void writeFile(File file) throws IOException {
        if (file.mkdirs()) {
            logger.trace("Create directories for path : {}", file.getAbsolutePath());
        }
        javaFile.writeTo(file);
    }

    public String getCode() {
        return javaFile.toString();
    }
}
