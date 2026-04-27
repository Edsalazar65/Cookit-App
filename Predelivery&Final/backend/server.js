require("dotenv").config(); // 1. Mover al inicio
const express = require("express");
const cors = require("cors");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const path = require("path");
const admin = require("firebase-admin");

const serviceAccount = require("./serviceAccountKey.json");

try {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  console.log("✅ Firebase Admin conectado");
} catch (error) {
  console.error("❌ Error inicializando Firebase:", error);
}

const db = admin.firestore();
const app = express();

app.use(cors());
app.use(express.json());


app.get("/", (req, res)=>{res.sendFile(path.join(__dirname, "..", "web_frontend", "login.html"));});
app.use(express.static(path.join(__dirname, "..", "web_frontend")));

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

app.post("/api/chat", async (req, res) => {
  try {
    const { prompt, userId } = req.body;
    let inventoryContext = "";

    if (userId) {
      const userDoc = await db.collection("users").doc(userId).get();
      if (userDoc.exists) {
        const inventory = userDoc.data().inventory || [];
        inventoryContext = `[Contexto: El usuario tiene estos ingredientes: ${inventory.join(", ")}.] `;
      }
    }

    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash",
      systemInstruction:
        "Eres Remy, un chef experto y apasionado. " +
        "Dices palabras en francés como 'Oui', 'Mon ami' o 'Magnifique', pero no muy seguido " +
        "Siempre usas analogías culinarias. " +
        "Si el usuario tiene ingredientes en su inventario, menciónalos para sugerir qué cocinar." +
        "Se breve con tus respuestas.",
    });

    const finalPrompt = `${inventoryContext} Usuario dice: ${prompt}`;
    const result = await model.generateContent(finalPrompt);
    const response = await result.response;
    res.json({ text: response.text() });
  } catch (error) {
    console.error("ERROR EN TERMINAL:", error);
    res.status(500).json({ text: "🐭 Ups, se me quemó la salsa. ¿Intentas de nuevo?" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Cocina abierta en http://localhost:${PORT}`);
});
