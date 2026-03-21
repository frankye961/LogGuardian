package com.logguardian.runners;

import com.github.dockerjava.api.model.Container;
import com.logguardian.service.docker.DockerContainerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static com.logguardian.runners.Command.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "logguardian.mode", havingValue = "cli")
public class CliRunnerService implements CommandLineRunner {

    private final DockerContainerService dockerContainerService;

    @Override
    public void run(String... args) {
        exit(execute(args));
    }

    int execute(String... args) {
        if (args.length == 0) {
            printHelp();
            return 0;
        }

        String command = args[0].trim().toLowerCase();

        switch (command) {
            case LIST -> {
                listContainers();
                return 0;
            }
            case TAIL_ALL -> {
                tailAllContainers();
                return 0;
            }
            case TAIL_ONE -> {
                if (args.length < 2) {
                    System.err.println("Missing container id for tail-one command.");
                    printHelp();
                    return 1;
                }
                tailOneContainer(args[1]);
                return 0;
            }
            case SHELL_COMMAND -> {
                runInteractiveShell();
                return 0;
            }
            case HELP, HELP_2, H_COMMAND -> {
                printHelp();
                return 0;
            }
            default -> {
                System.err.printf("Unknown command: %s%n%n", command);
                printHelp();
                return 1;
            }
        }
    }

    private void listContainers() {
        List<Container> containers = dockerContainerService.getRunningContainerList();

        if (containers.isEmpty()) {
            System.out.println("No running containers found.");
            return;
        }

        System.out.printf("%-15s %-30s %-25s%n", "CONTAINER ID", "NAME", "STATUS");
        System.out.println("-------------------------------------------------------------------------------");

        containers.stream()
                .map(this::toRow)
                .forEach(row -> System.out.printf(
                        "%-15s %-30s %-25s%n",
                        row.shortId(),
                        row.name(),
                        row.status()
                ));
    }

    private void tailAllContainers() {
        List<Container> containers = dockerContainerService.getRunningContainerList();
        if (containers.isEmpty()) {
            System.out.println("No running containers found.");
            return;
        }

        List<Disposable> subscriptions = containers.stream()
                .map(Container::getId)
                .peek(containerId -> System.out.printf("Current container id: %s%n", containerId))
                .map(dockerContainerService::startStream)
                .toList();

        System.out.printf("Started tailing %d running containers.%n", subscriptions.size());

        try {
            blockUntilInterrupted(subscriptions);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for containers to stream", e);
            Thread.currentThread().interrupt();
        }
    }

    private void tailOneContainer(String containerId) {
        try {
            System.out.printf("Started tailing container %s%n", containerId);
            var containerStream = dockerContainerService.startStream(containerId);
            blockUntilInterrupted(List.of(containerStream));
        }  catch (InterruptedException e) {
            log.error("Interrupted while waiting for containers to start!", e);
            Thread.currentThread().interrupt();
        }
    }

    protected void blockUntilInterrupted(List<Disposable> subscriptions) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            subscriptions.forEach(Disposable::dispose);
            dockerContainerService.stopAllStreams();
            latch.countDown();
        }));

        latch.await();
    }

    private void runInteractiveShell() {
        Scanner scanner = new Scanner(System.in);

        System.out.println(WELCOME_LINE_1);
        System.out.println(WELCOME_LINE_2);

        while (true) {
            System.out.print(BEGIN_LINE);

            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            if (line.isBlank()) {
                continue;
            }

            if (EXIT.equalsIgnoreCase(line) || QUIT.equalsIgnoreCase(line)) {
                break;
            }

            if (HELP.equalsIgnoreCase(line)) {
                printHelp();
                continue;
            }

            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case LIST -> listContainers();
                case TAIL_ALL -> tailAllContainers();
                case TAIL_ONE -> {
                    if (parts.length < 2) {
                        System.err.println("Usage: tail-one <containerId>");
                        continue;
                    }
                    tailOneContainer(parts[1]);
                }
                default -> System.err.printf("Unknown command: %s%n", line);
            }
        }
        System.out.println("Bye.");
    }

    private void printHelp() {
        System.out.println("""
                Usage:
                  java -jar logguardian.jar list
                  java -jar logguardian.jar tail-all
                  java -jar logguardian.jar tail-one <containerId>
                  java -jar logguardian.jar shell

                Commands:
                  list                  List running containers
                  tail-all              Start tailing all running containers
                  tail-one <id>         Start tailing one container
                  shell                 Start interactive shell
                  help                  Show this help
                """);
    }

    private ContainerRow toRow(Container container) {
        String shortId = container.getId() != null && container.getId().length() > 12
                ? container.getId().substring(0, 12)
                : safe(container.getId());

        String name = extractName(container);
        String status = safe(container.getStatus());

        return new ContainerRow(shortId, name, status);
    }

    private String extractName(Container container) {
        if (container.getNames() == null || container.getNames().length == 0) {
            return UNKNOWN;
        }

        return Arrays.stream(container.getNames())
                .findFirst()
                .orElse(UNKNOWN);
    }

    private String safe(String value) {
        return StringUtils.isEmpty(value) ? UNKNOWN : value;
    }

    protected void exit(int code) {
        System.exit(code);
    }

}
