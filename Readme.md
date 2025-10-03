# IrisStar

**⚠️ ACTIVE DEVELOPMENT - This project is currently in active development and may contain bugs, incomplete features, or breaking changes.**

## Project Description

This repository contains a llama.cpp-based offline Android chat application that provides a private, secure, and fully offline AI chat experience.

### Key Features

- **Offline-First Design**: All model inference is performed locally on your device; no prompts or responses are sent to external servers
- **Privacy-Focused**: User data and chat history remain on the device and are not collected or transmitted
- **Expandable Models**: Download external GGUF models from Hugging Face
- **Customizable Parameters**: Adjust n_threads, top_k, top_p, and temperature to optimize performance and behavior
- **GPU Acceleration**: Support for Vulkan and OpenCL backends for improved performance on compatible devices
- **Open Source**: Fully transparent development with easy modification capabilities

**Note**: This application is currently in active development. Features may be incomplete or subject to change.

## Images

![Main Screen Screenshot](./images/main_screen.png)
**Main Screen**  
This is the main interface of the app where users can access all core functionalities.

![Chat Screen Screenshot](./images/chat_screen.png)
**Chat Screen**  
The chat feature allows users to interact in real-time and access AI-driven responses.

![Settings Screen Screenshot](./images/settings_screen.png)
**Settings Screen**  
Users can customize app preferences and configure their account settings here.

![Models Screen Screenshot](./images/models_screen.png)
**Models Screen**  
This screen displays available AI models and allows users to manage them efficiently.

![Parameters Screen Screenshot](./images/parameters_screen.png)
**Parameters Screen**  
Users can adjust parameters to fine-tune the app's performance based on their needs.

## Installation

**Note**: Pre-built releases are not currently available as this project is in active development. To run the application:

1. Clone this repository
2. Build the project using the instructions in the Build section below
3. Install the resulting APK on your Android device

## Features

### Core Features

- **Offline-First Design**: All model inference is performed locally on your device; no prompts or responses are sent to external servers
- **Privacy-Focused**: User data and chat history remain on the device and are not collected or transmitted
- **Expandable Models**: Download external GGUF models from Hugging Face
- **Customizable Parameters**: Adjust n_threads, top_k, top_p, and temperature to optimize performance and behavior
- **GPU Acceleration**: Support for Vulkan and OpenCL backends for improved performance on compatible devices
- **Open Source**: Fully transparent development with easy modification capabilities

### Advanced Features (In Development)

- Text-to-Speech functionality
- Speech-to-Text functionality
- Default model selection and management
- Advanced chat management features

**Note**: Some advanced features are still in development and may not be fully functional.

## Security & Privacy

- All model inference is performed locally on your device; no prompts or responses are sent to external servers.
- User data and chat history remain on the device and are not collected or transmitted.
- Network access is only required for downloading models. Once models are installed, the app can run fully offline.

## GPU Backends (Vulkan/OpenCL) on Android

IrisStar integrates llama.cpp GPU backends via dynamic loading for improved performance on compatible devices:

### Supported Backends

- **Vulkan**: Preferred on modern Android devices (especially Adreno GPUs). Loaded at runtime from system `libvulkan.so` and packaged `libggml-vulkan.so` plugin
- **OpenCL**: Loaded if vendor `libOpenCL.so` is available; the ggml OpenCL plugin is loaded dynamically when present
- **CPU**: Always available as fallback option

### Configuration and Testing

1. Navigate to Settings → Detect Hardware to populate available backends (e.g., `CPU,Vulkan`)
2. Select your preferred Backend (Vulkan/OpenCL/CPU) in Settings
3. The app will unload and reload the model for the change to take effect
4. Use the Benchmark screen to run CPU vs GPU tests and compare throughput

### Performance Optimization

**Recommended Settings for Mobile GPUs:**

