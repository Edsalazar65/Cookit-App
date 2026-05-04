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
const ingredientInput = document.getElementById("ar-ingredient-input");
const stepInput = document.getElementById("ar-step-input");
const ingredientsListEl = document.getElementById("ar-ingredients-list");
const stepsListEl = document.getElementById("ar-steps-list");
const difficultyHidden = document.getElementById("ar-difficulty");
const photoZone = document.getElementById("ar-photo-zone");
const photoInput = document.getElementById("ar-image");
const photoPlaceholder = document.getElementById("ar-photo-placeholder");
const photoPreview = document.getElementById("ar-photo-preview");

let ingredients = [];
let steps = [];
let previewObjectUrl = null;

function renderIngredients() {
  ingredientsListEl.innerHTML = "";
  ingredients.forEach((text, index) => {
    const li = document.createElement("li");
    li.innerHTML = `
      <i class="fa-solid fa-check" style="color:#388e3c;font-size:0.85rem;margin-top:3px;"></i>
      <span class="ar-item-text"></span>
      <button type="button" class="ar-item-remove" data-kind="ingredient" data-index="${index}" aria-label="Remove"><i class="fa-solid fa-trash-can"></i></button>
    `;
    li.querySelector(".ar-item-text").textContent = text;
    ingredientsListEl.appendChild(li);
  });
}

function renderSteps() {
  stepsListEl.innerHTML = "";
  steps.forEach((text, index) => {
    const li = document.createElement("li");
    li.innerHTML = `
      <span class="ar-item-num">${index + 1}.</span>
      <span class="ar-item-text"></span>
      <button type="button" class="ar-item-remove" data-kind="step" data-index="${index}" aria-label="Remove"><i class="fa-solid fa-trash-can"></i></button>
    `;
    li.querySelector(".ar-item-text").textContent = text;
    stepsListEl.appendChild(li);
  });
}

function addIngredient() {
  const t = ingredientInput.value.trim();
  if (!t) return;
  ingredients.push(t);
  ingredientInput.value = "";
  renderIngredients();
}

function addStep() {
  const t = stepInput.value.trim();
  if (!t) return;
  steps.push(t);
  stepInput.value = "";
  renderSteps();
}

document.getElementById("ar-add-ingredient").addEventListener("click", addIngredient);
ingredientInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    addIngredient();
  }
});

document.getElementById("ar-add-step").addEventListener("click", addStep);
stepInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    addStep();
  }
});

ingredientsListEl.addEventListener("click", (e) => {
  const btn = e.target.closest(".ar-item-remove");
  if (!btn || btn.dataset.kind !== "ingredient") return;
  const i = Number(btn.dataset.index);
  ingredients.splice(i, 1);
  renderIngredients();
});

stepsListEl.addEventListener("click", (e) => {
  const btn = e.target.closest(".ar-item-remove");
  if (!btn || btn.dataset.kind !== "step") return;
  const i = Number(btn.dataset.index);
  steps.splice(i, 1);
  renderSteps();
});

document.querySelectorAll(".ar-diff-chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    document.querySelectorAll(".ar-diff-chip").forEach((c) => c.classList.remove("selected"));
    chip.classList.add("selected");
    difficultyHidden.value = chip.dataset.value;
  });
});

function updatePhotoUI() {
  const file = photoInput.files && photoInput.files[0];
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl);
    previewObjectUrl = null;
  }
  if (file) {
    photoPlaceholder.style.display = "none";
    photoPreview.style.display = "block";
    photoZone.classList.add("has-image");
    previewObjectUrl = URL.createObjectURL(file);
    photoPreview.src = previewObjectUrl;
  } else {
    photoPlaceholder.style.display = "block";
    photoPreview.style.display = "none";
    photoZone.classList.remove("has-image");
    photoPreview.removeAttribute("src");
  }
}

photoInput.addEventListener("change", updatePhotoUI);

photoZone.addEventListener("keydown", (e) => {
  if (e.key === "Enter" || e.key === " ") {
    e.preventDefault();
    photoInput.click();
  }
});

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
  const difficulty = difficultyHidden.value;
  const fileInput = photoInput;

  if (!fileInput.files || !fileInput.files[0]) {
    statusEl.textContent = "You must choose a photo of the dish.";
    photoZone.scrollIntoView({ behavior: "smooth", block: "center" });
    return;
  }

  if (!name || ingredients.length === 0 || steps.length === 0) {
    statusEl.textContent = "Enter a name, at least one ingredient, and one step.";
    return;
  }

  statusEl.textContent = "";
  const submitBtn = form.querySelector('button[type="submit"]');
  submitBtn.disabled = true;

  try {
    const payload = {
      name,
      ingredients: [...ingredients],
      steps: [...steps],
      difficulty,
      imageURL: "",
    };

    const docRef = await addDoc(collection(db, "public_recipes"), payload);
    const recipeId = docRef.id;

    const file = fileInput.files[0];
    const ext = file.name.split(".").pop() || "jpg";
    const storageRef = ref(storage, `recipes/manual_${recipeId}.${ext}`);
    await uploadBytes(storageRef, file);
    const url = await getDownloadURL(storageRef);
    await updateDoc(doc(db, "public_recipes", recipeId), { imageURL: url });

    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, { myRecipes: arrayUnion(recipeId) });

    sessionStorage.removeItem(`myRecipes_${user.uid}`);
    sessionStorage.removeItem(`myRecipes_cache_v2_${user.uid}`);

    window.location.href = "index.html";
  } catch (err) {
    console.error(err);
    statusEl.textContent = err.message || "Could not save the recipe.";
    submitBtn.disabled = false;
  }
});
