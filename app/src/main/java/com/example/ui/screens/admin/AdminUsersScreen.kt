package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Video
import com.example.data.model.Wallet
import com.example.data.model.Withdrawal
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel
) {
    val users by viewModel.filteredUsers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.userSearchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.userStatusFilter.collectAsStateWithLifecycle()
    val selectedUser by viewModel.selectedUser.collectAsStateWithLifecycle()
    val selectedVideos by viewModel.selectedUserVideos.collectAsStateWithLifecycle()
    val selectedTxs by viewModel.selectedUserTransactions.collectAsStateWithLifecycle()
    val selectedWithdrawals by viewModel.selectedUserWithdrawals.collectAsStateWithLifecycle()

    var userToSuspend by remember { mutableStateOf<User?>(null) }
    var suspensionReason by remember { mutableStateOf("") }
    var userToReactivate by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_users_screen")
    ) {
        Text(
            text = "Gestion des Utilisateurs",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "${users.size} utilisateur(s) correspondant",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.userSearchQuery.value = it },
            placeholder = { Text("Rechercher par nom, username, email ou UID...", color = Color(0xFF6B7280)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF9E95B8))
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.userSearchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer", tint = Color(0xFF9E95B8))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_users"),
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

        Spacer(modifier = Modifier.height(12.dp))

        // Filtres par puce (Chips)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filterOptions = listOf(
                "ALL" to "Tous",
                "ACTIVE" to "Actifs",
                "SUSPENDED" to "Suspendus",
                "ADMIN" to "Administrateurs"
            )
            items(filterOptions) { (key, label) ->
                val selected = currentFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.userStatusFilter.value = key },
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

        Spacer(modifier = Modifier.height(16.dp))

        // Liste des utilisateurs
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun utilisateur trouvé",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E95B8))
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users, key = { it.uid }) { user ->
                    UserAdminCard(
                        user = user,
                        onClick = { viewModel.selectUser(user) },
                        onSuspend = { userToSuspend = user },
                        onReactivate = { userToReactivate = user }
                    )
                }
            }
        }
    }

    // Modal de détails utilisateur complet
    selectedUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedUser() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0F0926)
        ) {
            UserDetailsSheet(
                user = user,
                videos = selectedVideos,
                transactions = selectedTxs,
                withdrawals = selectedWithdrawals,
                onClose = { viewModel.clearSelectedUser() },
                onSuspend = { userToSuspend = user },
                onReactivate = { userToReactivate = user }
            )
        }
    }

    // Dialogue de confirmation de suspension avec motif obligatoire
    userToSuspend?.let { user ->
        AlertDialog(
            onDismissRequest = { userToSuspend = null; suspensionReason = "" },
            title = { Text("Suspendre l'utilisateur", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Voulez-vous restreindre le compte de ${user.fullName} (@${user.username}) ?",
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suspensionReason,
                        onValueChange = { suspensionReason = it },
                        label = { Text("Motif de suspension (obligatoire)") },
                        placeholder = { Text("Ex: Activité suspecte, signalements multiples...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (suspensionReason.isNotBlank()) {
                            viewModel.suspendUser(user.uid, suspensionReason)
                            userToSuspend = null
                            suspensionReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    enabled = suspensionReason.isNotBlank()
                ) {
                    Text("Suspendre", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToSuspend = null; suspensionReason = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }

    // Dialogue de confirmation de réactivation
    userToReactivate?.let { user ->
        AlertDialog(
            onDismissRequest = { userToReactivate = null },
            title = { Text("Réactiver le compte", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Confirmez-vous la réactivation du compte de ${user.fullName} (@${user.username}) ?",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reactivateUser(user.uid)
                        userToReactivate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald)
                ) {
                    Text("Réactiver", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToReactivate = null }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }
}

@Composable
fun UserAdminCard(
    user: User,
    onClick: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("user_item_${user.uid}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo de profil
                if (!user.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = user.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(VideoCashPurplePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.fullName.ifBlank { "Utilisateur" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        if (user.role == "admin") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3F1322),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = Color(0xFFFDA4AF),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "@${user.username} • ${user.email}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                    )
                }

                // Badge de statut
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (user.isSuspended) Color(0xFF3B1219) else Color(0xFF0F3123),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (user.isSuspended) Color(0xFFF43F5E) else VideoCashEmerald
                    )
                ) {
                    Text(
                        text = if (user.isSuspended) "Suspendu" else "Actif",
                        color = if (user.isSuspended) Color(0xFFFDA4AF) else VideoCashEmerald,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ligne de détails rapides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${user.videosCount} vidéos",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                    )
                    Text(
                        text = "${user.followersCount} abonnés",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                    )
                }

                // Boutons d'action rapide
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.isSuspended) {
                        OutlinedButton(
                            onClick = onReactivate,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = VideoCashEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Réactiver", color = VideoCashEmerald, fontSize = 12.sp)
                        }
                    } else if (user.role != "admin") {
                        OutlinedButton(
                            onClick = onSuspend,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suspendre", color = Color(0xFFF43F5E), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserDetailsSheet(
    user: User,
    videos: List<Video>,
    transactions: List<Transaction>,
    withdrawals: List<Withdrawal>,
    onClose: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profil", "Vidéos (${videos.size})", "Transactions (${transactions.size})", "Retraits (${withdrawals.size})")
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Détails de l'utilisateur",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Onglets
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF140D2B),
            contentColor = VideoCashPurplePrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // Fiche profil
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow("Nom complet", user.fullName)
                    DetailRow("Nom d'utilisateur", "@${user.username}")
                    DetailRow("Email", user.email)
                    DetailRow("Téléphone", user.phone.ifBlank { "Non renseigné" })
                    DetailRow("UID", user.uid)
                    DetailRow("Rôle", user.role)
                    DetailRow("Statut", if (user.isSuspended) "Suspendu" else "Actif")
                    DetailRow("Inscrit le", dateFormat.format(Date(user.createdAt)))

                    Spacer(modifier = Modifier.height(16.dp))

                    if (user.isSuspended) {
                        Button(
                            onClick = {
                                onReactivate()
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Réactiver le compte utilisateur")
                        }
                    } else if (user.role != "admin") {
                        Button(
                            onClick = {
                                onSuspend()
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Suspendre l'utilisateur")
                        }
                    }
                }
            }
            1 -> {
                // Vidéos
                if (videos.isEmpty()) {
                    Text("Aucune vidéo publiée par cet utilisateur.", color = Color(0xFF9E95B8))
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(videos) { v ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, tint = VideoCashPurplePrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(v.description.ifBlank { "Sans titre" }, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text("${v.viewsCount} vues • ${v.likesCount} likes • ${v.status}", color = Color(0xFF9E95B8), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Transactions
                if (transactions.isEmpty()) {
                    Text("Aucune transaction enregistrée.", color = Color(0xFF9E95B8))
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(transactions) { tx ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.description.ifBlank { tx.type }, color = Color.White, fontSize = 12.sp)
                                        Text(dateFormat.format(Date(tx.createdAt)), color = Color(0xFF9E95B8), fontSize = 10.sp)
                                    }
                                    Text(
                                        text = "${if (tx.isPositive) "+" else "-"}${Wallet.formatCurrency(kotlin.math.abs(tx.amount))} FCFA",
                                        color = if (tx.isPositive) VideoCashEmerald else Color(0xFFFDA4AF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Retraits
                if (withdrawals.isEmpty()) {
                    Text("Aucune demande de retrait.", color = Color(0xFF9E95B8))
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(withdrawals) { w ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${w.method} - ${w.account}", color = Color.White, fontSize = 12.sp)
                                        Text("${w.status.uppercase()} • ${dateFormat.format(Date(w.createdAt))}", color = Color(0xFF9E95B8), fontSize = 10.sp)
                                    }
                                    Text("${w.formattedAmount} FCFA", color = VideoCashGoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF9E95B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
