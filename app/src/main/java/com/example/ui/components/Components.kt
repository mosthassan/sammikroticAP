package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber

enum class AmountSemanticType {
    INCOME,
    EXPENSE,
    NEUTRAL,
    AUTO
}

@Composable
fun AmountText(
    money: Money,
    modifier: Modifier = Modifier,
    semanticType: AmountSemanticType = AmountSemanticType.AUTO,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.SemiBold,
    includeSymbol: Boolean = true,
    testTag: String = "amount_text"
) {
    val color = when (semanticType) {
        AmountSemanticType.INCOME -> SemanticIncomeGreen
        AmountSemanticType.EXPENSE -> SemanticExpenseRed
        AmountSemanticType.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        AmountSemanticType.AUTO -> {
            if (money.minor > 0L) SemanticIncomeGreen
            else if (money.minor < 0L) SemanticExpenseRed
            else MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(
        text = money.format(includeSymbol = includeSymbol),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace, // Tabular numerals
        textAlign = TextAlign.Start,
        modifier = modifier.testTag(testTag)
    )
}

@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: CurrencyCode,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0.00",
    testTag: String = "money_field"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = currency.symbol,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun StatCard(
    title: String,
    amount: Money,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    semanticType: AmountSemanticType = AmountSemanticType.AUTO,
    testTag: String = "stat_card"
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            AmountText(
                money = amount,
                semanticType = semanticType,
                fontSize = 20,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InvariantBanner(
    isValid: Boolean,
    violationsCount: Int,
    debitTotal: Long,
    creditTotal: Long,
    onRunCheck: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "invariant_banner"
) {
    val containerColor = if (isValid) Color(0xFF064E3B) else Color(0xFF7F1D1D)
    val borderColor = if (isValid) SemanticIncomeGreen else SemanticExpenseRed
    val statusText = if (isValid) "دفتر الأستاذ متزن 100% (IFRS متوافق)" else "تنبيه: خلل في اتزان دفتر الأستاذ ($violationsCount مخالفات)"
    val icon = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onRunCheck() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "حالة الاتزان",
                tint = if (isValid) SemanticIncomeGreen else SemanticExpenseRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "إجمالي المدين: ${Money(debitTotal, CurrencyCode.FUNCTIONAL).format()} | الدائن: ${Money(creditTotal, CurrencyCode.FUNCTIONAL).format()}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier,
    testTag: String = "status_chip"
) {
    val (bg, fg) = when (status) {
        "POSTED", "NORMAL" -> SemanticIncomeGreen.copy(alpha = 0.2f) to SemanticIncomeGreen
        "DRAFT" -> SemanticWarningAmber.copy(alpha = 0.2f) to SemanticWarningAmber
        "VOIDED", "REVERSAL" -> SemanticExpenseRed.copy(alpha = 0.2f) to SemanticExpenseRed
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Text(
            text = status,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onActionClick() }
                    .padding(4.dp)
            )
        }
    }
}
