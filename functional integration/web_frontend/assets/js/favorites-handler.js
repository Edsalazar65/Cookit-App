import { auth } from "./firebase-init.js";
import {onAuthStateChanged} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {loadFavorites} from "./main.js";

onAuthStateChanged(auth, async(user)=> {
    if (user){
        loadFavorites();
    } else{
        window.location.href= "login.html";
    }
});
