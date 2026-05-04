import { auth, db, storage } from "./firebase-init.js";
import { ADMIN_EMAIL } from "./constants.js";
export { ADMIN_EMAIL } from "./constants.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  arrayRemove,
  collection,
  getDocs,
  getDoc,
  doc,
  updateDoc,
  arrayUnion,
  addDoc,
  writeBatch,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

import { ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js";

function escapeHtml(text) {
  if (text == null) return "";
  const d = document.createElement("div");
  d.textContent = text;
  return d.innerHTML;
}

function scrollChatToBottom() {
  const el = document.getElementById("messages");
  if (el) el.scrollTop = el.scrollHeight;
}

const $inventoryModal = $("#inventory-modal");
const $inventoryList = $("#inventory-list");
const $newIngInput = $("#new-ingredient");

const $recipeList = $("#recipeList");

/** Cached list entries for home/explore filtering. */
let recipeListCache = [];
let userInventoryList = [];

function isExplorePage() {
  return window.location.pathname.includes("explore.html");
}

function isAdminUser() {
  return auth.currentUser?.email === ADMIN_EMAIL;
}

function recipeMatchesInventoryFilter(recipe, inventory, onlyInventory) {
  if (!onlyInventory) return true;
  const ings = recipe.ingredients || [];
  if (ings.length === 0 || !inventory.length) return false;
  return ings.some((ing) =>
    inventory.some((stock) => stock.toLowerCase().includes(ing.toLowerCase())),
  );
}

function getFilterQuery() {
  const el = document.getElementById("recipeSearch");
  return (el && el.value ? el.value : "").trim().toLowerCase();
}

function isInventoryFilterOn() {
  const el = document.getElementById("filter-by-inventory");
  return el ? el.checked : false;
}

function buildFilteredList() {
  const q = getFilterQuery();
  const invOnly = isInventoryFilterOn();
  return recipeListCache.filter((entry) => {
    const name = (entry.data.name || "").toLowerCase();
    const matchesQ = !q || name.includes(q);
    const matchesInv = recipeMatchesInventoryFilter(entry.data, userInventoryList, invOnly);
    return matchesQ && matchesInv;
  });
}

function updateFilterHint(filteredCount, totalCount) {
  const hint = document.getElementById("filter-no-matches");
  if (!hint) return;
  if (totalCount > 0 && filteredCount === 0) {
    hint.style.display = "block";
  } else {
    hint.style.display = "none";
  }
}

export function applyRecipeFilters() {
  if (!$recipeList.length) return;
  const filtered = buildFilteredList();
  $recipeList.empty();
  const showDelete = isAdminUser();
  for (const e of filtered) {
    $recipeList.append(
      getRecipeCardHTML(e.id, e.data, e.isFav, e.isSaved, { showAdminDelete: showDelete }),
    );
  }
  const $emptyState = $("#empty-state");
  if ($emptyState.length && !isExplorePage()) {
    if (recipeListCache.length === 0) {
      $emptyState.show();
    } else {
      $emptyState.hide();
    }
  }
  const exploreEmpty = document.getElementById("explore-empty");
  if (exploreEmpty) {
    exploreEmpty.style.display = recipeListCache.length === 0 ? "block" : "none";
  }
  updateFilterHint(filtered.length, recipeListCache.length);
  assignCardEvents();
}

async function syncUserInventoryFromFirebase() {
  if (!auth.currentUser) {
    userInventoryList = [];
    return;
  }
  const userDoc = await getDoc(doc(db, "users", auth.currentUser.uid));
  userInventoryList = userDoc.exists() ? userDoc.data().inventory || [] : [];
}

function persistMyRecipesCache(uid) {
  try {
    sessionStorage.setItem(`myRecipes_cache_v2_${uid}`, JSON.stringify(recipeListCache));
  } catch (_) {}
}

function loadMyRecipesCacheFromSession(uid) {
  try {
    const raw = sessionStorage.getItem(`myRecipes_cache_v2_${uid}`);
    if (!raw) return false;
    recipeListCache = JSON.parse(raw);
    return Array.isArray(recipeListCache);
  } catch (_) {
    return false;
  }
}

function invalidateRecipeCaches(uid) {
  sessionStorage.removeItem(`myRecipes_${uid}`);
  sessionStorage.removeItem(`myRecipes_cache_v2_${uid}`);
}

$(document).ready(function () {
  const $userInput = $("#user-input");
  const $sendBtn = $("#send-btn");
  const $messagesContainer = $("#messages");
  const savedchat = sessionStorage.getItem("remy_chat");
  if (savedchat && window.location.pathname.includes("index.html")) {
    $messagesContainer.html(savedchat);
    scrollChatToBottom();
  }

  $("#chat-reset-btn").on("click", () => {
    if (!confirm("Clear all messages in this chat?")) return;
    $messagesContainer.empty();
    sessionStorage.removeItem("remy_chat");
    scrollChatToBottom();
  });
  let currentUserId = null;

  window.filterRecipes = function () {
    applyRecipeFilters();
  };

  $(document).on("input", "#recipeSearch", applyRecipeFilters);
  $(document).on("change", "#filter-by-inventory", applyRecipeFilters);

  onAuthStateChanged(auth, (user) => {
    const path = window.location.pathname;
    if (user) {
      currentUserId = user.uid;
      console.log("Welcome, Chef! ID:", currentUserId);
      if (path.includes("index.html") || path === "/" || path.endsWith("/")) {
        loadMyRecipes();
      } else if (path.includes("explore.html")) {
        loadPublicRecipes();
      }
    } else {
      currentUserId = null;
      console.warn("No signed-in user; Remy cannot see your pantry.");
      if (
        path.includes("explore.html") ||
        path.includes("index.html") ||
        path === "/" ||
        path.endsWith("/")
      ) {
        loadPublicRecipes();
      }
    }
  });

  $("#open-inventory-btn").on("click", () => {
    if (!auth.currentUser) {
      alert("Sign in to manage your pantry.");
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
      await renderInventory();
      await syncUserInventoryFromFirebase();
      if ($recipeList.length) applyRecipeFilters();
    } catch (e) {
      console.error(e);
    }
  });

  async function sendMessage() {
    const message = $userInput.val().trim();
    if (!message || $sendBtn.prop("disabled")) return;

    $sendBtn.prop("disabled", true);
    $userInput.val("");

    $messagesContainer.append(`
        <div class="user-message" style="margin-bottom: 15px; text-align: right;">
            <strong style="color: #ed7d31;">You:</strong> 
            <p style="background: #fff3e0; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${escapeHtml(message)}</p>
        </div>
    `);

    const $loading = $(`<div class="bot-message" style="margin-bottom: 15px;"><strong>Remy:</strong> <em>Getting ready... 🐭🍳</em></div>`);
    $messagesContainer.append($loading);
    scrollChatToBottom();

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
        You are Remy, an expert chef. The user says: "${message}".

        User pantry / inventory: [${userInventory.join(", ")}].

        If the user asks you to create or invent a recipe, respond ONLY with a JSON object (no extra text, no markdown fences). The JSON MUST follow this exact shape (example recipe):
        {
            "name": "Pork baos",
            "ingredients": ["Bao buns", "Mayonnaise", "Cucumber", "Carrot", "Red onion", "Ground pork", "Peanuts", "Soy sauce", "Sriracha"],
            "steps": ["Prep vegetables.", "Pickle cucumber, carrot, onion with vinegar and salt.", "Mix mayo and sriracha; crush peanuts.", "Cook pork with soy until glazed.", "Steam bao buns.", "Fill buns with pork, veg, sauces and peanuts."],
            "difficulty": "Low",
            "imagePrompt": "A detailed photorealistic prompt to generate an image of this dish, e.g. close-up of steamed bao buns with glazed pork and pickles, cinematic lighting."
        }

        You may use pantry items or omit some as needed for the recipe.
        Difficulty must be one of: High, Medium, Low.

        If the user asks a normal question (not to create a recipe), reply as a friendly chef in plain text.
        `;

      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: enrichedPrompt, userId: currentUserId }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.text || "Server error");
      }

      const data = await response.json();
      $loading.remove();

      let botReply = data.text;

      try {
        const start = botReply.indexOf("{");
        const end = botReply.lastIndexOf("}");
        if (start !== -1 && end !== -1) {
          const cleanJsonString = botReply.substring(start, end + 1);
          let recipeData = JSON.parse(cleanJsonString);

          if (recipeData.name && recipeData.ingredients && recipeData.steps) {
            if (botReply.includes("{") && botReply.includes("name")) {
              const cleaned = botReply.replace(/```json/g, "").replace(/```/g, "").trim();
              recipeData = JSON.parse(cleaned);
            }

            const $imgLoading = $(`<div class="bot-message" style="margin-bottom: 15px; color: #757575;"><em>Plating the dish...</em></div>`);
            $messagesContainer.append($imgLoading);
            scrollChatToBottom();

            let finalImageURL = "images/placeholder.png";

            try {
              const imgRes = await fetch("/api/generate-image", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prompt: recipeData.imagePrompt }),
              });

              const imgData = await imgRes.json();

              if (imgData.base64) {
                const byteCharacters = atob(imgData.base64);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                  byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);
                const imageType = imgData.type || "image/png";
                const blob = new Blob([byteArray], { type: imageType });

                const fileExtension = imageType === "image/jpeg" ? "jpg" : "png";
                const uniqueName = `recipes/${Date.now()}_${recipeData.name.replace(/\s+/g, "_")}.${fileExtension}`;
                const storageRef = ref(storage, uniqueName);

                await uploadBytes(storageRef, blob);

                finalImageURL = await getDownloadURL(storageRef);
              }
            } catch (imgError) {
              console.error("Image generate/upload error:", imgError);
            }

            $imgLoading.remove();

            recipeData.imageURL = finalImageURL;

            await addDoc(collection(db, "public_recipes"), recipeData);

            botReply = `Voilà! I created a new recipe using your pantry: **${recipeData.name}**. It's saved to the public cookbook with its photo. 🐭✨`;

            loadPublicRecipes();
          }
        }
      } catch (e) {
        console.error("Remy JSON parse error:", e);
      }

      $messagesContainer.append(`
            <div class="bot-message" style="margin-bottom: 15px;">
                <strong style="color: #2e7d32;">Remy:</strong> 
                <p style="background: #e8f5e9; display: inline-block; padding: 8px 12px; border-radius: 10px; margin: 0;">${botReply}</p>
            </div>
        `);
      scrollChatToBottom();
      sessionStorage.setItem("remy_chat", $messagesContainer.html());
    } catch (error) {
      $loading.remove();
      console.error("Error:", error);
    } finally {
      $sendBtn.prop("disabled", false);
    }
  }

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
      $inventoryList.html("<p style='color:gray; font-size:0.8em;'>Your pantry is empty.</p>");
      return;
    }

    items.forEach((item) => {
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
      const userRefInner = doc(db, "users", auth.currentUser.uid);
      await updateDoc(userRefInner, { inventory: arrayRemove(name) });
      await renderInventory();
      await syncUserInventoryFromFirebase();
      if ($recipeList.length) applyRecipeFilters();
    });
  }
}

