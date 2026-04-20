import { auth, db } from "./firebase-init.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  arrayRemove,
  collection,
  getDocs,
  getDoc,
  doc,
  updateDoc,
  arrayUnion,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const $recipeList = $("#recipeList");
const $favoriteList = $("#favorite-list");

$(document).ready(function () {
  const $userInput = $("#user-input");
  const $sendBtn = $("#send-btn");
  const $messagesContainer = $("#messages");
  let currentUserId = null;

  loadPublicRecipes();




  onAuthStateChanged(auth, (user) => {
    if (user) {
      currentUserId = user.uid;
      console.log("Welcome, Chef! ID:", currentUserId);
    } else {
      currentUserId = null;
      console.warn(
        "No hay usuario detectado. Remy no podrá ver el inventario.",
      );
    }
  });

  async function sendMessage() {
    const message = $userInput.val().trim();
    if (!message || $sendBtn.prop("disabled")) return;

    $sendBtn.prop("disabled", true);
    $userInput.val("");

    $messagesContainer.append(`
        <div class="user-message" style="margin-bottom: 15px; text-align: right;">
            <strong style="color: #ed7d31;">Tú:</strong> 
            <p style="background: #fff3e0; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${message}</p>
        </div>
    `);

    const $loading = $(
      `<div class="bot-message" style="margin-bottom: 15px;"><strong>Remy:</strong> <em> Preparando... 🐭🍳</em></div>`,
    );
    $messagesContainer.append($loading);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          prompt: message,
          userId: currentUserId,
        }),
      });
      $messagesContainer.scrollTop($messagesContainer[0].scrollHeight);

      const data = await response.json();
      $loading.remove();

      $messagesContainer.append(`
          <div class="bot-message" style="margin-bottom: 15px;">
              <strong style="color: #2e7d32;">Remy:</strong> 
              <p style="background: #e8f5e9; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${data.text}</p>
          </div>
      `);
      $messagesContainer.scrollTop($messagesContainer[0].scrollHeight);
    } catch (error) {
      $loading.remove();
      console.error("Error:", error);
    } finally {
      $sendBtn.prop("disabled", false);
    }
  }

  // EVENTOS
  $sendBtn.on("click", sendMessage);

  $userInput.on("keypress", (e) => {
    if (e.which === 13) sendMessage();
  });
});

async function loadPublicRecipes() {
  try {
    const querySnapshot = await getDocs(collection(db, "public_recipes"));
    $recipeList.empty();

    querySnapshot.forEach((doc) => {
      const recipe = doc.data();
      const recipeId = doc.id;

      const recipeCard = `
                <article data-id="${recipeId}" class="recipe-card">
                    <div class="recipe-info">
                        <img src="${recipe.imageURL}" alt="${recipe.name} class='cardImage'" />
                        <span class="recipe-title">${recipe.name}</span>
                        
                    </div>
                    <div class="ingredients-box">
                        <h3>Ingredientes</h3>
                        <ul>
                            ${(recipe.ingredients || [])
                              .slice(0, 5)
                              .map((ing) => `<li>${ing}</li>`)
                              .join("")}
                        </ul>
                        <button class="fav-btn add-fav-btn" data-id="${recipeId}" class="fav-btn">
                            Añadir a Favoritos ⭐
                        </button>
                    </div>
                </article>
            `;
      $recipeList.append(recipeCard);
    });


    //Asignar evento de click a los cards
    $(".recipe-card").on("click", function (){
      const id = $(this).data("id");
      window.location.href=`recipe.html?id=${id}`;

    });

    // Asignar evento a los botones de favoritos recién creados
    $(".add-fav-btn").on("click", function (event) {
      event.stopPropagation();
      const id = $(this).data("id");
      addToFavorites(id);
    });
  } catch (error) {
    console.error("Error al cargar recetas:", error);
  }
}

export async function loadFavorites() {
  try {
    const user = auth.currentUser;
    if (!user) return;

    const userRef = doc(db, "users", user.uid);
    const userDoc = await getDoc(userRef);

    if (userDoc.exists()) {
      const favoriteIDs = userDoc.data().favorites || [];
      $favoriteList.empty();

      for (const recipeID of favoriteIDs) {
        const recipeRef = doc(db, "public_recipes", recipeID);
        const recipeSnap = await getDoc(recipeRef);
        if (recipeSnap.exists()) {
          const recipe = recipeSnap.data();

          const recipeCard = `
                <article data-id="${recipeID}" class="recipe-card">
                    <div class="recipe-info">
                        <img src="${recipe.imageURL}" alt="${recipe.name} class='cardImage'" />
                        <span class="recipe-title">${recipe.name}</span>                  
                    </div>
                    <div class="ingredients-box">
                        <h3>Ingredientes</h3>
                        <ul>
                            ${(recipe.ingredients || [])
                              .slice(0, 5)
                              .map((ing) => `<li>${ing}</li>`)
                              .join("")}
                        </ul>
                        <button class="fav-btn remove-fav-btn" data-id="${recipeID}" >
                            Eliminar de Favoritos ❌
                        </button>
                    </div>
                </article>
            `;
          $favoriteList.append(recipeCard);
        }
      }
    }

    //Asignar evento de click a los cards
    $(".recipe-card").on("click", function (){
      const id = $(this).data("id");
      window.location.href=`recipe.html?id=${id}`;

    });

    // Asignar evento de eliminar de favoritos
    $(".remove-fav-btn").on("click", function (event) {
      event.stopPropagation();
      const id = $(this).data("id");
      removeFromFavorites(id);
    });
  } catch (error) {
    console.error("Error al cargar recetas:", error);
  }
}

//  Función para guardar en el array favorites del usuario
async function addToFavorites(recipeId) {
  const user = auth.currentUser;
  if (!user) {
    alert("Debes iniciar sesión para guardar favoritos.");
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, {
      favorites: arrayUnion(recipeId), 
    });
    alert("¡Receta añadida a tus favoritos! ⭐");
  } catch (error) {
    console.error("Error al guardar favorito:", error);
  }
}

async function removeFromFavorites(recipeId) {
  const user = auth.currentUser;
  if (!user) {
    alert("Debes iniciar sesión para guardar favoritos.");
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, {
      favorites: arrayRemove(recipeId),
    });

    alert("Receta eliminada de favoritos.");
    loadFavorites();
  } catch (error) {
    console.error("Error al eliminar de favoritos:", error);
    alert("No se pudo eliminar la receta.");
  }
}
