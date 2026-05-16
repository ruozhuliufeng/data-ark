package com.dataark.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CommandRunner {

    public CommandResult run(List<String> command, Map<String, String> environment, File workDir) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        if (workDir != null) {
            builder.directory(workDir);
        }
        if (environment != null) {
            builder.environment().putAll(environment);
        }

        StringBuilder output = new StringBuilder();
        try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output.toString());
        } catch (IOException e) {
            return new CommandResult(127, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandResult(130, "Command interrupted: " + e.getMessage());
        }
    }
}
