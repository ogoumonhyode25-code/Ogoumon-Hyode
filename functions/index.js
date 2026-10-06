const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Devise standard
const DEFAULT_CURRENCY = "XOF";

// Codes d'erreurs contrôlées
const ERRORS = {
  AUTH_REQUIRED: "AUTH_REQUIRED",
  PERMISSION_DENIED: "PERMISSION_DENIED",
  INVALID_AMOUNT: "INVALID_AMOUNT",
  INSUFFICIENT_BALANCE: "INSUFFICIENT_BALANCE",
  WITHDRAWAL_EXISTS: "WITHDRAWAL_EXISTS",
  ACCOUNT_SUSPENDED: "ACCOUNT_SUSPENDED",
  VIDEO_NOT_FOUND: "VIDEO_NOT_FOUND",
  REWARD_NOT_ALLOWED: "REWARD_NOT_ALLOWED",
  RATE_LIMITED: "RATE_LIMITED"
};

/**
 * Fonction utilitaire interne : Envoi de notification Firestore + Push FCM
 */
async function sendNotificationInternal({
  userId,
  title,
  body,
  type,
  referenceId = ""
}) {
  const notifRef = db.collection("notifications").doc();
  const notifData = {
    id: notifRef.id,
    userId: userId,
    title: title,
    body: body,
    type: type,
    read: false,
    createdAt: Date.now(),
    referenceId: referenceId
  };

  // 1. Enregistrement dans Firestore
  await notifRef.set(notifData);

  // 2. Envoi Push via Firebase Cloud Messaging aux appareils de l'utilisateur
  try {
    const devicesSnap = await db.collection("userDevices")
      .where("userId", "==", userId)
      .where("active", "==", true)
      .get();

    const tokens = [];
    devicesSnap.forEach(doc => {
      const data = doc.data();
      if (data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length > 0) {
      const multicastMessage = {
        tokens: tokens,
        notification: {
          title: title,
          body: body
        },
        data: {
          notificationId: notifRef.id,
          type: type,
          referenceId: referenceId,
          click_action: "FLUTTER_NOTIFICATION_CLICK"
        },
        android: {
          priority: "high",
          notification: {
            channelId: "videocash_notifications",
            sound: "default"
          }
        }
      };

      const response = await admin.messaging().sendEachForMulticast(multicastMessage);
      console.log(`Push FCM envoyé à ${response.successCount}/${tokens.length} appareils pour ${userId}`);
    }
  } catch (err) {
    console.warn(`Erreur lors de l'envoi push FCM: ${err.message}`);
  }

  return notifRef.id;
}

/**
 * 1. PROCESS QUALIFIED VIEW (Récompenses pour vues de vidéos)
 */
exports.processQualifiedView = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const userId = context.auth.uid;
  const { videoId, watchDuration } = data;

  if (!videoId || typeof watchDuration !== "number") {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres invalides");
  }

  // Vérifier compte actif
  const userDoc = await db.collection("users").document(userId).get();
  if (!userDoc.exists || userDoc.data().status !== "active") {
    throw new functions.https.HttpsError("permission-denied", ERRORS.ACCOUNT_SUSPENDED);
  }

  // Paramètres système
  const settingsDoc = await db.collection("settings").document("rewards").get();
  const settings = settingsDoc.exists ? settingsDoc.data() : {
    viewReward: 10,
    dailyViewLimit: 50,
    minimumWatchSeconds: 15,
    rewardEnabled: true
  };

  if (!settings.rewardEnabled) {
    throw new functions.https.HttpsError("failed-precondition", ERRORS.REWARD_NOT_ALLOWED);
  }

  if (watchDuration < settings.minimumWatchSeconds) {
    throw new functions.https.HttpsError("failed-precondition", ERRORS.REWARD_NOT_ALLOWED);
  }

  // Vérifier que la vidéo existe et est publiée
  const videoDoc = await db.collection("videos").document(videoId).get();
  if (!videoDoc.exists || videoDoc.data().status !== "published") {
    throw new functions.https.HttpsError("not-found", ERRORS.VIDEO_NOT_FOUND);
  }

  // Auto-vue interdite
  if (videoDoc.data().userId === userId) {
    throw new functions.https.HttpsError("permission-denied", ERRORS.REWARD_NOT_ALLOWED);
  }

  // Anti-doublon par jour
  const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const eventId = `${userId}_${videoId}_${today}`;
  const eventRef = db.collection("rewardEvents").document(eventId);
  const walletRef = db.collection("wallets").document(userId);

  return db.runTransaction(async (transaction) => {
    const eventSnap = await transaction.get(eventRef);
    if (eventSnap.exists) {
      throw new functions.https.HttpsError("already-exists", "Vue déjà rémunérée aujourd'hui");
    }

    const walletSnap = await transaction.get(walletRef);
    const walletData = walletSnap.exists ? walletSnap.data() : {
      uid: userId,
      availableBalance: 0,
      pendingBalance: 0,
      totalEarned: 0,
      totalWithdrawn: 0,
      currency: DEFAULT_CURRENCY
    };

    const rewardAmount = settings.viewReward;

    // Enregistrement événement
    transaction.set(eventRef, {
      id: eventId,
      userId: userId,
      videoId: videoId,
      amount: rewardAmount,
      date: today,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Crédit atomique
    transaction.set(walletRef, {
      uid: userId,
      availableBalance: (walletData.availableBalance || 0) + rewardAmount,
      pendingBalance: walletData.pendingBalance || 0,
      totalEarned: (walletData.totalEarned || 0) + rewardAmount,
      totalWithdrawn: walletData.totalWithdrawn || 0,
      currency: DEFAULT_CURRENCY,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Transaction
    const txRef = db.collection("transactions").doc();
    transaction.set(txRef, {
      id: txRef.id,
      userId: userId,
      type: "reward",
      amount: rewardAmount,
      currency: DEFAULT_CURRENCY,
      source: "qualified_view",
      status: "completed",
      description: "Gain de vue qualifiée",
      createdAt: Date.now(),
      referenceId: eventId
    });

    return { rewardAmount, referenceId: txRef.id };
  }).then(async (result) => {
    // Notification de récompense validée
    await sendNotificationInternal({
      userId: userId,
      title: "Gain validé !",
      body: `Félicitations, +${result.rewardAmount} FCFA ont été ajoutés à votre portefeuille.`,
      type: "REWARD_APPROVED",
      referenceId: result.referenceId
    });

    return { success: true, rewardAmount: result.rewardAmount };
  });
});

/**
 * 2. REQUEST WITHDRAWAL (Demande de retrait sécurisée)
 */
exports.requestWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const userId = context.auth.uid;
  const { amount, method, account } = data;

  if (!amount || typeof amount !== "number" || amount <= 0 || !method || !account) {
    throw new functions.https.HttpsError("invalid-argument", ERRORS.INVALID_AMOUNT);
  }

  // Statut du compte
  const userDoc = await db.collection("users").document(userId).get();
  if (!userDoc.exists || userDoc.data().status !== "active") {
    throw new functions.https.HttpsError("permission-denied", ERRORS.ACCOUNT_SUSPENDED);
  }

  // Paramètres
  const settingsDoc = await db.collection("settings").document("rewards").get();
  const settings = settingsDoc.exists ? settingsDoc.data() : {
    minimumWithdrawal: 1000,
    maximumDailyWithdrawal: 100000
  };

  if (amount < settings.minimumWithdrawal) {
    throw new functions.https.HttpsError("failed-precondition", ERRORS.INVALID_AMOUNT);
  }

  if (amount > settings.maximumDailyWithdrawal) {
    throw new functions.https.HttpsError("failed-precondition", ERRORS.INVALID_AMOUNT);
  }

  // Vérifier s'il y a déjà un retrait en cours
  const pendingSnap = await db.collection("withdrawals")
    .where("userId", "==", userId)
    .where("status", "==", "pending")
    .limit(1)
    .get();

  if (!pendingSnap.empty) {
    throw new functions.https.HttpsError("already-exists", ERRORS.WITHDRAWAL_EXISTS);
  }

  const walletRef = db.collection("wallets").document(userId);
  const withdrawalRef = db.collection("withdrawals").doc();

  return db.runTransaction(async (transaction) => {
    const walletSnap = await transaction.get(walletRef);
    if (!walletSnap.exists) {
      throw new functions.https.HttpsError("not-found", ERRORS.INSUFFICIENT_BALANCE);
    }

    const wallet = walletSnap.data();
    const available = wallet.availableBalance || 0;

    if (available < amount) {
      throw new functions.https.HttpsError("failed-precondition", ERRORS.INSUFFICIENT_BALANCE);
    }

    // Déduction atomique
    transaction.update(walletRef, {
      availableBalance: available - amount,
      pendingBalance: (wallet.pendingBalance || 0) + amount,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Retrait
    transaction.set(withdrawalRef, {
      id: withdrawalRef.id,
      userId: userId,
      amount: amount,
      currency: DEFAULT_CURRENCY,
      method: method,
      account: account,
      status: "pending",
      createdAt: Date.now(),
      updatedAt: Date.now(),
      adminNote: "Demande reçue, en attente de vérification",
      providerReference: `WD-${withdrawalRef.id.slice(0, 8).toUpperCase()}`
    });

    // Transaction
    const txRef = db.collection("transactions").doc();
    transaction.set(txRef, {
      id: txRef.id,
      userId: userId,
      type: "withdrawal",
      amount: amount,
      currency: DEFAULT_CURRENCY,
      source: method,
      status: "pending",
      description: `Retrait ${method} (${account})`,
      createdAt: Date.now(),
      referenceId: withdrawalRef.id
    });

    // Audit log
    const auditRef = db.collection("auditLogs").doc();
    transaction.set(auditRef, {
      id: auditRef.id,
      actorId: userId,
      actorRole: "user",
      action: "withdrawal_requested",
      targetId: withdrawalRef.id,
      description: `Demande de retrait de ${amount} FCFA vers ${method}`,
      createdAt: Date.now()
    });

    return { id: withdrawalRef.id, amount, method };
  }).then(async (result) => {
    // Notification : "Votre demande de retrait a bien été reçue."
    await sendNotificationInternal({
      userId: userId,
      title: "Demande de retrait reçue",
      body: "Votre demande de retrait a bien été reçue et est en cours d'examen.",
      type: "WITHDRAWAL_CREATED",
      referenceId: result.id
    });

    return { success: true, withdrawalId: result.id };
  });
});

/**
 * 3. APPROVE WITHDRAWAL (Validation par administrateur)
 */
exports.approveWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  // Vérifier rôle admin
  const userDoc = await db.collection("users").document(context.auth.uid).get();
  if (!userDoc.exists || userDoc.data().role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", ERRORS.PERMISSION_DENIED);
  }

  const { withdrawalId, adminNote } = data;
  const withdrawalRef = db.collection("withdrawals").document(withdrawalId);

  return db.runTransaction(async (transaction) => {
    const snap = await transaction.get(withdrawalRef);
    if (!snap.exists) {
      throw new functions.https.HttpsError("not-found", "Retrait introuvable");
    }

    const withdrawal = snap.data();
    if (withdrawal.status !== "pending" && withdrawal.status !== "reviewing") {
      throw new functions.https.HttpsError("failed-precondition", "Statut incompatible");
    }

    transaction.update(withdrawalRef, {
      status: "approved",
      updatedAt: Date.now(),
      adminNote: adminNote || "Demande validée par l'administration"
    });

    // Audit
    const auditRef = db.collection("auditLogs").doc();
    transaction.set(auditRef, {
      id: auditRef.id,
      actorId: context.auth.uid,
      actorRole: "admin",
      action: "withdrawal_approved",
      targetId: withdrawalId,
      description: `Retrait ${withdrawalId} approuvé.`,
      createdAt: Date.now()
    });

    return { userId: withdrawal.userId, withdrawalId };
  }).then(async (result) => {
    // Notification : « Votre demande de retrait a été approuvée. »
    await sendNotificationInternal({
      userId: result.userId,
      title: "Retrait approuvé",
      body: "Votre demande de retrait a été approuvée.",
      type: "WITHDRAWAL_APPROVED",
      referenceId: result.withdrawalId
    });

    return { success: true };
  });
});

/**
 * 4. REJECT WITHDRAWAL (Refus par administrateur)
 */
exports.rejectWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const userDoc = await db.collection("users").document(context.auth.uid).get();
  if (!userDoc.exists || userDoc.data().role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", ERRORS.PERMISSION_DENIED);
  }

  const { withdrawalId, reason } = data;
  if (!reason) {
    throw new functions.https.HttpsError("invalid-argument", "Un motif de refus est requis");
  }

  const withdrawalRef = db.collection("withdrawals").document(withdrawalId);

  return db.runTransaction(async (transaction) => {
    const snap = await transaction.get(withdrawalRef);
    if (!snap.exists) {
      throw new functions.https.HttpsError("not-found", "Retrait introuvable");
    }

    const withdrawal = snap.data();
    const walletRef = db.collection("wallets").document(withdrawal.userId);
    const walletSnap = await transaction.get(walletRef);
    const wallet = walletSnap.data();

    // Restitution atomique du solde
    transaction.update(walletRef, {
      availableBalance: (wallet.availableBalance || 0) + withdrawal.amount,
      pendingBalance: Math.max(0, (wallet.pendingBalance || 0) - withdrawal.amount),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    transaction.update(withdrawalRef, {
      status: "rejected",
      updatedAt: Date.now(),
      processedAt: Date.now(),
      adminNote: reason
    });

    // Transaction de remboursement
    const txRef = db.collection("transactions").doc();
    transaction.set(txRef, {
      id: txRef.id,
      userId: withdrawal.userId,
      type: "refund",
      amount: withdrawal.amount,
      currency: DEFAULT_CURRENCY,
      source: "withdrawal_rejected",
      status: "completed",
      description: `Remboursement: ${reason}`,
      createdAt: Date.now(),
      referenceId: withdrawalId
    });

    // Audit
    const auditRef = db.collection("auditLogs").doc();
    transaction.set(auditRef, {
      id: auditRef.id,
      actorId: context.auth.uid,
      actorRole: "admin",
      action: "withdrawal_rejected",
      targetId: withdrawalId,
      description: `Retrait ${withdrawalId} rejeté: ${reason}`,
      createdAt: Date.now()
    });

    return { userId: withdrawal.userId, withdrawalId };
  }).then(async (result) => {
    // Notification : « Votre demande de retrait a été refusée. »
    await sendNotificationInternal({
      userId: result.userId,
      title: "Retrait refusé",
      body: "Votre demande de retrait a été refusée.",
      type: "WITHDRAWAL_REJECTED",
      referenceId: result.withdrawalId
    });

    return { success: true };
  });
});

/**
 * 5. COMPLETE WITHDRAWAL (Confirmation réelle du paiement par le fournisseur)
 */
exports.completeWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const userDoc = await db.collection("users").document(context.auth.uid).get();
  if (!userDoc.exists || userDoc.data().role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", ERRORS.PERMISSION_DENIED);
  }

  const { withdrawalId, providerReference } = data;
  const withdrawalRef = db.collection("withdrawals").document(withdrawalId);

  return db.runTransaction(async (transaction) => {
    const snap = await transaction.get(withdrawalRef);
    if (!snap.exists) {
      throw new functions.https.HttpsError("not-found", "Retrait introuvable");
    }

    const withdrawal = snap.data();
    const walletRef = db.collection("wallets").document(withdrawal.userId);
    const walletSnap = await transaction.get(walletRef);
    const wallet = walletSnap.data();

    // Déduction définitive
    transaction.update(walletRef, {
      pendingBalance: Math.max(0, (wallet.pendingBalance || 0) - withdrawal.amount),
      totalWithdrawn: (wallet.totalWithdrawn || 0) + withdrawal.amount,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    transaction.update(withdrawalRef, {
      status: "paid",
      updatedAt: Date.now(),
      processedAt: Date.now(),
      providerReference: providerReference || withdrawal.providerReference,
      adminNote: "Paiement confirmé avec succès par le fournisseur"
    });

    // Audit
    const auditRef = db.collection("auditLogs").doc();
    transaction.set(auditRef, {
      id: auditRef.id,
      actorId: context.auth.uid,
      actorRole: "admin",
      action: "withdrawal_paid",
      targetId: withdrawalId,
      description: `Paiement confirmé avec référence ${providerReference || "N/A"}`,
      createdAt: Date.now()
    });

    return { userId: withdrawal.userId, withdrawalId };
  }).then(async (result) => {
    // Notification : « Votre paiement a été effectué. »
    // IMPORTANT : Ne jamais annoncer qu'un paiement a été effectué avant confirmation réelle.
    await sendNotificationInternal({
      userId: result.userId,
      title: "Paiement effectué",
      body: "Votre paiement a été effectué.",
      type: "WITHDRAWAL_PAID",
      referenceId: result.withdrawalId
    });

    return { success: true };
  });
});

/**
 * 6. CANCEL WITHDRAWAL (Par l'utilisateur si encore pending)
 */
exports.cancelWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const userId = context.auth.uid;
  const { withdrawalId } = data;
  const withdrawalRef = db.collection("withdrawals").document(withdrawalId);
  const walletRef = db.collection("wallets").document(userId);

  return db.runTransaction(async (transaction) => {
    const snap = await transaction.get(withdrawalRef);
    if (!snap.exists) {
      throw new functions.https.HttpsError("not-found", "Demande introuvable");
    }

    const withdrawal = snap.data();
    if (withdrawal.userId !== userId) {
      throw new functions.https.HttpsError("permission-denied", ERRORS.PERMISSION_DENIED);
    }

    if (withdrawal.status !== "pending") {
      throw new functions.https.HttpsError("failed-precondition", "Seule une demande en attente peut être annulée");
    }

    const walletSnap = await transaction.get(walletRef);
    const wallet = walletSnap.data();

    // Restitution atomique
    transaction.update(walletRef, {
      availableBalance: (wallet.availableBalance || 0) + withdrawal.amount,
      pendingBalance: Math.max(0, (wallet.pendingBalance || 0) - withdrawal.amount),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    transaction.update(withdrawalRef, {
      status: "cancelled",
      updatedAt: Date.now(),
      adminNote: "Annulé par l'utilisateur"
    });

    const txRef = db.collection("transactions").doc();
    transaction.set(txRef, {
      id: txRef.id,
      userId: userId,
      type: "refund",
      amount: withdrawal.amount,
      currency: DEFAULT_CURRENCY,
      source: "withdrawal_cancelled",
      status: "completed",
      description: "Restitution après annulation du retrait",
      createdAt: Date.now(),
      referenceId: withdrawalId
    });

    return { success: true };
  });
});

/**
 * 7. SEND NOTIFICATION (Fonction appelable sécurisée)
 */
exports.sendNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }

  const { targetUserId, title, body, type, referenceId } = data;
  if (!targetUserId || !title || !body) {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres manquants");
  }

  const notifId = await sendNotificationInternal({
    userId: targetUserId,
    title: title,
    body: body,
    type: type || "SYSTEM",
    referenceId: referenceId || ""
  });

  return { success: true, notificationId: notifId };
});

