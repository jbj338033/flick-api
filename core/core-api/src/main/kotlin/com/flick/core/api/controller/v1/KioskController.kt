package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.KioskPairRequest
import com.flick.core.api.controller.v1.response.BoothRef
import com.flick.core.api.controller.v1.response.KioskPairResponse
import com.flick.core.domain.kiosk.KioskPairingService
import com.flick.core.domain.kiosk.KioskSseService
import com.flick.support.security.KioskPrincipal
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/kiosk")
class KioskController(
    private val kioskSseService: KioskSseService,
    private val kioskPairingService: KioskPairingService,
) {
    @PostMapping("/pair")
    fun pair(
        @Valid @RequestBody request: KioskPairRequest,
    ): KioskPairResponse {
        val result = kioskPairingService.pair(request.pairingCode)
        return KioskPairResponse(token = result.token, booth = BoothRef(id = result.boothId, name = result.boothName))
    }

    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
        @AuthenticationPrincipal principal: KioskPrincipal,
    ): SseEmitter = kioskSseService.subscribe(principal.boothId)
}
