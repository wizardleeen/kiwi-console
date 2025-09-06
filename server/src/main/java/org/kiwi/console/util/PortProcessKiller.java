package org.kiwi.console.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to find and kill a process that is listening on a specific TCP port.
 * This is designed to be cross-platform, supporting Windows, macOS, and Linux.
 */
public class PortProcessKiller {

    /**
     * Finds and forcefully kills any process listening on the specified port.
     *
     * @param port The TCP port to check.
     * @return true if a process was found and successfully terminated, false otherwise.
     */
    public static boolean killProcessOnPort(int port) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                return killWindowsProcess(port);
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                return killUnixProcess(port);
            } else {
                System.err.println("Unsupported Operating System: " + os);
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occurred while trying to kill process on port " + port);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean killUnixProcess(int port) throws IOException, InterruptedException {
        // Use 'lsof -t' to get only the PID, which is much cleaner to parse.
        String findPidCommand = "lsof -t -i:" + port;
        String pid = executeAndGetOutput(findPidCommand);

        if (pid != null && !pid.trim().isEmpty()) {
            System.out.printf("Process found on port %d with PID: %s. Attempting to kill it.%n", port, pid.trim());
            // Use 'kill -9' for a forceful termination (SIGKILL)
            String killCommand = "kill -9 " + pid.trim();
            int exitCode = executeCommand(killCommand);
            if (exitCode == 0) {
                System.out.printf("Successfully killed process with PID: %s%n", pid.trim());
                return true;
            } else {
                System.err.printf("Failed to kill process with PID: %s. Exit code: %d%n", pid.trim(), exitCode);
                return false;
            }
        } else {
            System.out.println("No process found listening on port " + port + ". Nothing to kill.");
            return false;
        }
    }

    private static boolean killWindowsProcess(int port) throws IOException, InterruptedException {
        // Find PID using netstat and findstr
        String findPidCommand = "cmd.exe /c netstat -ano | findstr :" + port;
        String output = executeAndGetOutput(findPidCommand);

        if (output != null && !output.isEmpty()) {
            // Parsing the output of 'netstat -ano'
            // Example line: TCP    127.0.0.1:9222         0.0.0.0:0              LISTENING       12345
            String[] lines = output.split("\\R");
            for (String line : lines) {
                if (line.contains("LISTENING")) {
                    String[] parts = line.trim().split("\\s+");
                    String pid = parts[parts.length - 1];
                    System.out.printf("Process found on port %d with PID: %s. Attempting to kill it.%n", port, pid);
                    
                    // Kill PID using taskkill
                    String killCommand = "taskkill /F /PID " + pid;
                    int exitCode = executeCommand(killCommand);

                    if (exitCode == 0) {
                        System.out.printf("Successfully killed process with PID: %s%n", pid);
                        return true; // Assuming first listening process is the one we want.
                    } else {
                        System.err.printf("Failed to kill process with PID: %s. Exit code: %d%n", pid, exitCode);
                    }
                }
            }
        } else {
            System.out.println("No process found listening on port " + port + ". Nothing to kill.");
        }
        return false;
    }

    private static String executeAndGetOutput(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        process.waitFor(5, TimeUnit.SECONDS); // Wait for the process to finish
        return output.toString().trim();
    }

    private static int executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            System.err.println("Command timed out: " + command);
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }
}