/**
 * ====================================================================
 * PARTIE 6 : CLOUD FUNCTIONS D'ADMINISTRATION VIDÉOCASH
 * ====================================================================
 */

/**
 * Vérification des privilèges administrateur (Custom Claims ou Firestore)
 */
async function verifyAdmin(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", ERRORS.AUTH_REQUIRED);
  }
  const token = context.auth.token || {};
  if (token.admin === true || token.role === "admin") {
    return true;
  }
  const userDoc = await db.collection("users").doc(context.auth.uid).get();
  if (userDoc.exists && userDoc.data().role === "admin") {
    return true;
  }
  throw new functions.https.HttpsError("permission-denied", ERRORS.PERMISSION_DENIED);
}

/**
 * Helper d'enregistrement des journaux d'audit
 */
async function recordAuditLog({ actorId, actorRole = "admin", action, targetId, targetName = "", description }) {
  const auditRef = db.collection("auditLogs").doc();
  await auditRef.set({
    id: auditRef.id,
    actorId: actorId,
    actorRole: actorRole,
    action: action,
    targetId: targetId,
    targetName: targetName,
    description: description,
    createdAt: Date.now()
  });
  return auditRef.id;
}

/**
 * 8. SUSPEND USER (Suspension sécurisée d'un utilisateur)
 */