async function loadPublicRecipes() {
  try {
    await syncUserInventoryFromFirebase();
    let userFavorites = [];
    let userSavedRecipes = [];

    if (auth.currentUser) {
      const userDoc = await getDoc(doc(db, "users", auth.currentUser.uid));
      if (userDoc.exists()) {
        userFavorites = userDoc.data().favorites || [];
        userSavedRecipes = userDoc.data().myRecipes || [];
      }
    }

    const querySnapshot = await getDocs(collection(db, "public_recipes"));
    recipeListCache = [];
    querySnapshot.forEach((docSnap) => {
      const recipeId = docSnap.id;
      recipeListCache.push({
        id: recipeId,
        data: docSnap.data(),
        isFav: userFavorites.includes(recipeId),
        isSaved: userSavedRecipes.includes(recipeId),
      });
    });
    applyRecipeFilters();
  } catch (error) {
    console.error("loadPublicRecipes error:", error);
  }
}

async function moveRecipeToTrash(recipeId) {
  if (!isAdminUser()) return;
  const publicRef = doc(db, "public_recipes", recipeId);
  const snap = await getDoc(publicRef);
  if (!snap.exists()) return;
  const data = snap.data();
  const batch = writeBatch(db);
  batch.set(doc(db, "trashed_recipes", recipeId), data);
  batch.delete(publicRef);
  await batch.commit();
  if (auth.currentUser) {
    sessionStorage.removeItem(`favorites_${auth.currentUser.uid}`);
    invalidateRecipeCaches(auth.currentUser.uid);
  }
  if (isExplorePage()) await loadPublicRecipes();
  else await loadMyRecipes();
}

