import { auth, db, storage } from "./firebase-init.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  arrayRemove,
  collection,
  getDocs,
  getDoc,
  doc,
  updateDoc,
  arrayUnion,
  addDoc
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

import { ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js";

const $inventoryModal = $("#inventory-modal");
const $inventoryList = $("#inventory-list");
const $newIngInput = $("#new-ingredient");

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

  $("#open-inventory-btn").on("click", () => {
    if (!auth.currentUser) {
      alert("Inicia sesión para gestionar tu inventario 🐭");
      return;
    }
    $inventoryModal.fadeIn(200);
    renderInventory();
  });

  $("#close-inventory").on("click", () => $inventoryModal.fadeOut(200));



  $("#add-ingredient-btn").on("click", async () => {
    const item = $newIngInput.val().trim();
    if (!item) return;

    try {
      const userRef = doc(db, "users", auth.currentUser.uid);
      await updateDoc(userRef, { inventory: arrayUnion(item) });
      $newIngInput.val("");
      renderInventory();
    } catch (e) { console.error(e); }
  });

  async function sendMessage() {
    const message = $userInput.val().trim();
    if (!message || $sendBtn.prop("disabled")) return;

    $sendBtn.prop("disabled", true);
    $userInput.val("");

    // 1. Mostrar mensaje del usuario
    $messagesContainer.append(`
        <div class="user-message" style="margin-bottom: 15px; text-align: right;">
            <strong style="color: #ed7d31;">Tú:</strong> 
            <p style="background: #fff3e0; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${message}</p>
        </div>
    `);

    const $loading = $(`<div class="bot-message" style="margin-bottom: 15px;"><strong>Remy:</strong> <em> Preparando... 🐭🍳</em></div>`);
    $messagesContainer.append($loading);

    try {

      let userInventory = [];
      if (currentUserId) {
        const userRef = doc(db, "users", currentUserId);
        const userDoc = await getDoc(userRef);
        if (userDoc.exists() && userDoc.data().inventory) {
          userInventory = userDoc.data().inventory;
        }
      }

      const enrichedPrompt = `
        Eres Remy, un chef experto. El usuario te dice: "${message}".
        
        Inventario actual del usuario: [${userInventory.join(", ")}].
        
        Si el usuario te pide crear o inventar una receta, debes responder ÚNICAMENTE con un objeto JSON (sin texto adicional, sin formato markdown). El JSON DEBE tener esta estructura exacta (Este es un ejemplo de receta):
        {
            "name": "Baos de carne",
            "ingredients": ["Pan bao", "Mayonesa", "Pepino", "Zanahoria", "Cebolla roja", "Carne de cerdo picada", "Mani", "Salsa de soya", "Sriracha"],
            "steps": ["Corta los vegetales", "En un bowl, agrega el pepino, la zanahoria, la cebolla, el vinagre de vino tinto y la sal para encurtir.", "En un bol pequeño, mezcla la mayonesa y la sriracha. Con la base de un cazo, aplasta los cacahuetes en su propia bolsa para picarlos. Añade la mitad de los cacahuetes picados al bol con las verduras encurtidas.", "En una sartén, calienta un chorrito de aceite a fuego medio-alto. Agrega la carne picada de cerdo, salpimienta y cocina 4-5 min, desmenuzando con una espátula, hasta que se dore. Agrega la salsa de soja dulce y un chorrito de agua y cocina 1-2 min más, hasta que reduzca y la carne quede melosa.", "Coloca los panes bao en un plato, sin que se toquen entre ellos, y calienta en el microondas entre 40 y 60 segundos a máxima potencia, hasta que queden tiernos y esponjosos.", "En el interior de cada pan bao, agrega carne picada, zanahoria rallada y cebolla encurtida al gusto. Añade encima mayonesa de sriracha y cacahuetes picados. Sirve los encurtidos restantes a un lado como acompañamiento."],
            "difficulty": "Fácil",
            "imagePrompt": "Un prompt detallado y fotorrealista para generar una imagen de esta receta. Ejemplo: 'A close-up photograph of steamed bao buns filled with glazed minced pork, pickled cucumber, and carrots, topped with chopped peanuts and sriracha mayo, served on a ceramic plate, cinematic lighting'."
        }
        
        Si el usuario te hace una pregunta normal (no pide crear receta), responde como un chef amigable en texto normal.
        `;


      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          prompt: enrichedPrompt, // Enviamos el prompt enriquecido
          userId: currentUserId,
        }),
      });

      const data = await response.json();
      $loading.remove();

      let botReply = data.text;

      try {
        const cleanJsonString = botReply.replace(/```json/g, "").replace(/```/g, "").trim();
        const recipeData = JSON.parse(cleanJsonString);

        if (recipeData.name && recipeData.ingredients && recipeData.steps) {

          await addDoc(collection(db, "public_recipes"), recipeData);

          botReply = `¡Voilà! He creado una nueva receta usando lo que tienes en tu despensa: **${recipeData.name}**. La he guardado en el recetario principal para que puedas verla. 🐭✨`;

          loadPublicRecipes();
        }
      } catch (e) {
      }

      $messagesContainer.append(`
            <div class="bot-message" style="margin-bottom: 15px;">
                <strong style="color: #2e7d32;">Remy:</strong> 
                <p style="background: #e8f5e9; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${botReply}</p>
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


async function renderInventory() {
  const userRef = doc(db, "users", auth.currentUser.uid);
  const userDoc = await getDoc(userRef);

  $inventoryList.empty();

  if (userDoc.exists() && userDoc.data().inventory) {
    const items = userDoc.data().inventory;

    if (items.length === 0) {
      $inventoryList.html("<p style='color:gray; font-size:0.8em;'>Tu despensa está vacía.</p>");
      return;
    }

    items.forEach(item => {
      const chip = $(`
                <div class="ingredient-chip">
                    <span>${item}</span>
                    <span class="remove-chip" data-name="${item}">&times;</span>
                </div>
            `);
      $inventoryList.append(chip);
    });


    $(".remove-chip").on("click", async function () {
      const name = $(this).data("name");
      const userRef = doc(db, "users", auth.currentUser.uid);
      await updateDoc(userRef, { inventory: arrayRemove(name) });
      renderInventory();
    });
  }
}



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
    $(".recipe-card").on("click", function () {
      const id = $(this).data("id");
      window.location.href = `recipe.html?id=${id}`;

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
    $(".recipe-card").on("click", function () {
      const id = $(this).data("id");
      window.location.href = `recipe.html?id=${id}`;

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