exports.suspendUser = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { userId, reason } = data;
  if (!userId || !reason) {
    throw new functions.https.HttpsError("invalid-argument", "L'identifiant utilisateur et le motif sont requis.");
  }

  const userRef = db.collection("users").doc(userId);
  const userDoc = await userRef.get();
  if (!userDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Utilisateur introuvable");
  }

  await userRef.update({
    status: "suspended",
    suspendedReason: reason,
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "USER_SUSPENDED",
    targetId: userId,
    targetName: userDoc.data().username || userId,
    description: `Suspension de l'utilisateur. Motif: ${reason}`
  });

  await sendNotificationInternal({
    userId: userId,
    title: "Compte restreint",
    body: `Votre compte VidéoCash a été suspendu pour la raison suivante : ${reason}`,
    type: "ADMIN",
    referenceId: userId
  });

  return { success: true };
});

/**
 * 9. REACTIVATE USER (Réactivation d'un utilisateur suspendu)
 */
exports.reactivateUser = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { userId } = data;
  if (!userId) {
    throw new functions.https.HttpsError("invalid-argument", "Identifiant utilisateur manquant.");
  }

  const userRef = db.collection("users").doc(userId);
  const userDoc = await userRef.get();
  if (!userDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Utilisateur introuvable");
  }

  await userRef.update({
    status: "active",
    suspendedReason: admin.firestore.FieldValue.delete(),
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "USER_REACTIVATED",
    targetId: userId,
    targetName: userDoc.data().username || userId,
    description: "Réactivation du compte utilisateur."
  });

  await sendNotificationInternal({
    userId: userId,
    title: "Compte réactivé",
    body: "Votre compte VidéoCash est de nouveau actif. Vous pouvez reprendre vos activités.",
    type: "ADMIN",
    referenceId: userId
  });

  return { success: true };
});

