# Architecture ST16 MAVLink Controller

## Vue d'ensemble

L'application est structurée en couches pour une séparation des préoccupations et une maintenabilité optimale.

```
┌─────────────────────────────────────┐
│         UI Layer (MainActivity)      │
│   - Interface utilisateur            │
│   - Affichage statut/télémétrie     │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│      Service Layer                  │
│   (DroneControlService)             │
│   - Orchestration des composants    │
│   - Boucle de contrôle              │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        │             │
┌───────▼──────┐ ┌───▼──────────┐
│   Hardware   │ │  MAVLink     │
│   Layer      │ │  Protocol    │
│              │ │              │
│ST16Stick    │ │MAVLink       │
│Reader       │ │Connection   │
└──────────────┘ └──────────────┘
        │             │
        └──────┬──────┘
               │
        ┌──────▼──────────┐
        │  WiFi Socket    │
        │  (Drone)        │
        └─────────────────┘
```

## Composants

### 1. MainActivity
- Gestion de l'interface utilisateur
- Affichage des champs IP/Port
- Boutons Connecter/Déconnecter
- Affichage du statut et télémétrie

**Fichier**: `MainActivity.kt`

### 2. DroneControlService
- Service Android qui reste actif en arrière-plan
- Orchestration de la connexion MAVLink
- Gestion du lecteur de sticks
- Boucle de contrôle 20Hz (envoi des commandes)

**Fichier**: `service/DroneControlService.kt`

### 3. MAVLinkConnection
- Gestion de la connexion TCP au drone
- Sérialisation des messages MAVLink 2.0
- Création et envoi des paquets MANUAL_CONTROL

**Fichier**: `mavlink/MAVLinkConnection.kt`

### 4. ST16StickReader
- Lecture des entrées joystick/gamepad
- Conversion des axes analogiques
- Normalisation des valeurs (-1000 à 1000)

**Fichier**: `hardware/ST16StickReader.kt`

## Flux de données

```
1. Utilisateur appuie sur "Connecter"
   ↓
2. MainActivity crée une Intent pour DroneControlService
   ↓
3. DroneControlService démarre:
   - Initialise MAVLinkConnection
   - Initialise ST16StickReader
   - Lance la boucle de contrôle
   ↓
4. Boucle de contrôle (20Hz):
   - Lit les sticks ST16
   - Crée StickData
   - Envoie MANUAL_CONTROL via MAVLink
   ↓
5. Drone reçoit et exécute les commandes
```

## Détails des messages MAVLink

### MANUAL_CONTROL (Message ID: 69)

Structure du payload (11 bytes):
```
Offset | Type    | Nom        | Range
0      | int32   | Target ID  | 1-255
4      | int16   | Roll       | -1000 à 1000
6      | int16   | Pitch      | -1000 à 1000
8      | int16   | Throttle   | 0 à 1000
10     | int16   | Yaw        | -1000 à 1000
12     | uint16  | Buttons    | Bitfield
```

### Mapping des sticks ST16

```
Axe MAVLink | Stick ST16 | Axis Android | Direction
────────────┼────────────┼──────────────┼──────────
Roll        | Gauche X   | AXIS_X       | Gauche/Droite
Pitch       | Gauche Y   | AXIS_Y       | Haut/Bas (inversé)
Yaw         | Droit X    | AXIS_RZ      | Gauche/Droite
Throttle    | Droit Y    | AXIS_Z       | Haut/Bas
```

## Points d'extensibilité

### 1. Ajouter des messages MAVLink
```kotlin
fun sendArm() { /* ... */ }
fun sendDisarm() { /* ... */ }
fun sendSetMode(mode: String) { /* ... */ }
```

### 2. Ajouter la télémétrie
```kotlin
fun receiveHeartbeat() { /* ... */ }
fun receiveSystemStatus() { /* ... */ }
```

### 3. Ajouter des modes de contrôle
```kotlin
val controlModes = listOf(
    "STABILIZE",
    "ACRO",
    "ALTITUDE_HOLD",
    "GPS_GUIDED"
)
```

## Considérations de performance

- **Boucle de contrôle**: 20Hz (50ms) pour éviter la surcharge
- **Coroutines**: Utilisation de `Dispatchers.IO` pour le réseau
- **Logging**: Timber pour un logging non-bloquant
- **Timeout**: À ajouter pour la connexion réseau

## Sécurité

- [ ] Validation des adresses IP
- [ ] Timeouts de connexion
- [ ] Gestion des erreurs de réseau
- [ ] Logs sécurisés (pas de données sensibles)
- [ ] Permissions Android minimales

## Roadmap

1. ✅ Lecture des sticks ST16
2. ✅ Communication MAVLink WiFi
3. ✅ Envoi MANUAL_CONTROL
4. [ ] Réception de télémétrie
5. [ ] Interface de monitoring
6. [ ] Modes de contrôle avancés
7. [ ] Enregistrement des logs de vol
8. [ ] Support des missions autonomes
