package com.flick.core.api.controller.v1.response

import com.flick.core.enums.Bank
import com.flick.storage.db.core.entity.RefundRequest

data class RefundRequestCreatedResponse(
    val bank: Bank,
    val amount: Int,
)

data class RefundRequestResponse(
    val bank: Bank,
    val accountNumber: String,
    val amount: Int,
)

fun RefundRequest.toCreatedResponse() =
    RefundRequestCreatedResponse(
        bank = bank,
        amount = amount,
    )

fun RefundRequest.toResponse() =
    RefundRequestResponse(
        bank = bank,
        accountNumber = accountNumber.maskAccountNumber(),
        amount = amount,
    )

private fun String.maskAccountNumber(): String = if (length > 4) "${"*".repeat(length - 4)}${takeLast(4)}" else this