/**
 * 10. MODERATE VIDEO (Masquage ou blocage d'une vidéo signalée/inappropriée)
 */
exports.moderateVideo = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { videoId, status, reason } = data;
  if (!videoId || !status) {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres vidéo invalides.");
  }

  const videoRef = db.collection("videos").doc(videoId);
  const videoDoc = await videoRef.get();
  if (!videoDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Vidéo introuvable.");
  }

  const newStatus = status || "blocked";
  await videoRef.update({
    status: newStatus,
    moderationReason: reason || "Non conforme aux règles de la communauté",
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "VIDEO_MODERATED",
    targetId: videoId,
    targetName: videoDoc.data().description?.slice(0, 30) || videoId,
    description: `Statut modifié vers '${newStatus}'. Motif: ${reason || 'Non spécifié'}`
  });

  const creatorId = videoDoc.data().userId;
  if (creatorId) {
    await sendNotificationInternal({
      userId: creatorId,
      title: "Vidéo modérée",
      body: `Votre vidéo a été masquée par l'équipe de modération : ${reason || "Non-respect des règles"}`,
      type: "ADMIN",
      referenceId: videoId
    });
  }

  return { success: true };
});

/**
 * 11. RESTORE VIDEO (Restauration d'une vidéo vers le statut publiée)
 */
