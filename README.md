# ScanFlow

A modern Android document scanner built with Jetpack Compose. Capture documents with your camera, enhance them with filters, annotate with drawing tools, and save as PDF — all in a clean, intuitive interface.

## Features

- **Smart Scanning** — Capture multiple pages using CameraX with flash control and gallery import
- **Image Enhancement** — Apply filters (B/W, Lighten, HD Clear, Magic Color), rotate, and crop
- **Drawing Tools** — Annotate pages with pencil, marker, and highlighter in multiple colors with undo/redo
- **PDF Management** — Save, rename, delete, and share documents as PDFs
- **Page Editing** — Add, replace, or reorder pages in existing documents
- **Organized Library** — Browse and search documents by category (Work, Personal, ID Cards, Receipts)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Camera | CameraX 1.4 |
| Navigation | Navigation Compose 2.8 |
| Images | Coil 2.6 |
| PDF | Android PdfDocument / PdfRenderer |
| Architecture | Clean Architecture (Domain → Data → UI) + MVVM |

## Architecture

```
domain/          → Models & repository interface
data/            → PDF I/O, MediaStore storage, repository implementation
ui/
 ├── screens/    → Splash, Files, Camera, Preview, Detail, Edit, About
 ├── components/ → DocumentCard, BottomNavigationBar
 ├── navigation/ → NavHost setup & route definitions
 └── theme/      → Material 3 colors, typography, dark/light mode
```

## Screenshots

<!-- Add screenshots here -->
![WhatsApp Image 2026-04-13 at 1 30 12 AM (1)](https://github.com/user-attachments/assets/61bcb887-6e9f-4f2a-afcc-a11292d6b5cf)
![WhatsApp Image 2026-04-13 at 1 30 12 AM](https://github.com/user-attachments/assets/f2fcafa8-1a95-42bb-9378-8fa8468f6e10)
![WhatsApp Image 2026-04-13 at 1 30 11 AM](https://github.com/user-attachments/assets/d59c507c-5301-46d2-8e2c-4e7d84ceda72)
![WhatsApp Image 2026-04-13 at 1 30 13 AM](https://github.com/user-attachments/assets/3c6699a1-18ac-4549-b393-4378adb32802)


## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/your-username/ScanFlow.git
   ```
2. Open in Android Studio (Ladybug or newer)
3. Build and run on a device with API 26+

## Requirements

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36
- **Camera permission** required

## License

This project is for personal/educational use.

---

*Built by [Mahabir Neogy](https://www.linkedin.com/in/mahabir-neogy)*
