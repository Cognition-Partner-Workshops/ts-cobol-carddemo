package com.carddemo.trantypedynamic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {BatchAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class DynamicApplication {
    public static void main(String[] args) throws Exception {
        int[] rc = {0};
        try (ConfigurableApplicationContext context = SpringApplication.run(DynamicApplication.class, args)) {
            var repository = new ResourcelessJobRepository();
            var step = new StepBuilder("STEP1", repository)
                    .tasklet((contribution, chunk) -> {
                        String url = "jdbc:postgresql://" + env("PGHOST", "localhost") + ":"
                                + env("PGPORT", "5432") + "/" + env("PGDATABASE", "carddemo")
                                + "?currentSchema=carddemo";
                        try (var connection = DriverManager.getConnection(url,
                                env("PGUSER", "carddemo"), env("PGPASSWORD", "carddemo"))) {
                            connection.setAutoCommit(false);
                            String program = args.length == 0 ? "" : args[0];
                            rc[0] = new DynamicMaintenance(System.out).run(connection,
                                    DynamicMaintenance.assemble(read("DYNSQL", 80)), program,
                                    program.isEmpty() ? List.of() : read("INPFILE", 53));
                        }
                        return RepeatStatus.FINISHED;
                    }, new ResourcelessTransactionManager()).build();
            var job = new JobBuilder("MNTTRDYN", repository).start(step).build();
            var launcher = new TaskExecutorJobLauncher();
            launcher.setJobRepository(repository);
            launcher.afterPropertiesSet();
            var execution = launcher.run(job, new JobParameters());
            if (!execution.getAllFailureExceptions().isEmpty()) {
                throw new IllegalStateException("MNTTRDYN failed", execution.getAllFailureExceptions().getFirst());
            }
        }
        System.exit(rc[0]);
    }

    private static List<String> read(String dd, int width) throws Exception {
        return records(Files.readAllBytes(Path.of(System.getenv("DD_" + dd))), width);
    }

    static List<String> records(byte[] bytes, int width) {
        if (bytes.length % width != 0) {
            throw new IllegalArgumentException("Incomplete fixed-width record");
        }
        List<String> records = new ArrayList<>();
        for (int offset = 0; offset < bytes.length; offset += width) {
            records.add(new String(bytes, offset, width, StandardCharsets.ISO_8859_1));
        }
        return records;
    }

    private static String env(String key, String fallback) {
        return System.getenv().getOrDefault(key, fallback);
    }
}
