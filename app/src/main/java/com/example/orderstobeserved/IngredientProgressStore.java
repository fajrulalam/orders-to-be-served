package com.example.orderstobeserved;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ingredient preparation progress, shared by every kitchen tablet.
 *
 * Stored at {canteensCollection}/canteen375/KdsIngredientProgress/{statusDocId} as
 * {progress: {progressKey: preparedPortions}, updatedAt}. It is KDS-only data: it never touches the
 * Status document or a menu line's preparedQuantity, so the POS contract is unchanged.
 */
public class IngredientProgressStore {

    private static final String TAG = "IngredientProgress";
    public static final String COLLECTION = "KdsIngredientProgress";
    private static final String FIELD_PROGRESS = "progress";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    // Orders normally leave through a swipe, which deletes their progress. Anything left behind
    // (e.g. a Status doc removed elsewhere) is pruned once it has been idle this long.
    private static final long PRUNE_AFTER_MS = 10 * 60 * 1000L;

    public interface Listener {
        void onProgressChanged();

        void onProgressError(String message);
    }

    private final FirebaseFirestore fs;
    private final String canteensCollection;
    private final Map<String, Map<String, Integer>> progressByOrder = new HashMap<>();
    private final Map<String, Long> updatedAtByOrder = new HashMap<>();
    private ListenerRegistration registration;

    public IngredientProgressStore(FirebaseFirestore fs, String canteensCollection) {
        this.fs = fs;
        this.canteensCollection = canteensCollection;
    }

    public static CollectionReference collection(FirebaseFirestore fs, String canteensCollection) {
        return fs.collection(canteensCollection).document("canteen375").collection(COLLECTION);
    }

    /** Document ID used for an order's progress: its Status document ID. */
    public static String orderKey(OrderBlock order) {
        String docId = order.getFirestoreDocumentId();
        return docId != null && !docId.isEmpty() ? docId : String.valueOf(order.getCustomerNumber());
    }

    /**
     * Stable key of one ingredient of one order line. It includes orderedAt, so open-bill rounds are
     * tracked separately and a reused Status doc ID (after a queue-number reset) never inherits old
     * progress. Firestore map keys are kept to safe characters, with a hash for uniqueness.
     */
    public static String progressKey(NewOrderItem item, String ingredientKey) {
        List<String> optionIds = new ArrayList<>();
        if (item.getSelectedOptions() != null) {
            for (SelectedOption option : item.getSelectedOptions()) {
                optionIds.add(option.getOptionId() == null ? "" : option.getOptionId());
            }
        }
        Collections.sort(optionIds);
        String raw = item.getOrderType() + "\u0001" + item.getNamaPesanan() + "\u0001" + item.getOrderedAt()
                + "\u0001" + optionIds + "\u0001" + item.getCustomerNote().trim() + "\u0001" + ingredientKey;

        String readable = (item.getOrderType() + "_" + item.getNamaPesanan() + "_" + ingredientKey)
                .replaceAll("[^A-Za-z0-9-]+", "_");
        if (readable.length() > 60) readable = readable.substring(0, 60);
        return readable + "_" + Integer.toHexString(raw.hashCode());
    }

    // ── Listening ───────────────────────────────────────────────────────────

    public void start(Listener listener) {
        if (registration != null) return;
        registration = collection(fs, canteensCollection).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Progress listener failed", error);
                registration = null;
                listener.onProgressError(errorMessage("memuat", error));
                return;
            }
            if (value == null) return;
            progressByOrder.clear();
            updatedAtByOrder.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                progressByOrder.put(doc.getId(), toProgressMap(doc.get(FIELD_PROGRESS)));
                Object updatedAt = doc.get(FIELD_UPDATED_AT);
                if (updatedAt instanceof Timestamp) {
                    updatedAtByOrder.put(doc.getId(), ((Timestamp) updatedAt).toDate().getTime());
                }
            }
            listener.onProgressChanged();
        });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public boolean isListening() {
        return registration != null;
    }

    private static Map<String, Integer> toProgressMap(Object raw) {
        Map<String, Integer> progress = new HashMap<>();
        if (!(raw instanceof Map)) return progress;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.getValue() instanceof Number) {
                progress.put(String.valueOf(entry.getKey()), ((Number) entry.getValue()).intValue());
            }
        }
        return progress;
    }

    // ── Reads and writes ────────────────────────────────────────────────────

    public int getDone(String orderKey, String progressKey) {
        Map<String, Integer> progress = progressByOrder.get(orderKey);
        if (progress == null) return 0;
        Integer done = progress.get(progressKey);
        return done == null ? 0 : done;
    }

    /**
     * Sets the prepared portions of one ingredient. An absolute value (not an increment) keeps
     * working offline and matches what the tapping cook saw; if two tablets tap the same row at the
     * same moment, the last write wins.
     */
    public void setDone(String orderKey, String progressKey, int portions, Listener listener) {
        Map<String, Integer> local = progressByOrder.get(orderKey);
        if (local == null) {
            local = new HashMap<>();
            progressByOrder.put(orderKey, local);
        }
        local.put(progressKey, portions);

        Map<String, Object> progress = new HashMap<>();
        progress.put(progressKey, portions);
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_PROGRESS, progress);
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        collection(fs, canteensCollection).document(orderKey)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save ingredient progress", e);
                    listener.onProgressError(errorMessage("menyimpan", e));
                });
    }

    /** Removes an order's progress. Called when the order is served, whatever mode the tablet is in. */
    public static void deleteForOrder(FirebaseFirestore fs, String canteensCollection, String orderKey) {
        collection(fs, canteensCollection).document(orderKey).delete()
                .addOnFailureListener(e -> Log.w(TAG, "Failed to delete ingredient progress " + orderKey, e));
    }

    /** Deletes progress of orders no longer in Status that have been idle for a while. */
    public void pruneExcept(Set<String> activeOrderKeys) {
        long cutoff = System.currentTimeMillis() - PRUNE_AFTER_MS;
        for (Map.Entry<String, Long> entry : new HashMap<>(updatedAtByOrder).entrySet()) {
            if (!activeOrderKeys.contains(entry.getKey()) && entry.getValue() < cutoff) {
                deleteForOrder(fs, canteensCollection, entry.getKey());
            }
        }
    }

    private static String errorMessage(String action, Exception e) {
        if (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Izin ditolak saat " + action + " progres bahan. Aturan KdsIngredientProgress perlu di-deploy dari proyek 375POS.";
        }
        return "Gagal " + action + " progres bahan: " + e.getMessage();
    }
}
