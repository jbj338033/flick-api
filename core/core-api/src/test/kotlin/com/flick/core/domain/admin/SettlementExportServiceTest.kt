package com.flick.core.domain.admin

import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.TransactionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream

class SettlementExportServiceTest :
    FunSpec({
        val boothRepository = mockk<BoothRepository>()
        val orderRepository = mockk<OrderRepository>()
        val orderItemRepository = mockk<OrderItemRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val service = SettlementExportService(boothRepository, orderRepository, orderItemRepository, transactionRepository)

        test("exportSettlement 정상 → 3개 시트, 유효한 XLSX") {
            val owner = User(dauthId = "o1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
            val booth = Booth(name = "타코야끼", user = owner)
            val tx = Transaction(type = TransactionType.CHARGE, amount = 5000, balanceBefore = 0, balanceAfter = 5000, user = owner)

            every { boothRepository.findAll() } returns listOf(booth)
            every { orderRepository.findConfirmedOrderStatsByBooth() } returns
                listOf(arrayOf(booth.id, 10000L, 3L))
            every { orderItemRepository.findBoothProductSalesAggregated() } returns
                listOf(arrayOf("타코야끼", "기본", 3000, 3L, 9000L))
            every { transactionRepository.findAllWithUserOrderByCreatedAtDesc() } returns listOf(tx)

            val bytes = service.exportSettlement()
            bytes.size shouldBeGreaterThan 0

            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            workbook.numberOfSheets shouldBe 3
            workbook.getSheetAt(0).sheetName shouldBe "전체 요약"
            workbook.getSheetAt(1).sheetName shouldBe "부스별 상세"
            workbook.getSheetAt(2).sheetName shouldBe "전체 거래 내역"

            val summarySheet = workbook.getSheetAt(0)
            summarySheet.getRow(1).getCell(0).stringCellValue shouldBe "타코야끼"
            summarySheet.getRow(1).getCell(1).numericCellValue shouldBe 10000.0

            workbook.close()
        }
    })
