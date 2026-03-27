import admin from "firebase-admin";
import serviceAccount from './*-*-firebase-adminsdk-*-*.json' with { type: 'json' };
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

async function sendNotification() {
  try {
    const res = await admin.messaging().send({
      data: {
        title: "Vectras VM",
        message: "Welcome!",
        image: "https://github.com/xoureldeen/Vectras-VM-Android/blob/master/resources/vectrasvm.png",
        url : "https://github.com/xoureldeen/Vectras-VM-Android"
      },
      topic: "vectrasvmandroidgithub"
    });

    console.log("Sent:", res);
  } catch (err) {
    console.error("Error:", err);
  }
}

sendNotification();