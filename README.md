Java Video Editor(VibeCoded + unfinished)
Overview
A desktop video editor written in Java 11, combining a Swing interface with FFmpeg-powered processing.
It uses VLCJ for playback/preview and FlatLaf for modern look and feel.

Features
Project & track management – create projects with multiple video, audio, and subtitle tracks.

Segment editing – add, split, copy, and remove timeline segments; audio tracks can overlap for mixing.

Export service – render projects using FFmpeg with customizable resolution, frame rate, and quality.

Thumbnail caching – generate and cache thumbnails for quick timeline navigation.

Logging & diagnostics – detailed console logging for troubleshooting.

Requirements
Java 11+

FFmpeg available on the system PATH

VLC installed (required by VLCJ)

Gradle (wrapper included)

Optional: JMF libraries if you intend to use legacy media features.

Building
./gradlew build
Artifacts will appear in build/libs/.

Running
./gradlew run
The application starts with the Swing-based main window.

Project Structure
src/main/java/com/videoeditor/
├── core/          # Metadata, thumbnail, and processing services
├── export/        # Export dialog and service
├── model/         # Project, track, and segment models
├── ui/            # Swing UI components (controllers, panels, dialogs)
└── Main.java      # Application entry point
Contributing
Fork and clone the repository.

Create feature branches and follow the existing code style.

Submit pull requests with clear descriptions and ensure code compiles.

License
Specify your chosen license (e.g., MIT, Apache 2.0, etc.) here.
