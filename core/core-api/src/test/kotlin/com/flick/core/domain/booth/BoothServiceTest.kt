package com.flick.core.domain.booth

import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.UserRepository
import com.flick.storage.redis.KioskSessionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class BoothServiceTest :
    FunSpec({
        val boothRepository = mockk<BoothRepository>()
        val userRepository = mockk<UserRepository>()
        val kioskSessionRepository = mockk<KioskSessionRepository>(relaxed = true)
        val service = BoothService(boothRepository, userRepository, kioskSessionRepository)

        val owner = User(dauthId = "owner", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val booth = Booth(name = "타코야끼", description = "맛있는 타코야끼", pairingCode = "1234", user = owner)

        test("getBooth 정상") {
            every { boothRepository.findByIdOrNull(booth.id) } returns booth

            service.getBooth(booth.id).name shouldBe "타코야끼"
        }

        test("getBooth 존재하지 않음 → BOOTH_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { boothRepository.findByIdOrNull(id) } returns null

            val exception = shouldThrow<CoreException> { service.getBooth(id) }
            exception.type shouldBe ErrorType.BOOTH_NOT_FOUND
        }

        test("getAllBooths 정상") {
            every { boothRepository.findAll() } returns listOf(booth)

            service.getAllBooths().size shouldBe 1
        }

        test("getMyBooth 정상") {
            every { boothRepository.findByUserId(owner.id) } returns booth

            service.getMyBooth(owner.id)?.name shouldBe "타코야끼"
        }

        test("getKioskSessionCount 정상") {
            every { kioskSessionRepository.countByBoothId(booth.id) } returns 3L

            service.getKioskSessionCount(booth.id) shouldBe 3L
        }

        test("createBooth 정상") {
            every { boothRepository.existsByUserId(owner.id) } returns false
            every { userRepository.getReferenceById(owner.id) } returns owner
            every { boothRepository.findByPairingCode(any()) } returns null
            every { boothRepository.save(any<Booth>()) } answers { firstArg() }

            val result = service.createBooth(owner.id, "새부스", "설명")
            result.name shouldBe "새부스"
        }

        test("createBooth 이미 부스 있음 → BOOTH_ALREADY_EXISTS") {
            every { boothRepository.existsByUserId(owner.id) } returns true

            val exception = shouldThrow<CoreException> { service.createBooth(owner.id, "부스", null) }
            exception.type shouldBe ErrorType.BOOTH_ALREADY_EXISTS
        }

        test("verifyOwner 정상") {
            every { boothRepository.findByIdOrNull(booth.id) } returns booth

            service.verifyOwner(booth.id, owner.id)
        }

        test("verifyOwner 소유자 아님 → FORBIDDEN") {
            every { boothRepository.findByIdOrNull(booth.id) } returns booth
            val otherId = UUID.randomUUID()

            val exception = shouldThrow<CoreException> { service.verifyOwner(booth.id, otherId) }
            exception.type shouldBe ErrorType.FORBIDDEN
        }

        test("updateBooth 정상") {
            every { boothRepository.findByIdOrNull(booth.id) } returns booth

            val result = service.updateBooth(booth.id, "변경됨", null)
            result.name shouldBe "변경됨"
        }

        test("generatePairingCode → 기존 세션 삭제 확인") {
            every { boothRepository.findByIdOrNull(booth.id) } returns booth
            every { boothRepository.findByPairingCode(any()) } returns null

            val oldCode = booth.pairingCode
            service.generatePairingCode(booth.id)

            verify { kioskSessionRepository.deleteAllByBoothId(booth.id) }
            booth.pairingCode shouldNotBe oldCode
        }
    })
