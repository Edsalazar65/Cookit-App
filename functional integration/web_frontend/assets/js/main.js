import { auth,db } from "./firebase-init.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  collection,
  getDocs,
  doc,
  updateDoc,
  arrayUnion,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const $recipeList = $("#recipeList");

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
                <article class="recipe-card" style="margin-bottom: 20px;">
                    <div class="recipe-info">
                        <img src="images/comida1.png" alt="${recipe.name}" />
                        <span class="recipe-title">${recipe.name}</span>
                        <p style="font-size: 0.9em; color: #666;">${recipe.description}</p>
                    </div>
                    <div class="ingredients-box">
                        <h3>Ingredients</h3>
                        <ul>
                            ${(recipe.ingredients || []).map(ing => `<li>${ing}</li>`).join('')}
                        </ul>
                        <button class="button2 add-fav-btn" data-id="${recipeId}" style="margin-top:10px; margin-left:0; width:100%;">
                            Añadir a Favoritos ⭐
                        </button>
                    </div>
                </article>
            `;
      $recipeList.append(recipeCard);
    });

    // Asignar evento a los botones de favoritos recién creados
    $(".add-fav-btn").on("click", function () {
      const id = $(this).data("id");
      addToFavorites(id);
    });
  } catch (error) {
    console.error("Error al cargar recetas:", error);
  }
}

// 2. Función para guardar en el array 'favorites' del usuario
async function addToFavorites(recipeId) {
  const user = auth.currentUser;
  if (!user) {
    alert("Debes iniciar sesión para guardar favoritos, Chef.");
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, {
      favorites: arrayUnion(recipeId), // Guarda el ID de la receta
    });
    alert("¡Receta añadida a tus favoritos! ⭐");
  } catch (error) {
    console.error("Error al guardar favorito:", error);
  }
}