exports.restoreVideo = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { videoId } = data;
  if (!videoId) {
    throw new functions.https.HttpsError("invalid-argument", "Identifiant vidéo manquant.");
  }

  const videoRef = db.collection("videos").doc(videoId);
  const videoDoc = await videoRef.get();
  if (!videoDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Vidéo introuvable.");
  }

  await videoRef.update({
    status: "published",
    moderationReason: admin.firestore.FieldValue.delete(),
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "VIDEO_RESTORED",
    targetId: videoId,
    targetName: videoDoc.data().description?.slice(0, 30) || videoId,
    description: "Vidéo restaurée au statut publié."
  });

  const creatorId = videoDoc.data().userId;
  if (creatorId) {
    await sendNotificationInternal({
      userId: creatorId,
      title: "Vidéo rétablie",
      body: "Votre vidéo a été vérifiée et est de nouveau disponible pour tous les spectateurs.",
      type: "ADMIN",
      referenceId: videoId
    });
  }

  return { success: true };
});

/**
 * 12. DELETE VIDEO (Suppression administrative d'une vidéo)
 */
exports.deleteVideo = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { videoId, reason } = data;
  if (!videoId) {
    throw new functions.https.HttpsError("invalid-argument", "Identifiant vidéo manquant.");
  }

  const videoRef = db.collection("videos").doc(videoId);
  const videoDoc = await videoRef.get();
  if (!videoDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Vidéo introuvable.");
  }

  await videoRef.update({
    status: "deleted",
    deletedReason: reason || "Suppression par l'administration",
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "VIDEO_DELETED",
    targetId: videoId,
    targetName: videoDoc.data().description?.slice(0, 30) || videoId,
    description: `Suppression administrative: ${reason || 'Infraction majeure'}`
  });

  return { success: true };
});

