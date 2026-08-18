package com.example.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SampleContent(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val rawMarkdown: String
)

object SampleData {
    val samples = listOf(
        SampleContent(
            title = "AI Coding Architecture",
            description = "Kotlin Coroutines & Compose with syntax highlighting and callouts",
            icon = Icons.Default.Code,
            rawMarkdown = """
# Modern State Management with StateFlow

Here is how you handle asynchronous state cleanly in Jetpack Compose using modern Kotlin coroutines.

> [!NOTE]
> Always collect StateFlow in Compose using `collectAsStateWithLifecycle()` to avoid leaking resources when the app goes into the background.

## 1. ViewModel Implementation

```kotlin
class UserProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                val data = repository.fetchUser(userId)
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
```

## 2. Performance Comparison

| Mechanism | Lifecycle-Aware | Recomposition Scope | Memory Footprint |
| :--- | :---: | :---: | ---: |
| `StateFlow` | Yes | Targeted | Ultra Low |
| `LiveData` | Yes | Component | Medium |
| `ObservableField` | No | Full Tree | High |

## Key Checklist
- [x] Integrate `androidx.lifecycle.runtime.compose`
- [x] Replace legacy `remember { mutableStateOf() }` with reactive flows
- [ ] Add unit tests using `kotlinx-coroutines-test`

> [!TIP]
> Use `derivedStateOf` when deriving values that change frequently to prevent unnecessary recompositions!
""".trimIndent()
        ),
        SampleContent(
            title = "AI Prompting & Python Guide",
            description = "Python scripts, prompt techniques, and formulas",
            icon = Icons.Default.AutoAwesome,
            rawMarkdown = """
# Gemini 2.0 Function Calling Guide

Integrating structured tools with Large Language Models empowers autonomous agent workflows.

```python
import google.generativeai as genai

def get_current_weather(location: str, unit: str = "celsius") -> dict:
    '''Fetches live weather conditions for a given city.'''
    return {"city": location, "temp": 24, "condition": "Sunny", "unit": unit}

model = genai.GenerativeModel(
    model_name="gemini-2.0-flash",
    tools=[get_current_weather]
)

chat = model.start_chat(enable_automatic_function_calling=True)
response = chat.send_message("What's the weather in Tokyo right now?")
print(response.text)
```

## Mathematical Scoring Formula
For calculating embedding cosine similarity:

${'$'}${'$'} \text{Cosine Similarity} = \frac{\mathbf{A} \cdot \mathbf{B}}{\|\mathbf{A}\|_2 \|\mathbf{B}\|_2} = \frac{\sum_{i=1}^{n} A_i B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \sqrt{\sum_{i=1}^{n} B_i^2}} ${'$'}${'$'}

> [!WARNING]
> Keep temperature low (${'$'}T \le 0.2${'$'}) when requiring deterministic structured tool calling to prevent hallucinatory schema violations.

---
### Next Steps
1. Define your schema using Pydantic or Python type hints
2. Register custom callback hooks for validation
3. Add safety filters and graceful error handling
""".trimIndent()
        ),
        SampleContent(
            title = "REST API & JSON Payload",
            description = "JSON formatting, HTTP endpoint documentation, and tables",
            icon = Icons.Default.DataObject,
            rawMarkdown = """
# API Reference: User Authentication

This endpoint authenticates client devices and issues time-limited JWT bearer tokens.

### Endpoint: `POST /api/v2/auth/login`

**Request Payload:**
```json
{
  "email": "developer@example.com",
  "auth_method": "oauth2_google",
  "client_version": "2.4.0",
  "device_info": {
    "platform": "Android 15",
    "device_model": "Pixel 7 Pro",
    "screen_density": 420
  }
}
```

**Response Parameters:**

| Field | Type | Required | Description |
| :--- | :---: | :---: | :--- |
| `access_token` | `string` | **Yes** | Standard JWT bearer token with 1hr validity |
| `refresh_token` | `string` | **Yes** | Long-lived rotating token |
| `expires_in` | `integer` | **Yes** | Seconds until expiry (default: 3600) |
| `user_role` | `string` | No | Assigned ACL permission level |

> [!CAUTION]
> Never store raw client secrets or plain-text refresh tokens in unencrypted SharedPreferences!
""".trimIndent()
        ),
        SampleContent(
            title = "Research Paper & Scientific Notes",
            description = "Academic notes with quotes, mathematical formulas, and checklists",
            icon = Icons.Default.School,
            rawMarkdown = """
# Attention Is All You Need — Key Highlights

The Transformer architecture eschews recurrence and convolutions entirely, relying solely on multi-head self-attention mechanisms.

> "We propose the Transformer, a model architecture eschewing recurrence and instead relying entirely on an attention mechanism to draw global dependencies between input and output."
> — Vaswani et al. (2017)

## Scaled Dot-Product Attention
The core calculation is computed as follows:

${'$'}${'$'} \text{Attention}(Q, K, V) = \text{softmax}\left(\frac{Q K^T}{\sqrt{d_k}}\right) V ${'$'}${'$'}

Where:
- ${'$'}Q${'$'} is the Query matrix of dimension ${'$'}d_k${'$'}
- ${'$'}K${'$'} is the Key matrix of dimension ${'$'}d_k${'$'}
- ${'$'}V${'$'} is the Value matrix of dimension ${'$'}d_v${'$'}

### Architectural Advantages
- **Constant ${'$'}O(1)${'$'} sequential operations** compared to ${'$'}O(n)${'$'} in RNNs
- **Significantly higher parallelizability** during matrix multiplication on GPUs
- **Direct path length** between long-range tokens reduces gradient degradation
""".trimIndent()
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplePromptsSheet(
    onSelectSample: (SampleContent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("samples_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sample Raw AI Responses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Tap any sample to see how complex raw markdown, code blocks, callouts, and tables are formatted beautifully:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SampleData.samples) { sample ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectSample(sample)
                                onDismiss()
                            }
                            .testTag("sample_item_${sample.title.take(10).lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = sample.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sample.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sample.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
