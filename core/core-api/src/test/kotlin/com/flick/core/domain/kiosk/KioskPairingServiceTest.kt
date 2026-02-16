package com.flick.core.domain.kiosk

import com.flick.core.domain.booth.BoothService
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.User
import com.flick.storage.redis.KioskSessionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class KioskPairingServiceTest :
    FunSpec({
        val boothService = mockk<BoothService>()
        val kioskSessionRepository = mockk<KioskSessionRepository>(relaxed = true)
        val service = KioskPairingService(boothService, kioskSessionRepository)

        test("pair 정상 → 토큰 및 부스 정보 반환") {
            val user = User(dauthId = "u1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
            val booth = Booth(name = "타코야끼", pairingCode = "1234", user = user)

            every { boothService.getBoothByPairingCode("1234") } returns booth

            val result = service.pair("1234")
            result.token.shouldNotBeBlank()
            result.boothId shouldBe booth.id
            result.boothName shouldBe "타코야끼"
            verify { kioskSessionRepository.save(any(), eq(booth.id)) }
        }

        test("pair 없는 페어링 코드 → BOOTH_NOT_FOUND") {
            every { boothService.getBoothByPairingCode("9999") } returns null

            val exception = shouldThrow<CoreException> { service.pair("9999") }
            exception.type shouldBe ErrorType.BOOTH_NOT_FOUND
        }
    })
