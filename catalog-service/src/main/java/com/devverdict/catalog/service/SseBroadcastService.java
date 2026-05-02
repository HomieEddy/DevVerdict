package com.devverdict.catalog.service;

import com.devverdict.catalog.dto.FrameworkRatingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(SseBroadcastService.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, List<SseEmitter>> emittersByFramework = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long frameworkId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        List<SseEmitter> frameworkEmitters = emittersByFramework.computeIfAbsent(
            frameworkId, k -> new CopyOnWriteArrayList<>()
        );
        frameworkEmitters.add(emitter);
        logger.debug("SSE subscription added for frameworkId={}. Total emitters: {}",
            frameworkId, frameworkEmitters.size());

        emitter.onCompletion(() -> removeEmitter(frameworkId, emitter));
        emitter.onTimeout(() -> removeEmitter(frameworkId, emitter));
        emitter.onError((e) -> removeEmitter(frameworkId, emitter));

        return emitter;
    }

    public void broadcast(Long frameworkId, FrameworkRatingEvent event) {
        List<SseEmitter> frameworkEmitters = emittersByFramework.get(frameworkId);
        if (frameworkEmitters == null || frameworkEmitters.isEmpty()) {
            logger.debug("No SSE subscribers for frameworkId={}", frameworkId);
            return;
        }

        for (SseEmitter emitter : frameworkEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("rating-update")
                    .data(event));
            } catch (IOException e) {
                logger.debug("Failed to send SSE event to emitter for frameworkId={}, removing emitter",
                    frameworkId);
                removeEmitter(frameworkId, emitter);
            }
        }
    }

    private void removeEmitter(Long frameworkId, SseEmitter emitter) {
        List<SseEmitter> frameworkEmitters = emittersByFramework.get(frameworkId);
        if (frameworkEmitters != null) {
            frameworkEmitters.remove(emitter);
            logger.debug("SSE emitter removed for frameworkId={}. Remaining: {}",
                frameworkId, frameworkEmitters.size());
            if (frameworkEmitters.isEmpty()) {
                emittersByFramework.remove(frameworkId);
            }
        }
    }
}
