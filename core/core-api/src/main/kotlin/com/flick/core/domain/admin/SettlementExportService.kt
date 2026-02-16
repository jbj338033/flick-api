package com.flick.core.domain.admin

import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.TransactionRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream

@Service
@Transactional(readOnly = true)
class SettlementExportService(
    private val boothRepository: BoothRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val transactionRepository: TransactionRepository,
) {
    fun exportSettlement(): ByteArray {
        val workbook = XSSFWorkbook()

        createSummarySheet(workbook)
        createBoothDetailSheet(workbook)
        createTransactionSheet(workbook)

        return ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }
    }

    private fun createSummarySheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("전체 요약")
        val booths = boothRepository.findAll()
        val statsMap =
            orderRepository.findConfirmedOrderStatsByBooth().associate { row ->
                row[0] to ((row[1] as Long).toInt() to (row[2] as Long).toInt())
            }

        val header = sheet.createRow(0)
        listOf("부스명", "총 매출(원)", "주문 수").forEachIndexed { i, v -> header.createCell(i).setCellValue(v) }

        var totalSales = 0
        var totalOrders = 0

        booths.forEachIndexed { idx, booth ->
            val (sales, orderCount) = statsMap[booth.id] ?: (0 to 0)
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(booth.name)
            row.createCell(1).setCellValue(sales.toDouble())
            row.createCell(2).setCellValue(orderCount.toDouble())
            totalSales += sales
            totalOrders += orderCount
        }

        val totalRow = sheet.createRow(booths.size + 1)
        totalRow.createCell(0).setCellValue("합계")
        totalRow.createCell(1).setCellValue(totalSales.toDouble())
        totalRow.createCell(2).setCellValue(totalOrders.toDouble())
    }

    private fun createBoothDetailSheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("부스별 상세")
        val header = sheet.createRow(0)
        listOf("부스명", "상품명", "단가(원)", "판매수량", "매출(원)").forEachIndexed { i, v -> header.createCell(i).setCellValue(v) }

        val salesData = orderItemRepository.findBoothProductSalesAggregated()
        salesData.forEachIndexed { idx, data ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(data[0] as String)
            row.createCell(1).setCellValue(data[1] as String)
            row.createCell(2).setCellValue((data[2] as Int).toDouble())
            row.createCell(3).setCellValue((data[3] as Long).toDouble())
            row.createCell(4).setCellValue((data[4] as Long).toDouble())
        }
    }

    private fun createTransactionSheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("전체 거래 내역")
        val header = sheet.createRow(0)
        listOf("시간", "유형", "금액(원)", "잔액 전", "잔액 후", "사용자").forEachIndexed { i, v ->
            header.createCell(i).setCellValue(v)
        }

        val transactions = transactionRepository.findAllWithUserOrderByCreatedAtDesc()
        transactions.forEachIndexed { idx, tx ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(tx.createdAt.toString())
            row.createCell(1).setCellValue(tx.type.name)
            row.createCell(2).setCellValue(tx.amount.toDouble())
            row.createCell(3).setCellValue(tx.balanceBefore.toDouble())
            row.createCell(4).setCellValue(tx.balanceAfter.toDouble())
            row.createCell(5).setCellValue(tx.user.name)
        }
    }
}
