package com.logguardian.runners;

import com.github.dockerjava.api.model.Container;
import com.logguardian.rest.model.ContainerRulesetRequest;
import com.logguardian.rest.model.RuleEnum;
import com.logguardian.service.docker.DockerContainerService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.logguardian.runners.Command.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "logguardian.mode", havingValue = "cli")
public class CliRunner implements CommandLineRunner {

    private final DockerContainerService dockerContainerService;

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            printHelp();
            exit(0);
            return;
        }

        String command = args[0].trim().toLowerCase();

        switch (command) {
            case LIST -> {
                listContainers();
                exit(0);
            }
            case TAIL_ALL-> {
                tailAllContainers();
                exit(0);
            }
            case TAIL_ONE -> {
                if (args.length < 2) {
                    System.err.println("Missing container id for tail-one command.");
                    printHelp();
                    exit(1);
                    return;
                }
                tailOneContainer(args[1]);
                exit(0);
            }
            case SHELL_COMMAND -> {
                runInteractiveShell();
                exit(0);
            }
            case HELP, HELP_2, H_COMMAND -> {
                printHelp();
                exit(0);
            }
            default -> {
                System.err.printf("Unknown command: %s%n%n", command);
                printHelp();
                exit(1);
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
        ContainerRulesetRequest request = new ContainerRulesetRequest();
        request.setRule(RuleEnum.ALL);

        dockerContainerService.startTailing(request);
        System.out.println("Started tailing all running containers.");
    }

    private void tailOneContainer(String containerId) {
        ContainerRulesetRequest request = new ContainerRulesetRequest();
        request.setRule(RuleEnum.EQUAL);

        // adjust this line if your DTO uses a different field name
        request.setContainerId(containerId);

        dockerContainerService.startTailing(request);
        System.out.printf("Started tailing container %s%n", containerId);
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

    private void exit(int code) {
        System.exit(code);
    }

}