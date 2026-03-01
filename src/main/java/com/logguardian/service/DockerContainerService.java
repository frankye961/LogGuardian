package com.logguardian.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.logguardian.model.LogLine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.logguardian.parser.mapper.LogLineMapper.map;
import static org.apache.hc.core5.io.Closer.closeQuietly;

@Slf4j
@Service
@AllArgsConstructor
public class DockerContainerService {

    private final DockerClient client;

    public List<Container> getRunningContainerList() {
        try {
            return getRunningContainers();
        } catch (Exception e) {
            log.error("Docker listContainers failed. Problem: ", e);
            throw new RuntimeException();
        }
    }

    public Flux<LogLine> streamLogs(String containerId){
        return Flux.create(line -> {
            ResultCallback<Frame> callback = new ResultCallback.Adapter<Frame>() {
                @Override public void onNext(Frame frame) {
                    line.next(map(frame, containerId));
                }
                @Override public void onError(Throwable t) {
                    line.error(t);
                }
                @Override public void onComplete() {
                    line.complete();
                }
            };
            attachContainerCmd(containerId, callback);
            line.onCancel(() -> closeQuietly(callback));
            line.onDispose(() -> closeQuietly(callback));
        });
    }

    private List<Container> getRunningContainers(){
        return client.
                listContainersCmd().
                exec();
    }

    private void attachContainerCmd(String containerId, ResultCallback<Frame> callback){
        client.logContainerCmd(containerId).
                withContainerId(containerId).
                withStdErr(true)
                .withStdOut(true)
                .withTailAll().exec(callback);
    }
}
