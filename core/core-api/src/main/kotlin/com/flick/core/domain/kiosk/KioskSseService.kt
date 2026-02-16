package com.flick.core.domain.kiosk

import com.flick.support.logging.logger
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class KioskSseService {
    private val log = logger()
    private val emitters = ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(boothId: UUID): SseEmitter {
        val boothEmitters = emitters.computeIfAbsent(boothId) { CopyOnWriteArrayList() }
        val emitter = SseEmitter(180_000L)

        emitter.onCompletion { boothEmitters.remove(emitter) }
        emitter.onTimeout { boothEmitters.remove(emitter) }
        emitter.onError { boothEmitters.remove(emitter) }

        runCatching {
            emitter.send(SseEmitter.event().name("connected").data("connected"))
            boothEmitters.add(emitter)
        }.onFailure {
            emitter.complete()
        }

        return emitter
    }

    fun sendToKiosk(
        boothId: UUID,
        eventName: String,
        data: Any,
    ) {
        val boothEmitters = emitters[boothId] ?: return

        boothEmitters.removeAll { emitter ->
            runCatching {
                emitter.send(SseEmitter.event().name(eventName).data(data))
            }.onFailure {
                log.warn { "Failed to send SSE to booth $boothId: ${it.message}" }
            }.isFailure
        }
    }
}
