import { auth, db } from './firebase-init.js';
import { doc, getDoc, updateDoc } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";



$(document).ready(function () {
  const $userInput = $("#user-input");
  const $sendBtn = $("#send-btn");
  const $messagesContainer = $("#messages");

  async function sendMessage() {
    const message = $userInput.val().trim();
    if (!message || $sendBtn.prop("disabled")) return;

    $sendBtn.prop("disabled", true);
    $userInput.val(""); // Limpiar entrada

    // 1. Mostrar mensaje del usuario al PRINCIPIO (usando prepend)
    $messagesContainer.prepend(`
        <div class="user-message" style="margin-bottom: 10px; text-align: right;">
            <strong style="color: #ed7d31;">Tú:</strong> <span>${message}</span>
        </div>
    `);

    // 2. Mostrar indicador de carga al principio
    const $loading = $(
      '<div class="bot-message"><em>Remy está pensando... 🐭</em></div>',
    );
    $messagesContainer.prepend($loading);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: message }),
      });

      const data = await response.json();
      $loading.remove(); // Quitar indicador de carga

      const respuestaRemy =
        data && data.text
          ? data.text
          : "🐭 Oh non! Algo salió mal en mi cocina.";

      // 3. Mostrar respuesta de Remy al PRINCIPIO
      $messagesContainer.prepend(`
          <div class="bot-message">
              <strong>Remy:</strong> <span>${respuestaRemy}</span>
          </div>
      `);

      // 4. Opcional: Volver el scroll al tope si el contenedor tiene scroll
      $messagesContainer.scrollTop(0);
    } catch (error) {
      $loading.remove();
      $messagesContainer.prepend(
        `<p style="color:red;">Remy se escondió en la cocina.</p>`,
      );
    } finally {
      $sendBtn.prop("disabled", false);
    }
  }

  // Evento click
  $sendBtn.on("click", sendMessage);

  // Evento Enter
  $userInput.on("keypress", function (e) {
    if (e.which === 13) sendMessage();
  });
});
