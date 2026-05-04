import { auth, db } from "./firebase-init.js";
import { ADMIN_EMAIL } from "./constants.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  onSnapshot,
  writeBatch,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const trashList = document.getElementById("trash-list");
const emptyMsg = document.getElementById("trash-empty");

function renderRow(id, name, ingCount) {
  const row = document.createElement("div");
  row.className = "box";
  row.style.cssText = "display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;padding:1rem;margin-bottom:12px;";
  row.innerHTML = `
    <div>
      <strong>${escapeHtml(name)}</strong>
      <div style="font-size:0.85em;color:#666;">${ingCount} ingredientes</div>
    </div>
    <div style="display:flex;gap:8px;">
      <button type="button" class="button primary trash-restore" data-id="${escapeHtml(id)}"><i class="fa-solid fa-rotate-left"></i> Restaurar</button>
      <button type="button" class="button trash-perma" data-id="${escapeHtml(id)}" style="background:#c62828;"><i class="fa-solid fa-ban"></i> Eliminar</button>
    </div>
  `;
  return row;
}

function escapeHtml(s) {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

async function restoreRecipe(recipeId) {
  const trashRef = doc(db, "trashed_recipes", recipeId);
  const snap = await getDoc(trashRef);
  if (!snap.exists()) return;
  const data = snap.data();
  const batch = writeBatch(db);
  batch.set(doc(db, "public_recipes", recipeId), data);
  batch.delete(trashRef);
  await batch.commit();
}

async function permanentDelete(recipeId) {
  await deleteDoc(doc(db, "trashed_recipes", recipeId));
}

onAuthStateChanged(auth, (user) => {
  if (!user || user.email !== ADMIN_EMAIL) {
    window.location.href = "profile.html";
    return;
  }

  const unsub = onSnapshot(collection(db, "trashed_recipes"), (snap) => {
    trashList.innerHTML = "";
    if (snap.empty) {
      emptyMsg.style.display = "block";
      return;
    }
    emptyMsg.style.display = "none";
    snap.forEach((d) => {
      const data = d.data();
      const ings = data.ingredients || [];
      const row = renderRow(d.id, data.name || "(sin nombre)", Array.isArray(ings) ? ings.length : 0);
      trashList.appendChild(row);
    });

    trashList.querySelectorAll(".trash-restore").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        if (!confirm("¿Restaurar esta receta al recetario público?")) return;
        try {
          await restoreRecipe(id);
        } catch (e) {
          console.error(e);
          alert("No se pudo restaurar.");
        }
      });
    });

    trashList.querySelectorAll(".trash-perma").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        if (!confirm("¿Eliminar permanentemente? Esta acción no se puede deshacer.")) return;
        try {
          await permanentDelete(id);
        } catch (e) {
          console.error(e);
          alert("No se pudo eliminar.");
        }
      });
    });
  });

  window.addEventListener("beforeunload", () => unsub());
});
