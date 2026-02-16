package com.flick.core.domain.kiosk

import com.flick.core.domain.booth.BoothService
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.redis.KioskSessionRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KioskPairingService(
    private val boothService: BoothService,
    private val kioskSessionRepository: KioskSessionRepository,
) {
    fun pair(pairingCode: String): KioskPairResult {
        val booth =
            boothService.getBoothByPairingCode(pairingCode)
                ?: throw CoreException(ErrorType.BOOTH_NOT_FOUND)
        val token = UUID.randomUUID().toString()
        kioskSessionRepository.save(token, booth.id)
        return KioskPairResult(token = token, boothId = booth.id, boothName = booth.name)
    }
}

data class KioskPairResult(
    val token: String,
    val boothId: UUID,
    val boothName: String,
)
