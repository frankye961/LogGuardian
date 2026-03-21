package com.logguardian.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.logguardian.model.LogLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.logguardian.mapper.LogLineMapper.map;
import static org.apache.hc.core5.io.Closer.closeQuietly;

@Slf4j
@Service
public class DockerContainerService {

    private final DockerClient client;
    private final DockerLogPipelineService dockerLogPipelineService;
    private final ConcurrentMap<String, Disposable> activeContainers = new ConcurrentHashMap<>();

    public DockerContainerService(DockerClient client, DockerLogPipelineService dockerLogPipelineService) {
        this.client = client;
        this.dockerLogPipelineService = dockerLogPipelineService;
    }

    public List<Container> getRunningContainerList() {
        try {
            return getRunningContainers();
        } catch (Exception e) {
            log.error("Docker listContainers failed. Problem: ", e);
            throw new IllegalStateException("Failed to list running Docker containers", e);
        }
    }


    public Disposable startStream(String containerId) {
        Disposable currentStream = activeContainers.get(containerId);
        if (currentStream != null && !currentStream.isDisposed()) {
            log.info("Stream already active for container {}", containerId);
            return currentStream;
        }

        Disposable subscription = dockerLogPipelineService.process(containerId, streamLogs(containerId))
                .doOnError(error -> log.error("Stream failed for container {}", containerId, error))
                .doFinally(signal -> activeContainers.remove(containerId))
                .subscribe();

        activeContainers.put(containerId, subscription);
        return subscription;
    }

    /**
     * streams logs from a container
     *
     * @param containerId
     * @return
     */
    public Flux<LogLine> streamLogs(String containerId) {
        log.info("stream started...");
        return Flux.create(line -> {
            ResultCallback<Frame> callback = new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    line.next(map(frame, containerId));
                }

                @Override
                public void onError(Throwable t) {
                    line.error(t);
                }

                @Override
                public void onComplete() {
                    line.complete();
                }
            };
            attachContainerCmd(containerId, callback);
            line.onCancel(() -> closeQuietly(callback));
            line.onDispose(() -> closeQuietly(callback));
        });
    }

    /**
     * retrieves all the running containers
     *
     * @return
     */
    private List<Container> getRunningContainers() {
        return client.
                listContainersCmd().
                exec();
    }

    private void attachContainerCmd(String containerId, ResultCallback<Frame> callback) {
        try {
            client.logContainerCmd(containerId).
                    withStdErr(true)
                    .withStdOut(true)
                    .withFollowStream(true)
                    .withTailAll().exec(callback);
        } catch (Exception e) {
            log.error("Error in attaching to the container: ", e);
            throw new IllegalStateException("Failed to attach to container " + containerId, e);
        }
    }

    public void stopAllStreams() {
        activeContainers.values().forEach(Disposable::dispose);
        activeContainers.clear();
    }
}