async function toggleFavorite(recipeId, isCurrentlyFav, $btn) {
  const user = auth.currentUser;
  if (!user) return alert("Sign in to continue.");

  const userRef = doc(db, "users", user.uid);
  try {
    if (isCurrentlyFav) {
      await updateDoc(userRef, { favorites: arrayRemove(recipeId) });
      $btn.removeClass("active").html('<i class="fa-regular fa-star"></i> Add');
    } else {
      await updateDoc(userRef, { favorites: arrayUnion(recipeId) });
      $btn.addClass("active").html('<i class="fa-solid fa-star"></i> Favorite');
    }
    sessionStorage.removeItem(`favorites_${auth.currentUser.uid}`);
    const entry = recipeListCache.find((e) => e.id === recipeId);
    if (entry) entry.isFav = !isCurrentlyFav;
    if (window.location.pathname.includes("favorites.html")) {
      await loadFavorites();
    } else {
      applyRecipeFilters();
    }
  } catch (e) {
    console.error(e);
  }
}

async function toggleSaveRecipe(recipeId, isCurrentlySaved, $btn) {
  const user = auth.currentUser;
  if (!user) return alert("Sign in to continue.");

  const userRef = doc(db, "users", user.uid);
  try {
    if (isCurrentlySaved) {
      await updateDoc(userRef, { myRecipes: arrayRemove(recipeId) });
      $btn.removeClass("active").html('<i class="fa-regular fa-bookmark"></i> Save');
    } else {
      await updateDoc(userRef, { myRecipes: arrayUnion(recipeId) });
      $btn.addClass("active").html('<i class="fa-solid fa-bookmark"></i> Saved');
    }
    invalidateRecipeCaches(auth.currentUser.uid);
    const entry = recipeListCache.find((e) => e.id === recipeId);
    if (entry) entry.isSaved = !isCurrentlySaved;
    applyRecipeFilters();
  } catch (e) {
    console.error(e);
  }
}

