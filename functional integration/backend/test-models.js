const { GoogleGenerativeAI } = require("@google/generative-ai");
require("dotenv").config();

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

async function listRealModels() {
  try {
    // Esta es la forma correcta de listar en versiones recientes
    const response = await fetch(`https://generativelanguage.googleapis.com/v1/models?key=${process.env.GEMINI_API_KEY}`);
    const data = await response.json();
    
    console.log("--- MODELOS QUE PUEDES USAR ---");
    if (data.models) {
      data.models.forEach(m => console.log("- " + m.name.replace("models/", "")));
    } else {
      console.log("No se encontraron modelos. Revisa tu API KEY.");
    }
  } catch (e) {
    console.error("Error al listar:", e);
  }
}

listRealModels();