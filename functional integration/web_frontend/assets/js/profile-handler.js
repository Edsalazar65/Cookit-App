import { doc, getDoc } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
import { auth, db } from "./firebase-init.js";
import {onAuthStateChanged} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {signOut} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

const nameDisplay= document.getElementById("profile-username");
const logOutBtn=document.getElementById("logout-btn");

onAuthStateChanged(auth, async(user)=> {
    if (user){
        const userDoc= await getDoc(doc(db, "users", "user.uid"));
        if (userDoc.exists() && userDoc.data().name){
            nameDisplay.textContent= `Chef ${userDoc.data().name}`;
        } else if(user.displayName){
            nameDisplay.textContent=`Chef ${user.displayName}`;
        } else{
            nameDisplay.textContent=`Chef ${user.email.split("@")[0]}`;
        }
    } else{
        window.location.href= "login.html";
    }
});

logOutBtn.addEventListener("click", (e)=>{
    e.preventDefault();
    signOut(auth).then(()=>{
        window.location.href= "login.html";

    });
});