function getRecipeCardHTML(recipeId, recipe, isFav, isSaved, options = {}) {
  const { showAdminDelete = false } = options;
  const favIcon = isFav ? "fa-solid" : "fa-regular";
  const saveIcon = isSaved ? "fa-solid" : "fa-regular";

  const adminBtn = showAdminDelete
    ? `<button type="button" class="admin-trash-recipe-btn action-btn" data-id="${recipeId}" title="Move to trash"><i class="fa-solid fa-trash"></i></button>`
    : "";

  return `
        <article data-id="${recipeId}" class="recipe-card">
            <div class="recipe-info">
                <img src="${recipe.imageURL || "images/placeholder.png"}" alt="${recipe.name}" class="cardImage" />
                
                <span class="recipe-title">${recipe.name}</span>
            </div>
            <div class="ingredients-box">
                <h3>Ingredients</h3>
                <ul>
                    ${(recipe.ingredients || [])
                      .slice(0, 5)
                      .map((ing) => `<li>${ing}</li>`)
                      .join("")}
                </ul>
                <div class="card-actions" style="display: flex; gap: 10px; margin-top: 15px; flex-wrap: wrap;">
                    <button class="toggle-fav-btn action-btn ${isFav ? "active" : ""}" 
                            data-id="${recipeId}" title="Favorites">
                        <i class="${favIcon} fa-star"></i>
                        <span>${isFav ? "Favorite" : "Add"}</span>
                    </button>
                    <button class="toggle-save-btn action-btn ${isSaved ? "active" : ""}" 
                            data-id="${recipeId}" title="Save to my recipes">
                        <i class="${saveIcon} fa-bookmark"></i>
                        <span>${isSaved ? "Saved" : "Save"}</span>
                    </button>
                    ${adminBtn}
                </div>
            </div>
        </article>
    `;
}

