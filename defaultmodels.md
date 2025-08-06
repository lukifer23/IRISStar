1. Cogito-llama-3b
    link:
    https://huggingface.co/bartowski/deepcogito_cogito-v1-preview-llama-3B-GGUF/resolve/main/deepcogito_cogito-v1-preview-llama-3B-Q4_K_M.gguf
    
    Chat Template:
    {{- bos_token }}
{%- if not tools is defined %}
    {%- set tools = none %}
{%- endif %}
{%- if not enable_thinking is defined %}
    {%- set enable_thinking = false %}
{%- endif %}
{#- This block extracts the system message, so we can slot it into the right place. #}
{%- if messages[0]['role'] == 'system' %}
    {%- set system_message = messages[0]['content']|trim %}
    {%- set messages = messages[1:] %}
{%- else %}
    {%- set system_message = "" %}
{%- endif %}
{#- Set the system message. If enable_thinking is true, add the "Enable deep thinking subroutine." #}
{%- if enable_thinking %}
    {%- if system_message != "" %}
        {%- set system_message = "Enable deep thinking subroutine.

" ~ system_message %}
    {%- else %}
        {%- set system_message = "Enable deep thinking subroutine." %}
    {%- endif %}
{%- endif %}
{#- Set the system message. In case there are tools present, add them to the system message. #}
{%- if tools is not none or system_message != '' %}
    {{- "<|start_header_id|>system<|end_header_id|>

" }}
    {{- system_message }}
    {%- if tools is not none %}
        {%- if system_message != "" %}
            {{- "

" }}
        {%- endif %}
        {{- "Available Tools:
" }}
        {%- for t in tools %}
            {{- t | tojson(indent=4) }}
            {{- "

" }}
        {%- endfor %}
    {%- endif %}
    {{- "<|eot_id|>" }}
{%- endif %}

{#- Rest of the messages #}
{%- for message in messages %}
    {#- The special cases are when the message is from a tool (via role ipython/tool/tool_results) or when the message is from the assistant, but has "tool_calls". If not, we add the message directly as usual. #}
    {#- Case 1 - Usual, non tool related message. #}
    {%- if not (message.role == "ipython" or message.role == "tool" or message.role == "tool_results" or (message.tool_calls is defined and message.tool_calls is not none)) %}
        {{- '<|start_header_id|>' + message['role'] + '<|end_header_id|>

' }}
        {%- if message['content'] is string %}
            {{- message['content'] | trim }}
        {%- else %}
            {%- for item in message['content'] %}
                {%- if item.type == 'text' %}
                    {{- item.text | trim }}
                {%- endif %}
            {%- endfor %}
        {%- endif %}
        {{- '<|eot_id|>' }}
    
    {#- Case 2 - the response is from the assistant, but has a tool call returned. The assistant may also have returned some content along with the tool call. #}
    {%- elif message.tool_calls is defined and message.tool_calls is not none %}
        {{- "<|start_header_id|>assistant<|end_header_id|>

" }}
        {%- if message['content'] is string %}
            {{- message['content'] | trim }}
        {%- else %}
            {%- for item in message['content'] %}
                {%- if item.type == 'text' %}
                    {{- item.text | trim }}
                    {%- if item.text | trim != "" %}
                        {{- "

" }}
                    {%- endif %}
                {%- endif %}
            {%- endfor %}
        {%- endif %}
        {{- "[" }}
        {%- for tool_call in message.tool_calls %}
            {%- set out = tool_call.function|tojson %}
            {%- if not tool_call.id is defined %}
                {{- out }}
            {%- else %}
                {{- out[:-1] }}
                {{- ', "id": "' + tool_call.id + '"}' }}
            {%- endif %}
            {%- if not loop.last %}
                {{- ", " }}
            {%- else %}
                {{- "]<|eot_id|>" }}
            {%- endif %}
        {%- endfor %}
    
    {#- Case 3 - the response is from a tool call. The tool call may have an id associated with it as well. If it does, we add it to the prompt. #}
    {%- elif message.role == "ipython" or message["role"] == "tool_results" or message["role"] == "tool" %}
        {{- "<|start_header_id|>ipython<|end_header_id|>

" }}
        {%- if message.tool_call_id is defined and message.tool_call_id != '' %}
            {{- '{"content": ' + (message.content | tojson) + ', "call_id": "' + message.tool_call_id + '"}' }}
        {%- else %}
            {{- '{"content": ' + (message.content | tojson) + '}' }}
        {%- endif %}
        {{- "<|eot_id|>" }}
    {%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
    {{- '<|start_header_id|>assistant<|end_header_id|>

' }}
{%- endif %}

2. Exaone-Deep-2 4b
    link:
    https://huggingface.co/bartowski/LGAI-EXAONE_EXAONE-Deep-2.4B-GGUF/resolve/main/LGAI-EXAONE_EXAONE-Deep-2.4B-Q4_K_M.gguf

    Chat Template:
    {%- for message in messages -%}
	{%- if loop.first and message["role"] != "system" -%}
		{{- "[|system|][|endofturn|]\n" -}}
	{%- endif -%}
	{%- set content = message["content"] -%}
	{%- if "</thought>" in content -%}
		{%- set content = (content.split("</thought>") | last).lstrip("\n") -%}
	{%- endif -%}
	{{- "[|" + message["role"] + "|]" + content -}}
	{%- if not (message["role"] == "user") -%}
		{{- "[|endofturn|]" -}}
	{%- endif -%}
	{%- if not loop.last -%}
		{{- "\n" -}}
	{%- endif -%}
{%- endfor -%}
{%- if add_generation_prompt -%}
	{{- "\n[|assistant|]<thought>\n" -}}
{%- endif -%}




3. DeepHermes-3-llama-3-3b
    Link:
    https://huggingface.co/bartowski/NousResearch_DeepHermes-3-Llama-3-3B-Preview-GGUF/resolve/main/NousResearch_DeepHermes-3-Llama-3-3B-Preview-Q4_K_M.gguf

    Chat Template:
    {% if not add_generation_prompt is defined %}{% set add_generation_prompt = false %}{% endif %}{% set loop_messages = messages %}{% for message in loop_messages %}{% set content = '<|start_header_id|>' + message['role'] + '<|end_header_id|>

'+ message['content'] | trim + '<|eot_id|>' %}{% if loop.index0 == 0 %}{% set content = bos_token + content %}{% endif %}{{ content }}{% endfor %}{% if add_generation_prompt %}{{ '<|start_header_id|>assistant<|end_header_id|>

' }}{% endif %}



4. Qwen3-4b-Thinking
    Link:
    https://huggingface.co/bartowski/Qwen_Qwen3-4B-Thinking-2507-GGUF/resolve/main/Qwen_Qwen3-4B-Thinking-2507-Q4_K_M.gguf


    Chat Template:
    {%- if tools %}
    {{- '<|im_start|>system\n' }}
    {%- if messages[0].role == 'system' %}
        {{- messages[0].content + '\n\n' }}
    {%- endif %}
    {{- "# Tools\n\nYou may call one or more functions to assist with the user query.\n\nYou are provided with function signatures within <tools></tools> XML tags:\n<tools>" }}
    {%- for tool in tools %}
        {{- "\n" }}
        {{- tool | tojson }}
    {%- endfor %}
    {{- "\n</tools>\n\nFor each function call, return a json object with function name and arguments within <tool_call></tool_call> XML tags:\n<tool_call>\n{\"name\": <function-name>, \"arguments\": <args-json-object>}\n</tool_call><|im_end|>\n" }}
{%- else %}
    {%- if messages[0].role == 'system' %}
        {{- '<|im_start|>system\n' + messages[0].content + '<|im_end|>\n' }}
    {%- endif %}
{%- endif %}
{%- set ns = namespace(multi_step_tool=true, last_query_index=messages|length - 1) %}
{%- for message in messages[::-1] %}
    {%- set index = (messages|length - 1) - loop.index0 %}
    {%- if ns.multi_step_tool and message.role == "user" and message.content is string and not(message.content.startswith('<tool_response>') and message.content.endswith('</tool_response>')) %}
        {%- set ns.multi_step_tool = false %}
        {%- set ns.last_query_index = index %}
    {%- endif %}
{%- endfor %}
{%- for message in messages %}
    {%- if message.content is string %}
        {%- set content = message.content %}
    {%- else %}
        {%- set content = '' %}
    {%- endif %}
    {%- if (message.role == "user") or (message.role == "system" and not loop.first) %}
        {{- '<|im_start|>' + message.role + '\n' + content + '<|im_end|>' + '\n' }}
    {%- elif message.role == "assistant" %}
        {%- set reasoning_content = '' %}
        {%- if message.reasoning_content is string %}
            {%- set reasoning_content = message.reasoning_content %}
        {%- else %}
            {%- if '</think>' in content %}
                {%- set reasoning_content = content.split('</think>')[0].rstrip('\n').split('<think>')[-1].lstrip('\n') %}
                {%- set content = content.split('</think>')[-1].lstrip('\n') %}
            {%- endif %}
        {%- endif %}
        {%- if loop.index0 > ns.last_query_index %}
            {%- if loop.last or (not loop.last and reasoning_content) %}
                {{- '<|im_start|>' + message.role + '\n<think>\n' + reasoning_content.strip('\n') + '\n</think>\n\n' + content.lstrip('\n') }}
            {%- else %}
                {{- '<|im_start|>' + message.role + '\n' + content }}
            {%- endif %}
        {%- else %}
            {{- '<|im_start|>' + message.role + '\n' + content }}
        {%- endif %}
        {%- if message.tool_calls %}
            {%- for tool_call in message.tool_calls %}
                {%- if (loop.first and content) or (not loop.first) %}
                    {{- '\n' }}
                {%- endif %}
                {%- if tool_call.function %}
                    {%- set tool_call = tool_call.function %}
                {%- endif %}
                {{- '<tool_call>\n{"name": "' }}
                {{- tool_call.name }}
                {{- '", "arguments": ' }}
                {%- if tool_call.arguments is string %}
                    {{- tool_call.arguments }}
                {%- else %}
                    {{- tool_call.arguments | tojson }}
                {%- endif %}
                {{- '}\n</tool_call>' }}
            {%- endfor %}
        {%- endif %}
        {{- '<|im_end|>\n' }}
    {%- elif message.role == "tool" %}
        {%- if loop.first or (messages[loop.index0 - 1].role != "tool") %}
            {{- '<|im_start|>user' }}
        {%- endif %}
        {{- '\n<tool_response>\n' }}
        {{- content }}
        {{- '\n</tool_response>' }}
        {%- if loop.last or (messages[loop.index0 + 1].role != "tool") %}
            {{- '<|im_end|>\n' }}
        {%- endif %}
    {%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
    {{- '<|im_start|>assistant\n<think>\n' }}
{%- endif %}


5. Gemma-3n-E2B (supports vision out of the box, natively multimodal)
    link:
    https://huggingface.co/bartowski/google_gemma-3n-E2B-it-GGUF/resolve/main/google_gemma-3n-E2B-it-Q4_K_M.gguf

    Chat Template:
    {{ bos_token }}
{%- if messages[0]['role'] == 'system' -%}
    {%- if messages[0]['content'] is string -%}
        {%- set first_user_prefix = messages[0]['content'] + '

' -%}
    {%- else -%}
        {%- set first_user_prefix = messages[0]['content'][0]['text'] + '

' -%}
    {%- endif -%}
    {%- set loop_messages = messages[1:] -%}
{%- else -%}
    {%- set first_user_prefix = "" -%}
    {%- set loop_messages = messages -%}
{%- endif -%}
{%- for message in loop_messages -%}
    {%- if (message['role'] == 'user') != (loop.index0 % 2 == 0) -%}
        {{ raise_exception("Conversation roles must alternate user/assistant/user/assistant/...") }}
    {%- endif -%}
    {%- if (message['role'] == 'assistant') -%}
        {%- set role = "model" -%}
    {%- else -%}
        {%- set role = message['role'] -%}
    {%- endif -%}
    {{ '<start_of_turn>' + role + '
' + (first_user_prefix if loop.first else "") }}
    {%- if message['content'] is string -%}
        {{ message['content'] | trim }}
    {%- elif message['content'] is iterable -%}
        {%- for item in message['content'] -%}
            {%- if item['type'] == 'audio' -%}
                {{ '<audio_soft_token>' }}
            {%- elif item['type'] == 'image' -%}
                {{ '<image_soft_token>' }}
            {%- elif item['type'] == 'text' -%}
                {{ item['text'] | trim }}
            {%- endif -%}
        {%- endfor -%}
    {%- else -%}
        {{ raise_exception("Invalid content type") }}
    {%- endif -%}
    {{ '<end_of_turn>
' }}
{%- endfor -%}
{%- if add_generation_prompt -%}
    {{'<start_of_turn>model
'}}
{%- endif -%}
