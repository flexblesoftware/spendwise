package com.shahid.spendwise.domain.usecase

import com.shahid.spendwise.domain.model.SplitType
import kotlin.math.roundToLong

data class ParticipantSplitResult(
    val personId: Long,
    val owedAmountMinor: Long
)

data class DebtTransfer(
    val fromPersonId: Long,
    val toPersonId: Long,
    val amountMinor: Long
)

object SplitCalculator {

    fun splitEqually(
        totalAmountMinor: Long,
        participantIds: List<Long>
    ): List<ParticipantSplitResult> {
        if (participantIds.isEmpty()) return emptyList()
        val count = participantIds.size
        val baseShare = totalAmountMinor / count
        val remainder = totalAmountMinor % count

        return participantIds.mapIndexed { index, personId ->
            val extra = if (index < remainder) 1L else 0L
            ParticipantSplitResult(personId, baseShare + extra)
        }
    }

    fun splitByPercentage(
        totalAmountMinor: Long,
        percentages: Map<Long, Double>
    ): List<ParticipantSplitResult> {
        if (percentages.isEmpty()) return emptyList()
        var currentSum = 0L
        val results = mutableListOf<ParticipantSplitResult>()

        percentages.entries.forEachIndexed { index, entry ->
            val share = ((totalAmountMinor * entry.value) / 100.0).roundToLong()
            currentSum += share
            results.add(ParticipantSplitResult(entry.key, share))
        }

        val diff = totalAmountMinor - currentSum
        if (diff != 0L && results.isNotEmpty()) {
            val first = results[0]
            results[0] = first.copy(owedAmountMinor = first.owedAmountMinor + diff)
        }
        return results
    }

    fun simplifyDebts(netBalances: Map<Long, Long>): List<DebtTransfer> {
        val debtors = mutableListOf<Pair<Long, Long>>()
        val creditors = mutableListOf<Pair<Long, Long>>()

        for ((personId, balance) in netBalances) {
            if (balance < 0) {
                debtors.add(personId to -balance)
            } else if (balance > 0) {
                creditors.add(personId to balance)
            }
        }

        val transfers = mutableListOf<DebtTransfer>()
        var dIndex = 0
        var cIndex = 0

        val mutDebtors = debtors.map { it.second }.toMutableList()
        val mutCreditors = creditors.map { it.second }.toMutableList()

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val debtorAmount = mutDebtors[dIndex]
            val creditorAmount = mutCreditors[cIndex]

            val settleAmount = minOf(debtorAmount, creditorAmount)
            if (settleAmount > 0) {
                transfers.add(
                    DebtTransfer(
                        fromPersonId = debtors[dIndex].first,
                        toPersonId = creditors[cIndex].first,
                        amountMinor = settleAmount
                    )
                )
            }

            mutDebtors[dIndex] -= settleAmount
            mutCreditors[cIndex] -= settleAmount

            if (mutDebtors[dIndex] == 0L) dIndex++
            if (mutCreditors[cIndex] == 0L) cIndex++
        }

        return transfers
    }
}