async function loadMyRecipes() {
  const user = auth.currentUser;
  const $emptyState = $("#empty-state");

  if (!user || !$recipeList.length) return;

  await syncUserInventoryFromFirebase();

  if (loadMyRecipesCacheFromSession(user.uid)) {
    applyRecipeFilters();
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    const userDoc = await getDoc(userRef);

    recipeListCache = [];

    if (!userDoc.exists()) {
      $emptyState.show();
      return;
    }

    const myRecipesIDs = userDoc.data().myRecipes || [];
    const favoritesIDs = userDoc.data().favorites || [];

    if (myRecipesIDs.length === 0) {
      $emptyState.show();
      return;
    }

    $emptyState.hide();

    for (const id of myRecipesIDs) {
      const recipeSnap = await getDoc(doc(db, "public_recipes", id));
      if (recipeSnap.exists()) {
        const isFav = favoritesIDs.includes(id);
        recipeListCache.push({
          id,
          data: recipeSnap.data(),
          isFav,
          isSaved: true,
        });
      }
    }

    persistMyRecipesCache(user.uid);
    applyRecipeFilters();
  } catch (e) {
    console.error("loadMyRecipes error:", e);
  }
}

function assignCardEvents() {
  $(".recipe-card").off("click").on("click", function () {
    const id = $(this).data("id");
    window.location.href = `recipe.html?id=${id}`;
  });

  $(".toggle-fav-btn").off("click").on("click", function (event) {
    event.stopPropagation();
    const id = $(this).data("id");
    const isCurrentlyFav = $(this).hasClass("active");
    toggleFavorite(id, isCurrentlyFav, $(this));
  });

  $(".toggle-save-btn").off("click").on("click", function (event) {
    event.stopPropagation();
    const id = $(this).data("id");
    const isCurrentlySaved = $(this).hasClass("active");
    toggleSaveRecipe(id, isCurrentlySaved, $(this));
  });

  $(".admin-trash-recipe-btn").off("click").on("click", function (event) {
    event.stopPropagation();
    if (!confirm("Move this recipe to trash?")) return;
    const id = $(this).data("id");
    moveRecipeToTrash(id);
  });
}

export async function loadFavorites() {
  try {
    const user = auth.currentUser;
    if (!user) return;

    const $favoriteListContainer = $("#favorite-list");
    if (!$favoriteListContainer.length) return;

    const cachedRecipes = sessionStorage.getItem(`favorites_${user.uid}`);
    if (cachedRecipes) {
      $favoriteListContainer.html(cachedRecipes);
      assignCardEvents();
      return;
    }

    const userDoc = await getDoc(doc(db, "users", user.uid));
    if (userDoc.exists()) {
      const favoriteIDs = userDoc.data().favorites || [];
      const myRecipesIDs = userDoc.data().myRecipes || [];

      $favoriteListContainer.empty();

      for (const id of favoriteIDs) {
        const recipeSnap = await getDoc(doc(db, "public_recipes", id));
        if (recipeSnap.exists()) {
          const isSaved = myRecipesIDs.includes(id);
          const html = getRecipeCardHTML(id, recipeSnap.data(), true, isSaved, { showAdminDelete: false });
          $favoriteListContainer.append(html);
        }
      }
      sessionStorage.setItem(`favorites_${user.uid}`, $favoriteListContainer.html());
      assignCardEvents();
    }
  } catch (error) {
    console.error("loadFavorites error:", error);
  }
}

async function addToFavorites(recipeId) {
  const user = auth.currentUser;
  if (!user) {
    alert("Sign in to save favorites.");
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, {
      favorites: arrayUnion(recipeId),
    });
    alert("Recipe added to favorites! ⭐");
  } catch (error) {
    console.error("addToFavorites error:", error);
  }
}

async function removeFromFavorites(recipeId) {
  const user = auth.currentUser;
  if (!user) {
    alert("Sign in to manage favorites.");
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    await updateDoc(userRef, {
      favorites: arrayRemove(recipeId),
    });

    alert("Recipe removed from favorites.");
    loadFavorites();
  } catch (error) {
    console.error("Remove favorite error:", error);
    alert("Could not remove the recipe.");
  }
}
