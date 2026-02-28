package com.logguardian.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class DockerContainerService {

    private final DockerClient client;

    public List<Container> getRunningContainerList(){
        List<Container> runningContainers = client.listContainersCmd()
                .exec().
                stream().
                toList();
        log.info("Running containers [{}]", runningContainers);
        return runningContainers;
    }
}
