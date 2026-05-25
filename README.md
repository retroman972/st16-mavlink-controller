# ST16 MAVLink Controller

Application Android pour utiliser la télécommande Yuneec ST16 afin de piloter un drone via le protocole MAVLink.

## Caractéristiques

- 📡 Communication WiFi MAVLink (port 14550)
- 🕹️ Lecture directe du firmware Yuneec pour les sticks de contrôle
- 🖥️ Interface optimisée pour écran tactile 7" (1920x1200)
- ✈️ Envoi de commandes MANUAL_CONTROL au drone
- 📊 Affichage de la télémétrie en temps réel

## Architecture

```
st16-mavlink-controller/
├── app/
│   ├── src/main/
│   │   ├── java/com/retroman972/st16mavlink/
│   │   │   ├── MainActivity.kt
│   │   │   ├── mavlink/
│   │   │   │   ├── MAVLinkConnection.kt
│   │   │   │   └── MAVLinkProtocol.kt
│   │   │   ├── hardware/
│   │   │   │   ├── ST16StickReader.kt
│   │   │   │   └── YuneecFirmwareAPI.kt
│   │   │   ├── ui/
│   │   │   │   ├── ControllerUI.kt
│   │   │   │   └── TelemetryDisplay.kt
│   │   │   └── service/
│   │   │       └── DroneControlService.kt
│   │   └── res/
│   │       ├── layout/
│   │       └── values/
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Prérequis

- Android 8.0+ (API level 26)
- Yuneec ST16 avec firmware récent
- Drone compatible MAVLink (Pixhawk, APM, etc.)
- Connexion WiFi

## Installation

```bash
git clone https://github.com/retroman972/st16-mavlink-controller.git
cd st16-mavlink-controller
./gradlew build
./gradlew installDebug
```

## Configuration

1. Connecter la ST16 au WiFi du drone
2. Lancer l'application
3. Entrer l'adresse IP et le port du drone (défaut: 127.0.0.1:14550)
4. Appuyer sur "Connecter"

## Utilisation

- **Sticks gauche/droit** : Lecture automatique du firmware Yuneec
- **Throttle** : Commande MANUAL_CONTROL Z
- **Pitch/Roll** : Commandes MANUAL_CONTROL X/Y
- **Yaw** : Commandes MANUAL_CONTROL R

## Documentation MAVLink

- [MAVLink Protocol Reference](https://mavlink.io/)
- [MANUAL_CONTROL Message](https://mavlink.io/en/messages/common.html#MANUAL_CONTROL)

## Licence

MIT

## Auteur

retroman972
