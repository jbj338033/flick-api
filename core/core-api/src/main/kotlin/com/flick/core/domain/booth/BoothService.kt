package com.flick.core.domain.booth

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.UserRepository
import com.flick.storage.redis.KioskSessionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BoothService(
    private val boothRepository: BoothRepository,
    private val userRepository: UserRepository,
    private val kioskSessionRepository: KioskSessionRepository,
) {
    fun getBooth(boothId: UUID): Booth =
        boothRepository.findByIdOrNull(boothId)
            ?: throw CoreException(ErrorType.BOOTH_NOT_FOUND)

    fun getAllBooths(): List<Booth> = boothRepository.findAll()

    fun getMyBooth(userId: UUID): Booth? = boothRepository.findByUserId(userId)

    fun getBoothByPairingCode(pairingCode: String): Booth? = boothRepository.findByPairingCode(pairingCode)

    fun getKioskSessionCount(boothId: UUID): Long = kioskSessionRepository.countByBoothId(boothId)

    @Transactional
    fun createBooth(
        userId: UUID,
        name: String,
        description: String?,
    ): Booth {
        if (boothRepository.existsByUserId(userId)) throw CoreException(ErrorType.BOOTH_ALREADY_EXISTS)
        val user = userRepository.getReferenceById(userId)
        val pairingCode = generateUniquePairingCode()
        return boothRepository.save(
            Booth(name = name, description = description, pairingCode = pairingCode, user = user),
        )
    }

    fun verifyOwner(
        boothId: UUID,
        userId: UUID,
    ) {
        val booth = getBooth(boothId)
        if (booth.user.id != userId) throw CoreException(ErrorType.FORBIDDEN)
    }

    @Transactional
    fun updateBooth(
        boothId: UUID,
        name: String?,
        description: String?,
    ): Booth {
        val booth = getBooth(boothId)
        name?.let { booth.name = it }
        description?.let { booth.description = it }
        return booth
    }

    @Transactional
    fun generatePairingCode(boothId: UUID): Booth {
        val booth = getBooth(boothId)
        kioskSessionRepository.deleteAllByBoothId(boothId)
        booth.pairingCode = generateUniquePairingCode()
        return booth
    }

    private fun generateUniquePairingCode(): String {
        repeat(100) {
            val code = (1000..9999).random().toString()
            if (boothRepository.findByPairingCode(code) == null) return code
        }
        throw CoreException(ErrorType.INTERNAL_ERROR)
    }
}
