package com.logguardian.rest;

import com.github.dockerjava.api.model.Container;
import com.logguardian.model.LogLine;
import com.logguardian.service.DockerContainerService;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
public class DockerController {

    private final DockerContainerService service;

    @GetMapping(value = "/running/containers")
    public ResponseEntity<List<Container>> retrieveRunningContainer(){
        try {
            return ResponseEntity.ok(service.getRunningContainerList());
        }catch(Exception e){
            log.info(Arrays.toString(e.getStackTrace()));
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping(value = "/show-logs", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LogLine> showLogLines(@QueryParam("containerId") String containerId){
        try {
            return service.streamLogs(containerId);
        }catch(Exception e){
            log.info(Arrays.toString(e.getStackTrace()));
            return Flux.empty();
        }
    }
}
