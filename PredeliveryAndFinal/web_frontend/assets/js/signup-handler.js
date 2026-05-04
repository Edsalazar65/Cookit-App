import { auth, db } from "./firebase-init.js";
import {
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
  doc,
  setDoc,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const signupForm = document.getElementById("signup-form");
const googleBtn = document.getElementById("google-signup-btn");

signupForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = document.getElementById("signup-email").value;
  const pass = document.getElementById("pass-key").value;
  const confirmPass = document.getElementById("confirm-pass").value;
  const name = document.getElementById("signup-name").value;

  if (pass !== confirmPass) {
    alert("Las contrasenas no coinciden");
    return;
  }

  try {
    const userCred = await createUserWithEmailAndPassword(auth, email, pass);
    const user = userCred.user;
    await setDoc(doc(db, "users", user.uid), {
      name: name,
      email: email,
      inventory: [],
      myRecipes: [],
      favorites: [],
      photoURL: ""
    });
    window.location.href = "index.html";
  } catch (error) {
    alert("Error al crear cuenta" + error.message);
  }
});

googleBtn.addEventListener("click", async () => {
  const provider = new GoogleAuthProvider();

  try {
    const result = await signInWithPopup(auth, provider);
    const user = result.user;

    await setDoc(
      doc(db, "users", user.uid),
      {
        name: user.displayName,
        email: user.email,
        inventory: [],
        myRecipes: [],
        favorites: [],
      },
      { merge: true },
    );

    window.location.href = "index.html";
  } catch (error) {
    console.error("Error al hacer Sign Up con Google", error);
  }
});
