package com.example.orderstobeserved;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the shared menu groups in sync with Firestore and runs the group picker and editor dialogs.
 *
 * Groups and the menu list are kitchen configuration, not order data: both always live under the
 * production Canteens/canteen375 document and are never testing-prefixed.
 */
public class MenuGroupManager {

    private static final String TAG = "MenuGroupManager";
    private static final String CANTEEN_DOC_ID = "canteen375";
    private static final String GROUPS_COLLECTION = "KdsMenuGroups";
    private static final String MENU_COLLECTION = "MenuCollection";

    public interface Callback {
        /** The user picked a group in the picker. */
        void onGroupSelected(MenuGroup group);

        /** Groups changed in Firestore (on this or another tablet). */
        void onGroupsChanged();
    }

    /** One MenuCollection document, as shown in the editor. */
    private static class MenuEntry {
        final String id;
        final String name;
        final boolean isMakanan;

        MenuEntry(String id, String name, boolean isMakanan) {
            this.id = id;
            this.name = name;
            this.isMakanan = isMakanan;
        }
    }

    private final Activity activity;
    private final FirebaseFirestore fs;
    private final Callback callback;

    private final List<MenuGroup> groups = new ArrayList<>();
    private ListenerRegistration registration;
    private boolean loaded;
    private String loadError;

    private Dialog listDialog;
    private Dialog editorDialog;
    private Dialog deleteDialog;
    private String listActiveGroupId;

    public MenuGroupManager(Activity activity, FirebaseFirestore fs, Callback callback) {
        this.activity = activity;
        this.fs = fs;
        this.callback = callback;
    }

    private DocumentReference canteen() {
        return fs.collection("Canteens").document(CANTEEN_DOC_ID);
    }

    private CollectionReference groupsCollection() {
        return canteen().collection(GROUPS_COLLECTION);
    }

