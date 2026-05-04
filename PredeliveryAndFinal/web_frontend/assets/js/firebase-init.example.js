import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "API key aqui",
  authDomain: "cookit-app-4a59d.firebaseapp.com",
  projectId: "cookit-app-4a59d",
  storageBucket: "cookit-app-4a59d.firebasestorage.app",
  messagingSenderId: "801437778194",
  appId: "1:801437778194:web:8effb5327fb55bdcfc33cb",
  measurementId: "G-LZ0KNRPX9C"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);