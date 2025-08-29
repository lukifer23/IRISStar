# Default Models

The curated default model metadata is defined in [`app/src/main/assets/default_models.json`](app/src/main/assets/default_models.json). This file is loaded by `ModelRepositoryImpl` to provide a consistent set of starter models.

## Current default models

| Name | Supports Reasoning | Supports Vision | Chat Template |
| --- | --- | --- | --- |
| deepcogito_cogito-v1-preview-llama-3B-Q4_K_M.gguf | true | false | COGITO |
| LGAI-EXAONE_EXAONE-Deep-2.4B-Q4_K_M.gguf | true | false | EXAONE |
| NousResearch_DeepHermes-3-Llama-3-3B-Preview-Q4_K_M.gguf | false | false | DEEPHERMES |
| Qwen_Qwen3-0.6B-Q4_K_M.gguf | false | false | QWEN3 |
| Qwen_Qwen3-4B-Thinking-2507-Q4_K_M.gguf | true | false | QWEN3 |
| google_gemma-3n-E2B-it-Q4_K_M.gguf | false | true | GEMMA |
| Llama-3.2-3B-Instruct-Q4_K_L.gguf | false | false | - |
| Llama-3.2-1B-Instruct-Q6_K_L.gguf | false | false | - |
| stablelm-2-1_6b-chat.Q4_K_M.imx.gguf | false | false | - |
| NemoTron-1.5B-Q4_K_M.gguf | true | false | - |

To add, remove or update models, modify the JSON file above and regenerate this documentation if needed.
