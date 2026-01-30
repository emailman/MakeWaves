# Make Waves

A Kotlin Multiplatform 3D wave animation demonstrating a sphere falling into water and creating realistic ripple effects. The project runs on Desktop (JVM with OpenGL) and Web (WebAssembly with WebGL).

## Features

- Real-time 3D graphics rendering with OpenGL (Desktop) and WebGL (Web)
- Physics simulation for falling sphere with gravity
- Procedural wave generation with circular ripples spreading from impact point
- Wireframe rendering mode for visualizing mesh geometry
- Automatic animation reset after completion
- Cross-platform support: Windows, macOS, Linux, and modern web browsers

## Demo

The animation shows:
1. A red sphere falling from above
2. Impact with a blue translucent water surface
3. Circular waves radiating outward from the impact point
4. The sphere slowly sinking beneath the surface

## Controls

| Key | Action |
|-----|--------|
| `R` | Reset animation |
| `W` | Toggle wireframe mode |
| `ESC` | Exit (Desktop only) |

## Requirements

- JDK 17 or higher
- Gradle 8.x (included via wrapper)

## Running the Application

### Desktop (JVM with OpenGL)

```bash
./gradlew :composeApp:run
```

On Windows:
```cmd
.\gradlew.bat :composeApp:run
```

### Web (WebAssembly)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

This starts a development server at `http://localhost:8080/`.

## Building

### Desktop Distribution

```bash
# Windows MSI
./gradlew :composeApp:packageMsi

# macOS DMG
./gradlew :composeApp:packageDmg

# Linux DEB
./gradlew :composeApp:packageDeb
```

### Web Production Build

```bash
./gradlew :composeApp:wasmJsBrowserProductionWebpack
```

Output will be in `composeApp/build/dist/wasmJs/productionExecutable/`.

## Project Structure

```
MakeWaves/
├── composeApp/
│   └── src/
│       ├── commonMain/          # Shared code (physics, animation, math)
│       │   └── kotlin/edu/ericm/
│       │       ├── AnimationController.kt  # Animation state management
│       │       ├── AnimationState.kt       # State data class
│       │       ├── MeshGenerator.kt        # Procedural mesh generation
│       │       ├── PhysicsSimulation.kt    # Gravity and collision
│       │       ├── Vector3.kt              # 3D vector math
│       │       └── WaveSimulation.kt       # Wave height calculations
│       ├── jvmMain/             # Desktop-specific (OpenGL/LWJGL)
│       │   └── kotlin/edu/ericm/
│       │       ├── main.kt
│       │       ├── OpenGLRenderer.kt
│       │       └── Shaders.kt
│       └── wasmJsMain/          # Web-specific (WebGL)
│           ├── kotlin/edu/ericm/
│           │   ├── main.kt
│           │   ├── WebGLRenderer.kt
│           │   ├── WebGLShaders.kt
│           │   └── Matrix4.kt
│           └── resources/
│               └── index.html
├── gradle/
│   └── libs.versions.toml       # Version catalog
└── build.gradle.kts
```

## Technology Stack

- **Kotlin Multiplatform** 2.1.21
- **Compose Multiplatform** 1.9.3
- **LWJGL** 3.3.4 (Desktop OpenGL bindings)
- **JOML** 1.10.8 (Java OpenGL Math Library)
- **WebGL** (Browser rendering)
- **Kotlin/Wasm** (WebAssembly compilation)

## How It Works

### Physics
The sphere starts at position (0, 5, 0) and accelerates downward due to gravity (-9.8 m/s²). Upon hitting the water surface at y=0, it records the impact position and begins sinking slowly.

### Wave Simulation
Waves are calculated using the formula:
- Wave radius expands at a constant speed from impact point
- Height follows a damped sine wave pattern
- Multiple overlapping wave rings create a more realistic effect
- Amplitude decreases with time (exponential damping) and distance

### Rendering
Both platforms use similar shader programs:
- Vertex shader: Transforms 3D positions and calculates lighting normals
- Fragment shaders: Phong lighting for sphere, semi-transparent blue for water, solid color for wireframe

## License

MIT License
