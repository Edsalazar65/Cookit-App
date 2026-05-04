import { auth, db } from "./firebase-init.js";
import {
  signInWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  GoogleAuthProvider,
  signOut,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

import {
  doc,
  getDoc,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const loginForm = document.getElementById("login-form");
const googleBtn = document.getElementById("google-login-btn");

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = document.getElementById("login-email").value;
  const pass = document.getElementById("login-password").value;

  try {
    await signInWithEmailAndPassword(auth, email, pass);
    window.location.href = "index.html";
  } catch (error) {
    alert("Error al iniciar sesión. Verifica tus credenciales o regístrate.");
  }
});

googleBtn.addEventListener("click", async () => {
  const provider = new GoogleAuthProvider();
  try {
    const result = await signInWithPopup(auth, provider);
    const user = result.user;

    const userRef = doc(db, "users", user.uid);
    const docSnap = await getDoc(userRef);

    if (!docSnap.exists()) {
      
      await signOut(auth); // Cerramos su sesión inmediatamente
      alert(
        "No encontramos tu cuenta. Por favor, ve a Sign Up para registrarte.",
      );
      return; // Detenemos el código para que no viaje al index.html
    }
    window.location.href = "index.html";
  } catch (error) {
    console.error("Error Google Login:", error.code, error);
  }
});