    public void start() {
        if (registration != null) return;
        registration = groupsCollection().addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to listen to menu groups", error);
                loadError = error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED
                        ? "Izin Firestore ditolak untuk grup menu. Aturan KdsMenuGroups perlu di-deploy dari proyek 375POS."
                        : "Gagal memuat grup menu: " + error.getMessage();
                // A listener that errored is dead; showGroupList() restarts it.
                registration = null;
                renderList();
                return;
            }
            if (value == null) return;
            loaded = true;
            loadError = null;
            groups.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                groups.add(MenuGroup.fromSnapshot(doc));
            }
            Collections.sort(groups, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            renderList();
            callback.onGroupsChanged();
        });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        dismiss(deleteDialog);
        dismiss(editorDialog);
        dismiss(listDialog);
    }

    @Nullable
    public MenuGroup findById(String id) {
        if (id == null) return null;
        for (MenuGroup group : groups) {
            if (group.getId().equals(id)) return group;
        }
        return null;
    }

    // ── Group picker ────────────────────────────────────────────────────────

    public void showGroupList(@Nullable String activeGroupId) {
        if (activity.isFinishing()) return;
        listActiveGroupId = activeGroupId;
        if (registration == null) start();

        Dialog dialog = createDialog(R.layout.dialog_menu_groups);
        listDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (listDialog == dialog) listDialog = null;
        });
        dialog.findViewById(R.id.btnCloseGroups).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnNewGroup).setOnClickListener(v -> showEditor(null));

        renderList();
        sizeDialog(dialog, false);
        dialog.show();
    }

    private void renderList() {
        Dialog dialog = listDialog;
        if (dialog == null) return;

        TextView status = dialog.findViewById(R.id.groupListStatus);
        String message = null;
        if (loadError != null) {
            message = loadError;
        } else if (!loaded) {
            message = "Memuat grup…";
        } else if (groups.isEmpty()) {
            message = "Belum ada grup. Buat grup pertama, misalnya \"Telur\" atau \"Gorengan\".";
        }
        status.setText(message);
        status.setVisibility(message == null ? View.GONE : View.VISIBLE);

        LinearLayout container = dialog.findViewById(R.id.groupListContainer);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (MenuGroup group : groups) {
            View row = inflater.inflate(R.layout.item_menu_group_row, container, false);
            ((TextView) row.findViewById(R.id.groupName)).setText(group.getName());
            ((TextView) row.findViewById(R.id.groupSummary)).setText(summarize(group));
            boolean isActive = group.getId().equals(listActiveGroupId);
            row.setBackgroundResource(isActive
                    ? R.drawable.menu_group_row_active_bg
                    : R.drawable.menu_group_row_bg);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onGroupSelected(group);
            });
            row.findViewById(R.id.btnEditGroup).setOnClickListener(v -> showEditor(group));
            container.addView(row);
        }
    }

    private static String summarize(MenuGroup group) {
        List<String> names = group.getMenuNames();
        int count = Math.max(names.size(), group.getMenuItemIds().size());
        StringBuilder summary = new StringBuilder().append(count).append(" menu");
        if (!names.isEmpty()) {
            summary.append(" · ");
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) summary.append(", ");
                summary.append(names.get(i));
            }
        }
        return summary.toString();
    }

    // ── Group editor ────────────────────────────────────────────────────────

    private void showEditor(@Nullable MenuGroup existing) {
        if (activity.isFinishing()) return;

        Dialog dialog = createDialog(R.layout.dialog_menu_group_editor);
        editorDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (editorDialog == dialog) editorDialog = null;
        });

        TextView title = dialog.findViewById(R.id.editorTitle);
        EditText nameInput = dialog.findViewById(R.id.groupNameInput);
        EditText searchInput = dialog.findViewById(R.id.menuSearchInput);
        TextView selectedCount = dialog.findViewById(R.id.selectedCountText);
        TextView status = dialog.findViewById(R.id.editorStatus);
        LinearLayout container = dialog.findViewById(R.id.menuCheckboxContainer);
        TextView deleteBtn = dialog.findViewById(R.id.btnDeleteGroup);
        TextView cancelBtn = dialog.findViewById(R.id.btnCancelEditor);
        TextView saveBtn = dialog.findViewById(R.id.btnSaveGroup);

        title.setText(existing == null ? "Grup baru" : "Ubah grup");
        if (existing != null) {
            nameInput.setText(existing.getName());
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(v -> confirmDelete(existing));
        }
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        List<MenuEntry> menus = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        setSaveState(saveBtn, false, "Simpan");

        canteen().collection(MENU_COLLECTION).get()
                .addOnSuccessListener(snapshot -> {
                    if (!dialog.isShowing()) return;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        menus.add(toMenuEntry(doc));
                    }
                    Collections.sort(menus, (a, b) -> {
                        if (a.isMakanan != b.isMakanan) return a.isMakanan ? -1 : 1;
                        return a.name.compareToIgnoreCase(b.name);
                    });
                    if (existing != null) {
                        for (MenuEntry menu : menus) {
                            if (existing.containsMenu(menu.id, menu.name)) selectedIds.add(menu.id);
                        }
                    }
                    buildMenuRows(container, menus, selectedIds, selectedCount);
                    applySearch(container, searchInput.getText().toString());
                    if (menus.isEmpty()) {
                        status.setText("Daftar menu kosong.");
                    } else {
                        status.setVisibility(View.GONE);
                        setSaveState(saveBtn, true, "Simpan");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load MenuCollection", e);
                    status.setText("Gagal memuat daftar menu: " + e.getMessage());
                });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                applySearch(container, s.toString());
            }
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Nama grup wajib diisi");
                return;
            }
            if (isNameTaken(name, existing)) {
                nameInput.setError("Nama grup sudah dipakai");
                return;
            }
            if (selectedIds.isEmpty()) {
                toast("Pilih minimal satu menu");
                return;
            }

            List<String> menuItemIds = new ArrayList<>();
            List<String> menuNames = new ArrayList<>();
            for (MenuEntry menu : menus) {
                if (selectedIds.contains(menu.id)) {
                    menuItemIds.add(menu.id);
                    menuNames.add(menu.name);
                }
            }

            boolean isNew = existing == null;
            Map<String, Object> data = MenuGroup.toFirestore(name, menuItemIds, menuNames, isNew);
            DocumentReference ref = isNew
                    ? groupsCollection().document()
                    : groupsCollection().document(existing.getId());
            // Merge on edit so createdAt survives.
            Task<Void> write = isNew ? ref.set(data) : ref.set(data, SetOptions.merge());

            setSaveState(saveBtn, false, "Menyimpan…");
            write.addOnSuccessListener(unused -> {
                        toast("Grup \"" + name + "\" disimpan");
                        dismiss(dialog);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save menu group", e);
                        setSaveState(saveBtn, true, "Simpan");
                        toast(writeErrorMessage("menyimpan", e));
                    });
        });

        sizeDialog(dialog, true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        dialog.show();
    }

    private static MenuEntry toMenuEntry(DocumentSnapshot doc) {
        // Newer menus have auto-generated document IDs; the display name is the namaMenu field.
        Object rawName = doc.get("namaMenu");
        String name = rawName == null ? "" : String.valueOf(rawName).trim();
        if (name.isEmpty()) name = doc.getId();
        boolean isMakanan = !Boolean.FALSE.equals(doc.get("isMakanan"));
        return new MenuEntry(doc.getId(), name, isMakanan);
    }

    private void buildMenuRows(LinearLayout container, List<MenuEntry> menus,
                               Set<String> selectedIds, TextView selectedCount) {
        container.removeAllViews();
        Boolean currentSection = null;
        for (MenuEntry menu : menus) {
            if (currentSection == null || currentSection != menu.isMakanan) {
                currentSection = menu.isMakanan;
                TextView header = new TextView(activity);
                header.setText(menu.isMakanan ? "MAKANAN" : "MINUMAN");
                header.setTextSize(12f);
                header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                header.setTextColor(Color.parseColor("#9CA3AF"));
                header.setPadding(0, dpToPx(12), 0, dpToPx(4));
                container.addView(header);
            }

            CheckBox checkBox = new CheckBox(activity);
            checkBox.setText(menu.name);
            checkBox.setTextSize(17f);
            checkBox.setTextColor(Color.parseColor("#1F2937"));
            checkBox.setPadding(dpToPx(4), dpToPx(10), 0, dpToPx(10));
            checkBox.setChecked(selectedIds.contains(menu.id));
            checkBox.setTag(menu);
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    selectedIds.add(menu.id);
                } else {
                    selectedIds.remove(menu.id);
                }
                selectedCount.setText(selectedIds.size() + " menu dipilih");
            });
            container.addView(checkBox);
        }
        selectedCount.setText(selectedIds.size() + " menu dipilih");
    }

    private static void applySearch(LinearLayout container, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getTag() instanceof MenuEntry) {
                String name = ((MenuEntry) child.getTag()).name.toLowerCase(Locale.ROOT);
                child.setVisibility(q.isEmpty() || name.contains(q) ? View.VISIBLE : View.GONE);
            } else {
                // Section headers only make sense for the unfiltered list.
                child.setVisibility(q.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private boolean isNameTaken(String name, @Nullable MenuGroup existing) {
        for (MenuGroup group : groups) {
            if (existing != null && group.getId().equals(existing.getId())) continue;
            if (group.getName().trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void confirmDelete(MenuGroup group) {
        Dialog dialog = createDialog(R.layout.dialog_confirm_serve);
        deleteDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (deleteDialog == dialog) deleteDialog = null;
        });
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Hapus grup?");
        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText(
                "Grup \"" + group.getName() + "\" akan dihapus dari semua tablet dapur.");
        TextView cancelBtn = dialog.findViewById(R.id.btnCancel);
        TextView confirmBtn = dialog.findViewById(R.id.btnConfirm);
        cancelBtn.setText("Batal");
        confirmBtn.setText("Ya, hapus");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            groupsCollection().document(group.getId()).delete()
                    .addOnSuccessListener(unused -> {
                        toast("Grup \"" + group.getName() + "\" dihapus");
                        dismiss(editorDialog);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete menu group", e);
                        toast(writeErrorMessage("menghapus", e));
                    });
        });
        dialog.show();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Dialog createDialog(int layoutRes) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layoutRes);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void sizeDialog(Dialog dialog, boolean tall) {
        Window window = dialog.getWindow();
        if (window == null) return;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int preferred = Math.max((int) (metrics.widthPixels * 0.42f), dpToPx(440));
        int width = Math.min(preferred, (int) (metrics.widthPixels * 0.92f));
        int height = tall ? (int) (metrics.heightPixels * 0.88f) : WindowManager.LayoutParams.WRAP_CONTENT;
        window.setLayout(width, height);
    }

    private static void setSaveState(TextView saveBtn, boolean enabled, String label) {
        saveBtn.setEnabled(enabled);
        saveBtn.setAlpha(enabled ? 1f : 0.5f);
        saveBtn.setText(label);
    }

    private static String writeErrorMessage(String action, Exception e) {
        if (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Izin ditolak saat " + action + " grup. Aturan KdsMenuGroups belum di-deploy dari proyek 375POS.";
        }
        return "Gagal " + action + " grup: " + e.getMessage();
    }

    private static void dismiss(@Nullable Dialog dialog) {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private void toast(String message) {
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
