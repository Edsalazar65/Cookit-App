import { doc, getDoc } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
import { auth, db, storage } from "./firebase-init.js";
import { ADMIN_EMAIL } from "./constants.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { signOut } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getStorage, ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js";
import { getFirestore, updateDoc } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

const nameDisplay = document.getElementById("profile-username");
const logOutBtn = document.getElementById("logout-btn");

const editBadge = document.querySelector('.edit-badge');
const avatarInput = document.getElementById('avatar-input');
const cropModal = document.getElementById('crop-modal');
const imageToCrop = document.getElementById('image-to-crop');
const cancelCropBtn = document.getElementById('cancel-crop');
const confirmCropBtn = document.getElementById('confirm-crop');
const profileAvatar = document.querySelector('.profile-avatar');

let cropper;

editBadge.addEventListener('click', () => {
    avatarInput.click();
});

avatarInput.addEventListener('change', (event) => {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            imageToCrop.src = e.target.result;
            cropModal.style.display = 'flex';


            if (cropper) {
                cropper.destroy();
            }


            cropper = new Cropper(imageToCrop, {
                aspectRatio: 1,
                viewMode: 1,
                dragMode: 'move'
            });
        };
        reader.readAsDataURL(file);
    }

    avatarInput.value = '';
});

cancelCropBtn.addEventListener('click', () => {
    cropModal.style.display = 'none';
    if (cropper) cropper.destroy();
});


confirmCropBtn.addEventListener('click', async () => {
    if (!cropper) return;

    const originalText = confirmCropBtn.innerText;
    confirmCropBtn.innerText = 'Subiendo...';
    confirmCropBtn.disabled = true;


    cropper.getCroppedCanvas({
        width: 400,
        height: 400
    }).toBlob(async (blob) => {
        try {
            const user = auth.currentUser;
            if (!user) throw new Error("No hay usuario autenticado");

            const storageRef = ref(storage, `avatars/${user.uid}.jpg`);


            await uploadBytes(storageRef, blob);


            const downloadURL = await getDownloadURL(storageRef);

            const userDocRef = doc(db, "users", user.uid);
            await updateDoc(userDocRef, {
                photoURL: downloadURL
            });

            profileAvatar.src = downloadURL;


            cropModal.style.display = 'none';
            cropper.destroy();

        } catch (error) {
            console.error("Error al subir la imagen: ", error);
            alert("Hubo un error al subir la foto de perfil.");
        } finally {
            confirmCropBtn.innerText = originalText;
            confirmCropBtn.disabled = false;
        }
    }, 'image/jpeg', 0.8);
});

onAuthStateChanged(auth, async (user) => {
    if (user) {
        const userDoc = await getDoc(doc(db, "users", user.uid));

        if (userDoc.exists()) {
            const userData = userDoc.data();

            let rawName = userData.name || user.email.split("@")[0];
            
            const firstName = rawName.trim().split(" ")[0];
            nameDisplay.textContent = `Chef ${firstName}`;

            const myR = userData.myRecipes || [];
            const fav = userData.favorites || [];
            const inv = userData.inventory || [];
            const elR = document.getElementById("stat-recipes");
            const elF = document.getElementById("stat-favorites");
            const elI = document.getElementById("stat-ingredients");
            if (elR) elR.textContent = String(myR.length);
            if (elF) elF.textContent = String(fav.length);
            if (elI) elI.textContent = String(inv.length);

            const trashLink = document.getElementById("admin-trash-link");
            if (trashLink) {
                trashLink.style.display = user.email === ADMIN_EMAIL ? "inline-block" : "none";
            }

            if (userData.photoURL != "") {
                profileAvatar.src = userData.photoURL;
            } else {
                profileAvatar.src = "images/default-chef.png";
            }
        }
    } else {
        window.location.href = "login.html";
    }
});

logOutBtn.addEventListener("click", (e) => {
    e.preventDefault();
    signOut(auth).then(() => {
        window.location.href = "login.html";

    });
});