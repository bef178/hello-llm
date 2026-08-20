# LLM Hub

```bash
curl -X POST 'http://localhost:8482/api/dispatch' \
  --header 'content-type: application/json' \
  --header 'x-request-id: 222' \
  --header 'x-session-id: 333' \
  --header 'x-device-id: abc' \
  --data '{
  "traceId": 111,
  "openai": {
    "model": "GPT-5 mini",
    "messages": [
      {
        "role": "system",
        "content": "You are an AI assistant."
      },
      {
        "role": "user",
        "content": "Hello"
      }
    ],
    "stream": true
  },
  "trace": true
}'
```