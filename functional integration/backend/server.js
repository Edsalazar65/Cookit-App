const express = require("express");
const cors = require("cors");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const path = require("path");
require("dotenv").config();

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "web")));

// CAMBIO CLAVE: Forzamos el uso de la API estable (v1)
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

app.post("/api/chat", async (req, res) => {
  try {
    const { prompt } = req.body;

    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash", // O el que estés usando
      systemInstruction:
        "Eres Remy, un ayudante de cocina en una aplicacion en la que se guardan recetas. " +
        "Dices palabras en frances de vez en cuando."+
        "siempre usas analogías culinarias.",
    });

    const fullPrompt = `Eres Remy de Ratatouille. Responde breve y con emojis: ${prompt}`;

    const result = await model.generateContent(fullPrompt);
    const response = await result.response;
    const text = response.text();

    res.json({ text: text });
  } catch (error) {
    console.error("ERROR EN TERMINAL:", error);

    if (error.status === 429) {
      return res.status(429).json({
        text: "🐭 ¡Cálmate, lingüini! Estoy cocinando demasiado rápido. Dame un minuto para respirar.",
      });
    }

    res.status(500).json({
      text: "🐭 Ups, se me quemó la salsa. ¿Puedes intentar de nuevo?",
    });
  }
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`\n✅ Cocina abierta en http://localhost:${PORT}`);
});