/**
 * 13. RESOLVE REPORT (Traitement d'un signalement)
 */
exports.resolveReport = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { reportId, status, resolution } = data;
  if (!reportId || !status) {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres du signalement incomplets.");
  }

  const reportRef = db.collection("reports").doc(reportId);
  const reportDoc = await reportRef.get();
  if (!reportDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Signalement introuvable.");
  }

  await reportRef.update({
    status: status, // "resolved" ou "rejected"
    resolution: resolution || "Signalement traité par l'administration",
    resolvedBy: context.auth.uid,
    updatedAt: Date.now()
  });

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "REPORT_RESOLVED",
    targetId: reportId,
    targetName: `${reportDoc.data().targetType}:${reportDoc.data().targetId}`,
    description: `Signalement ${status}: ${resolution || 'Aucun détail'}`
  });

  return { success: true };
});

/**
 * 14. CREATE ADMIN ADJUSTMENT (Ajustement financier d'un portefeuille)
 * RÈGLE ABSOLUE : Transaction Firestore atomique côté serveur uniquement.
 */
exports.createAdminAdjustment = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { userId, amount, reason } = data;

  if (!userId || typeof amount !== "number" || amount === 0 || !reason) {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres d'ajustement invalides. Le motif est obligatoire.");
  }

  const walletRef = db.collection("wallets").doc(userId);

  return db.runTransaction(async (transaction) => {
    const walletSnap = await transaction.get(walletRef);
    const wallet = walletSnap.exists ? walletSnap.data() : {
      uid: userId,
      availableBalance: 0,
      pendingBalance: 0,
      totalEarned: 0,
      totalWithdrawn: 0,
      currency: DEFAULT_CURRENCY
    };

    const newBalance = (wallet.availableBalance || 0) + amount;
    if (newBalance < 0) {
      throw new functions.https.HttpsError("failed-precondition", "L'ajustement résulterait en un solde négatif.");
    }

    const newTotalEarned = amount > 0 ? (wallet.totalEarned || 0) + amount : (wallet.totalEarned || 0);

    transaction.set(walletRef, {
      uid: userId,
      availableBalance: newBalance,
      pendingBalance: wallet.pendingBalance || 0,
      totalEarned: newTotalEarned,
      totalWithdrawn: wallet.totalWithdrawn || 0,
      currency: DEFAULT_CURRENCY,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Création de la transaction d'ajustement
    const txRef = db.collection("transactions").doc();
    transaction.set(txRef, {
      id: txRef.id,
      userId: userId,
      type: "adjustment",
      amount: amount,
      currency: DEFAULT_CURRENCY,
      source: "admin_adjustment",
      status: "completed",
      description: `Ajustement administratif : ${reason}`,
      createdAt: Date.now(),
      referenceId: context.auth.uid
    });

    // Journal d'audit
    const auditRef = db.collection("auditLogs").doc();
    transaction.set(auditRef, {
      id: auditRef.id,
      actorId: context.auth.uid,
      actorRole: "admin",
      action: "ADMIN_ADJUSTMENT_CREATED",
      targetId: userId,
      targetName: `Wallet:${userId}`,
      description: `Ajustement de ${amount > 0 ? '+' : ''}${amount} FCFA. Motif: ${reason}`,
      createdAt: Date.now()
    });

    return { txId: txRef.id, amount, newBalance };
  }).then(async (res) => {
    await sendNotificationInternal({
      userId: userId,
      title: amount > 0 ? "Crédit exceptionnel" : "Ajustement de solde",
      body: `Votre solde a été ajusté de ${amount > 0 ? '+' : ''}${amount} FCFA : ${reason}`,
      type: "ADMIN",
      referenceId: res.txId
    });

    return { success: true, newBalance: res.newBalance };
  });
});

