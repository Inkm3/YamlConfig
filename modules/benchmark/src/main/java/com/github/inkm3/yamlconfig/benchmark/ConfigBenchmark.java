package com.github.inkm3.yamlconfig.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
public class ConfigBenchmark {
    @Param({"16", "256"}) public int size;
    private ConfigWorkload workload;

    @Setup public void setup() { workload = new ConfigWorkload(size); }
    @Benchmark public Object codecRoundTrip() { return workload.codecRoundTrip(); }
    @Benchmark public Object load() { return workload.load(); }
    @Benchmark public Object moveAndUpdate() { return workload.moveAndUpdate(); }
    @Benchmark public Object minimalReduction() { return workload.minimalReduction(); }
}
