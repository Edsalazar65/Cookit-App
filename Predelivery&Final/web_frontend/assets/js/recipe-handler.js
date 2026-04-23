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
      const diffRef = document.getElementById("difficulty-body");


      document.getElementById("recipe-name-banner").textContent = recipe.name;
      const image = document.createElement("img");
    // `<img src="${recipe.imageURL}" alt="${recipe.name}" class='recipe-img' />`
      image.src= recipe.imageURL;
      image.className = "recipe-img";
      bodyRef.prepend(image);
      bodyRef.innerHTML += (`<p> <ul>${(recipe.ingredients || [])
                              .map((ing) => `<li>${ing}<br></li>`)
                              .join("")}</ul></p>`);

      const diff = document.createElement("p");
      diff.textContent =recipe.difficulty;
      diff.style= "padding-top:0px";
      diffRef.append(diff);
      

      stepsRef.innerHTML += (` <ol style=" margin-left: 20px;">${(recipe.steps || [])
                              .map((step) => `<li>${step}<br></li>`)
                              .join("")}</ol>`);


    }
  } catch (error) {
    console.error("Error al obtener la receta:", error);
  }
}
