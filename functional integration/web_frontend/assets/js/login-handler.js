import { auth } from "./firebase-init.js";
import { 
    signInWithEmailAndPassword, 
    signInWithPopup, 
    GoogleAuthProvider 
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

const loginForm = document.getElementById('login-form');
const googleBtn = document.getElementById('google-login-btn');


loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const pass = document.getElementById('login-password').value;

    try {
        await signInWithEmailAndPassword(auth, email, pass);
        window.location.href = "index.html"; // Redirigir al chat
    } catch (error) {
        alert("Error al iniciar sesión: " + error.message);
    }
});


googleBtn.addEventListener('click', async () => {
    const provider = new GoogleAuthProvider();
    try {
        await signInWithPopup(auth, provider);
        window.location.href = "index.html";
    } catch (error) {
        console.error("Error Google Login:", error);
    }
});