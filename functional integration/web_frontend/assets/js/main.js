import { auth } from "./firebase-init.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

$(document).ready(function () {
  const $userInput = $("#user-input");
  const $sendBtn = $("#send-btn");
  const $messagesContainer = $("#messages");

  let currentUserId = null;

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

    const $loading = $(`<div class="bot-message" style="margin-bottom: 15px;"><strong>Remy:</strong> <em> Preparando... 🐭🍳</em></div>`);
    $messagesContainer.append($loading);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          prompt: message,
          userId: currentUserId
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

  $userInput.on("keypress", (e) => { if (e.which === 13) sendMessage(); });
});
