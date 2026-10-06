package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminTransactionsScreen(
    viewModel: AdminViewModel
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val currentTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.transactionQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_transactions_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Audit des Transactions",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${transactions.size} opération(s) enregistrée(s)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E153A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C1D95))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = VideoCashEmerald, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Immuable", color = VideoCashEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recherche par ID ou utilisateur
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.transactionQuery.value = it },
            placeholder = { Text("Rechercher par ID transaction, UID utilisateur...", color = Color(0xFF6B7280)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF9E95B8)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.transactionQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer", tint = Color(0xFF9E95B8))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF140D2B),
                unfocusedContainerColor = Color(0xFF140D2B),
                focusedBorderColor = VideoCashPurplePrimary,
                unfocusedBorderColor = Color(0xFF281F47),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filtre par type
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val typeList = listOf(
                "ALL" to "Toutes",
                Transaction.TYPE_REWARD to "Récompenses",
                Transaction.TYPE_WITHDRAWAL to "Retraits",
                Transaction.TYPE_ADJUSTMENT to "Ajustements",
                Transaction.TYPE_REFUND to "Remboursements"
            )
            items(typeList) { (key, label) ->
                val selected = currentTypeFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.transactionTypeFilter.value = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF140D2B),
                        labelColor = Color(0xFF9E95B8),
                        selectedContainerColor = VideoCashPurplePrimary,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Color(0xFF281F47),
                        selectedBorderColor = VideoCashPurplePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune transaction trouvée.", color = Color(0xFF9E95B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    AdminTransactionCard(tx = tx)
                }
            }
        }
    }
}

@Composable
fun AdminTransactionCard(tx: Transaction) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.FRENCH) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF281F47)
                ) {
                    Text(
                        text = "${tx.type.uppercase()} • #${tx.id.take(8)}",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${if (tx.isPositive) "+" else "-"}${Wallet.formatCurrency(kotlin.math.abs(tx.amount))} FCFA",
                    color = if (tx.isPositive) VideoCashEmerald else Color(0xFFFDA4AF),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tx.description.ifBlank { "Opération sur portefeuille" },
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Utilisateur : ${tx.userId.take(14)}...",
                    color = Color(0xFF9E95B8),
                    fontSize = 11.sp
                )
                Text(
                    text = dateFormat.format(Date(tx.createdAt)),
                    color = Color(0xFF9E95B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