- Context length (`n_ctx`): 2048 tokens (default)
- Batch size (`n_batch`): 256 tokens (default)
- Micro-batch size (`n_ubatch`): 64 tokens (default)
- Generation limit: 256 tokens per response (default)
- GPU layers: Auto (offload as many as fit) or manually configure in Settings

**Troubleshooting:**

- If you see `failed to find ggml_backend_init in libggml-vulkan.so` during startup but Vulkan is reported as present, this is benign; the backend registry still succeeds
- GPU latency issues: Try smaller models or reduce context length in Settings
- Backend switching problems: The app forcefully unloads/reloads models on backend changes

### Runtime Diagnostics

- Native diagnostics snapshot available in UI logs after model load
- Includes backend registry, active contexts, GPU offload counts, KV cache size, and micro-batch configuration
- Backend switching is safe - runtime defers teardown when contexts are active
- GPU benchmark skips GPU path if device reports zero offload or CPU-forced session
- Token logging is gated to avoid excessive log traffic (verbose logs available in development builds)

## Model Selection and Performance

The performance of IrisStar is directly influenced by the size, speed, and compute requirements of the models you use. These factors significantly impact the overall user experience.

### Performance Considerations

- **Smaller models** are ideal for quicker interactions but may compromise slightly on response quality
- **Larger models** offer more comprehensive responses but require higher compute power and may result in slower speeds
- **GPU acceleration** can significantly improve performance on compatible devices

### Model Recommendations

On opening the app, users can download suggested models to optimize performance based on their preferences and device capabilities. Choose a model that best balances speed and quality for your specific use case.

**Important Disclaimer:**

- IrisStar may produce **inaccurate results** depending on the complexity of queries and model limitations
- Performance and accuracy are influenced by the size and type of model selected
- This application is in active development; expect potential bugs and incomplete features

## Prerequisites

- Install [JDK 17](https://adoptium.net/).
- Install [Android Studio](https://developer.android.com/studio).
- Initialize the `llama.cpp` submodule:

```bash
git submodule update --init --recursive
```

## Development Setup

### Prerequisites

- **JDK 17** - Install from [Eclipse Adoptium](https://adoptium.net/)
- **Android Studio** - Latest stable version from [developer.android.com](https://developer.android.com/studio)
- **Android SDK** - API level 28 or higher
- **Git** - For cloning the repository

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/lukifer23/IRISStar.git
cd IRISStar
```

2. Initialize submodules:
```bash
git submodule update --init --recursive
```

3. Open the project in Android Studio and build the APK

### GPU Backend Support

**Vulkan Backend:**
- No extra steps required on device
- Ensure `vulkan-headers` are installed on host for cross-compilation (e.g., via Homebrew on macOS)

**OpenCL Backend:**
- Requires vendor `libOpenCL.so` on the target device
- The app loads the ggml OpenCL backend at runtime when available

### Device Setup for Development

1. Enable Developer Options on your Android device
2. Enable USB Debugging or Wireless Debugging
3. For wireless debugging:
   - Connect both devices to the same Wi-Fi network
   - Scan the QR code displayed in Android Studio
4. Select your device in Android Studio and run the app

**Note**: After installation, download at least one model through the app's interface to enable full functionality.

## Contributing

**Note**: This project is in active development. Contributions are welcome but please be aware that the codebase may undergo significant changes.

### Development Guidelines

1. **Fork the repository** and create a feature branch
2. **Follow the existing code style** and architecture patterns
3. **Test your changes** thoroughly before submitting
4. **Update documentation** if adding new features
5. **Submit a pull request** with a clear description of changes

### Getting Started with Development

```bash
# Fork and clone the repository
git clone https://github.com/YOUR_USERNAME/IRISStar.git
cd IRISStar

# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes and test them
# ...

# Commit your changes
git commit -m 'Add your feature description'

# Push to your fork
git push origin feature/your-feature-name

# Open a pull request
```

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Write tests for new functionality

**Note**: Please ensure your contributions align with the project's privacy-first, offline-capable design philosophy.
