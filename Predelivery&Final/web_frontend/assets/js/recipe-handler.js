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
      const bodyRef = document.getElementById("recipe-body");
      const stepsRef = document.getElementById("recipe-steps");

      document.getElementById("recipe-name-banner").textContent = recipe.name;
      const image = `<img src="${recipe.imageURL}" alt="${recipe.name}" class='recipe-img' />`;
      bodyRef.innerHTML += (image);
      bodyRef.innerHTML += (`<text>Ingredientes: </text><br><br>${(recipe.ingredients || [])
                              .map((ing) => `${ing}<br>`)
                              .join("")}</text>`);

      stepsRef.innerHTML += (`<text>Pasos:</text><br><br><br> <ul>${(recipe.steps || [])
                              .map((step) => `<li>${step}<br></li>`)
                              .join("")}<ul></text>`);


    }
  } catch (error) {
    console.error("Error al obtener la receta:", error);
  }
}
