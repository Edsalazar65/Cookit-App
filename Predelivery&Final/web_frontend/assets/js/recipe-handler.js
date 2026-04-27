import { db } from "./firebase-init.js";
import {
  doc,
  getDoc,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

$(document).ready(function () {
  loadRecipe();
});

async function loadRecipe() {
  const urlParams = new URLSearchParams(window.location.search);
  const recipeId = urlParams.get("id");

  if (!recipeId) {
    console.error("No se encontró un ID en la URL");
    return;
  }

  try {
    const recipeRef = doc(db, "public_recipes", recipeId);
    const recipeSnap = await getDoc(recipeRef);

    if (recipeSnap.exists()) {

      const recipe = recipeSnap.data();
      const hero = document.getElementById("recipe-hero");
      hero.style.backgroundImage = `url('${recipe.imageURL}')`;

      document.getElementById("recipe-name-banner").textContent = recipe.name;
      const diffBadge = document.getElementById("difficulty-badge");
      diffBadge.textContent = recipe.difficulty || "Normal";

  

      const bodyRef = document.getElementById("recipe-body");


      bodyRef.innerHTML = `<ul>${(recipe.ingredients || [])
        .map((ing) => `<li>${ing}</li>`)
        .join("")}</ul>`;

      
      const stepsRef = document.getElementById("recipe-steps");
      stepsRef.innerHTML = `<ol>${(recipe.steps || [])
        .map((step) => `<li style="margin-bottom: 15px;">${step}</li>`)
        .join("")}</ol>`;


    }
  } catch (error) {
    console.error("Error al obtener la receta:", error);
  }
}
