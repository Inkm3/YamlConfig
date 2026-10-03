package com.github.inkm3.yamlconfig.javaapi;

import com.github.inkm3.yamlconfig.source.YamlSource;
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;

/** A state-changing output, not a recording-only mock. */
final class JavaMemorySource implements YamlSource {
    String text;
    boolean present;
    boolean failNextCommit;
    int opens;
    int writes;
    int commits;

    JavaMemorySource(String text, boolean present) {
        this.text = text;
        this.present = present;
    }

    @Override public String getDescription() { return "java-memory"; }
    @Override public boolean exists() { return present; }
    @Override public Reader openReader() {
        opens++;
        return new StringReader(present ? text : "");
    }
    @Override public YamlWriteTransaction beginWrite() {
        writes++;
        return new YamlWriteTransaction() {
            private final StringWriter staging = new StringWriter();
            @Override public Writer getWriter() { return staging; }
            @Override public void commit() {
                if (failNextCommit) {
                    failNextCommit = false;
                    throw new UncheckedIOException(new IOException("rejected before publication"));
                }
                text = staging.toString();
                present = true;
                commits++;
            }
            @Override public void close() { }
        };
    }
}
