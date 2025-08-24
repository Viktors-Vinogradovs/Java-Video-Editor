# Java Video Editor (Vibe Coded)

[![GitHub repo](https://img.shields.io/badge/repo-Java--Video--Editor-blue)](https://github.com/Viktors-Vinogradovs/Java-Video-Editor)

A desktop video editor written in **Java 11**, combining a Swing interface with **FFmpeg** for processing.
Uses **VLCJ** for playback/preview and **FlatLaf** for a modern look and feel.

 **Status:** Work in Progress – some features may be incomplete or unstable.

---

##  Features

* **Project & Track Management** – create projects with multiple video, audio, and subtitle tracks.
* **Segment Editing** – add, split, copy, and remove timeline segments; audio tracks can overlap for mixing.
* **Export Service** – render projects using FFmpeg with customizable resolution, frame rate, and quality.
* **Thumbnail Caching** – generate and cache thumbnails for smooth timeline navigation.
* **Logging & Diagnostics** – detailed console logs for troubleshooting.

---

##  Requirements

* **Java 11+**
* **FFmpeg** installed and available on system `PATH`
* **VLC** installed (required by VLCJ)
* **Gradle** (wrapper included)
* *(Optional)* JMF libraries if legacy media features are needed

---

##  Building

```bash
./gradlew build
```

Artifacts will be available in `build/libs/`.

---

##  Running

```bash
./gradlew run
```

The application starts with the Swing-based main window.

---

##  Project Structure

```
src/main/java/com/videoeditor/
├── core/          # Metadata, thumbnail, and processing services
├── export/        # Export dialog and service
├── model/         # Project, track, and segment models
├── ui/            # Swing UI components (controllers, panels, dialogs)
└── Main.java      # Application entry point
```

---

##  Contributing

1. Fork and clone the repository
2. Create a feature branch and follow the existing code style
3. Submit a pull request with a clear description and ensure the code compiles

---

##  License

Specify your chosen license here (e.g., **MIT**, **Apache 2.0**, etc.).

---

**Repository:** [Viktors-Vinogradovs/Java-Video-Editor](https://github.com/Viktors-Vinogradovs/Java-Video-Editor)