/**
 * 15. SEND ADMIN NOTIFICATION (Diffusion ciblée ou globale)
 */
exports.sendAdminNotification = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { title, body, targetUserId, targetAudience } = data;

  if (!title || !body) {
    throw new functions.https.HttpsError("invalid-argument", "Le titre et le message sont requis.");
  }

  let sentCount = 0;

  if (targetAudience === "all") {
    const usersSnap = await db.collection("users").where("status", "==", "active").limit(500).get();
    const promises = [];
    usersSnap.forEach(doc => {
      promises.push(sendNotificationInternal({
        userId: doc.id,
        title: title,
        body: body,
        type: "ADMIN",
        referenceId: "broadcast"
      }));
    });
    await Promise.all(promises);
    sentCount = usersSnap.size;
  } else if (targetUserId) {
    await sendNotificationInternal({
      userId: targetUserId,
      title: title,
      body: body,
      type: "ADMIN",
      referenceId: "direct"
    });
    sentCount = 1;
  } else {
    throw new functions.https.HttpsError("invalid-argument", "Cible indéterminée.");
  }

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "ADMIN_NOTIFICATION_SENT",
    targetId: targetUserId || "all_users",
    targetName: targetAudience === "all" ? "Tous les utilisateurs" : targetUserId,
    description: `Notification envoyée (${sentCount} destinataires) : "${title}"`
  });

  return { success: true, sentCount };
});

