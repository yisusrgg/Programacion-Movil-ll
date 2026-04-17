const express = require('express');
const admin = require('firebase-admin');
const path = require('path');

// Inicializar Firebase Admin con tu clave privada
const serviceAccount = require('./serviceAccount.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();
app.use(express.json());
app.use(express.static('public'));

// Endpoint para enviar notificación push
app.post('/send', async (req, res) => {
  const { token, title, body } = req.body;

  if (!token || !title || !body) {
    return res.status(400).json({ error: 'Faltan campos: token, title, body' });
  }

  const message = {
    notification: { title, body },
    data: { title, body },  // también como data para recibirlo en foreground
    token: token
  };

  try {
    const response = await admin.messaging().send(message);
    console.log('Mensaje enviado:', response);
    res.json({ success: true, messageId: response });
  } catch (error) {
    console.error('Error enviando mensaje:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Servidor FCM corriendo en http://localhost:${PORT}`);
});