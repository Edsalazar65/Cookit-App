import { auth, db, storage } from "./firebase-init.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  addDoc,
  arrayUnion,
  collection,
  doc,
  updateDoc,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
import { getDownloadURL, ref, uploadBytes } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js";

const form = document.getElementById("add-recipe-form");
const statusEl = document.getElementById("ar-status");

function linesToList(text) {
  return text
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean);
}

onAuthStateChanged(auth, (user) => {
  if (!user) {
    window.location.href = "login.html";
  }
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const user = auth.currentUser;
  if (!user) {
    window.location.href = "login.html";
    return;
  }

  const name = document.getElementById("ar-name").value.trim();
  const difficulty = document.getElementById("ar-difficulty").value;
  const ingredients = linesToList(document.getElementById("ar-ingredients").value);
  const steps = linesToList(document.getElementById("ar-steps").value);
  const fileInput = document.getElementById("ar-image");

  if (!name || ingredients.length === 0 || steps.length === 0) {
    statusEl.textContent = "Completa nombre, ingredientes y pasos.";
    return;
  }

  statusEl.textContent = "";
  const submitBtn = form.querySelector('button[type="submit"]');
  submitBtn.disabled = true;

  try {
    let payload = {
      name,
      ingredients,
      steps,
      difficulty,
      imageURL: "",
    };

    const docRef = await addDoc(collection(db, "public_recipes"), payload);
    const recipeId = docRef.id;

    if (fileInput.files && fileInput.files[0]) {
      const file = fileInput.files[0];
      const ext = file.name.split(".").pop() || "jpg";
      const storageRef = ref(storage, `recipes/manual_${recipeId}.${ext}`);
      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);
      payload = { ...payload, imageURL: url };
      await updateDoc(doc(db, "public_recipes", recipeId), { imageURL: url });
    }

    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, { myRecipes: arrayUnion(recipeId) });

    sessionStorage.removeItem(`myRecipes_${user.uid}`);
    sessionStorage.removeItem(`myRecipes_cache_v2_${user.uid}`);

    window.location.href = "index.html";
  } catch (err) {
    console.error(err);
    statusEl.textContent = err.message || "No se pudo guardar la receta.";
    submitBtn.disabled = false;
  }
});
