package com.shahid.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import com.shahid.spendwise.ui.theme.ExpenseRed
import com.shahid.spendwise.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountPrefix = if (isExpense) "-" else "+"
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val amountFormatted = Money(transaction.amountMinor, transaction.currency).format()

    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(transaction.timestampMillis))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val catColor = runCatching { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#10B981")) }
                .getOrDefault(MaterialTheme.colorScheme.primary)

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIcon(category?.iconName ?: "Category"),
                    contentDescription = category?.name ?: "Category",
                    tint = catColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (transaction.note.isNotBlank()) transaction.note else (category?.name ?: "Expense"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.isPrivacyLocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy Locked",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!transaction.receiptPhotoPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Has Receipt",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${category?.name ?: "General"} • $dateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$amountPrefix$amountFormatted",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                fontSize = 15.sp
            )
        }
    }
}