/**
 * 16. UPDATE ADMIN SETTINGS (Mise à jour des paramètres système)
 */
exports.updateAdminSettings = functions.https.onCall(async (data, context) => {
  await verifyAdmin(context);
  const { settings } = data;
  if (!settings || typeof settings !== "object") {
    throw new functions.https.HttpsError("invalid-argument", "Paramètres invalides.");
  }

  const batch = db.batch();

  // Mise à jour settings/rewards
  const rewardsRef = db.collection("settings").doc("rewards");
  batch.set(rewardsRef, {
    rewardEnabled: settings.rewardEnabled !== undefined ? settings.rewardEnabled : true,
    viewReward: settings.viewReward || 10,
    dailyViewLimit: settings.dailyViewLimit || 50,
    minimumWatchSeconds: settings.minimumWatchSeconds || 15,
    minimumWithdrawal: settings.minimumWithdrawal || 1000,
    maximumDailyWithdrawal: settings.maximumDailyWithdrawal || 100000,
    updatedAt: Date.now(),
    updatedBy: context.auth.uid
  }, { merge: true });

  // Mise à jour settings/global
  const globalRef = db.collection("settings").doc("global");
  batch.set(globalRef, {
    appName: settings.appName || "VidéoCash",
    maintenanceMode: settings.maintenanceMode || false,
    registrationEnabled: settings.registrationEnabled !== undefined ? settings.registrationEnabled : true,
    publishingEnabled: settings.publishingEnabled !== undefined ? settings.publishingEnabled : true,
    updatedAt: Date.now(),
    updatedBy: context.auth.uid
  }, { merge: true });

  await batch.commit();

  await recordAuditLog({
    actorId: context.auth.uid,
    action: "SETTINGS_CHANGED",
    targetId: "settings",
    targetName: "Paramètres VidéoCash",
    description: "Mise à jour globale des paramètres par l'administrateur."
  });

  return { success: true };
});

