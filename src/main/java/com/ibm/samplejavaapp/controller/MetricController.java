package com.ibm.samplejavaapp.controller;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.common.TextFormat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.StringWriter;
import java.io.Writer;

@RestController
public class MetricController {

    enum MetricsType {
        FILE_UPLOAD_COUNT,
        CPU_IDLE_TIME,
        FILE_UPLOAD_DURATION
    }

    CollectorRegistry collectorRegistry;

    private final Map<String, Object> metricsMap = new HashMap<String, Object>();

    public MetricController(CollectorRegistry collectorRegistry) {
        this.collectorRegistry = collectorRegistry;
    }

    public Object getMetrics(String applicationName, String subComponent, String statsGroup, MetricsType metricsType) {
        Object metrics = null;
        String key = applicationName + "_" + subComponent + "_" + statsGroup + "_" + metricsType;

        if (metricsMap.containsKey(key))
            return metricsMap.get(key);

        if (metricsType == MetricsType.FILE_UPLOAD_COUNT) {
            metrics = Counter.build()
                    .name("mas_" + applicationName + "_" + subComponent + "_" + statsGroup + "_file_upload_count")
                    .help("Number of times the file has been uploaded.")
                    .register(collectorRegistry);

            metricsMap.put(key, (Counter) metrics);
        }

        if (metricsType == MetricsType.CPU_IDLE_TIME) {
            metrics = Gauge.build()
                    .name("mas_" + applicationName + "_" + subComponent + "_" + statsGroup + "_cpu_idle_time")
                    .help("Idle time of CPU.")
                    .register(collectorRegistry);

            metricsMap.put(key, (Gauge) metrics);
        }

        if (metricsType == MetricsType.FILE_UPLOAD_DURATION) {
            metrics = Histogram.build()
                    .name("mas_" + applicationName + "_" + subComponent + "_" + statsGroup + "_file_upload_duration")
                    .help("Time taken to upload the file.")
                    .buckets(0.15, 0.2, 0.25, 0.30, 0.35, 0.40, 1, 2.5, 5, 7.5, 10)
                    .register(collectorRegistry);

            metricsMap.put(key, (Histogram) metrics);
        }

        return metrics;

    }

    @GetMapping(value = "/application_name/{application_name}/subcomponent/{subcomponent}/statsgroup/{statsgroup}/file_upload")
    public String simulateFileUpload(@PathVariable("application_name") String applicationName,
            @PathVariable("subcomponent") String subComponent, @PathVariable("statsgroup") String statsGroup)
            throws InterruptedException {

        Counter fileUploadCount = (Counter) getMetrics(applicationName, subComponent, statsGroup,
                MetricsType.FILE_UPLOAD_COUNT);
        fileUploadCount.inc();

        double cpu = 0;
        String command[] = { "/bin/sh", "-c", "top -n 1 -b | grep \"%Cpu\" | cut -d ',' -f 4 | cut -d ' ' -f 2" };
        String s;

        try {
            Process proc = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((s = stdInput.readLine()) != null) {
                cpu = Double.parseDouble(s.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Gauge cpuIdleTime = (Gauge) getMetrics(applicationName, subComponent, statsGroup, MetricsType.CPU_IDLE_TIME);
        cpuIdleTime.set(cpu);

        int min = 200;
        int max = 400;
        int sleepDuration = (int) (Math.random() * (max - min + 1) + min);

        Histogram fileUploadDuration = (Histogram) getMetrics(applicationName, subComponent, statsGroup,
                MetricsType.FILE_UPLOAD_DURATION);
        Histogram.Timer timer = fileUploadDuration.startTimer();

        sleep(sleepDuration);

        timer.observeDuration();

        return String.format("Time taken to upload a file is %s ms.", sleepDuration);
    }

    @GetMapping(value = "/application_name/{application_name}/subcomponent/{subcomponent}/statsgroup/{statsgroup}/bigger_file_upload")
    public String simulateBiggerFileUpload(@PathVariable("application_name") String applicationName,
            @PathVariable("subcomponent") String subComponent, @PathVariable("statsgroup") String statsGroup)
            throws InterruptedException {

        int min = 5;
        int max = 10;
        int sleepDuration = (int) (Math.random() * (max - min + 1) + min);
        Histogram fileUploadDuration = (Histogram) getMetrics(applicationName, subComponent, statsGroup,
                MetricsType.FILE_UPLOAD_DURATION);
        Histogram.Timer timer = fileUploadDuration.startTimer();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(sleepDuration);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return "Simulating bigger file upload";
        });

        future.thenApply(result -> {
            Counter fileUploadCount = (Counter) getMetrics(applicationName, subComponent, statsGroup,
                    MetricsType.FILE_UPLOAD_COUNT);
            fileUploadCount.inc();
            timer.observeDuration();
            return "Metrics populated as file upload is success";
        });

        new Thread(() -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }).start();

        return String.format("Time taken to upload a bigger file is %s s.", sleepDuration);
    }

    @GetMapping(value = "/all_metrics", produces = { TextFormat.CONTENT_TYPE_004 })
    public String getMetrics() throws IOException {
        Writer writer = new StringWriter();
        try {
            TextFormat.writeFormat(TextFormat.CONTENT_TYPE_004, writer, collectorRegistry.metricFamilySamples());
            return writer.toString();
        } finally {
            writer.close();
        }
    }
}
