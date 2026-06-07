--- section: kb-section ---
{snippet_section}<content>
{chunks_body}
</content>

--- section: snippet-rules ---
<rules>
{rules}
</rules>

--- section: mcp-section ---
{snippet_section}<data>
{body}
</data>

--- section: mcp-intent-rules ---
<rules>
{rules}
</rules>

--- section: mcp-error ---
<errors>
{error_list}
</errors>

--- section: sub-question-kb-wrapper ---
<document index="{index}">
<question>{question}</question>
{context}
</document>

--- section: sub-question-mcp-wrapper ---
<result index="{index}">
<question>{question}</question>
{context}
</result>

--- section: single-question ---
<question>{question}</question>

--- section: multi-questions ---
<questions>
{questions}
</questions>

--- section: kb-evidence ---
<documents>
{body}
</documents>

--- section: kb-citation-item ---
<citation index="{index}">
<title>{title}</title>
<source-url>{source_url}</source-url>
<chunk-index>{chunk_index}</chunk-index>
<score>{score}</score>
<excerpt>{excerpt}</excerpt>
</citation>

--- section: kb-citations ---
<citations>
{items}
</citations>

--- section: mcp-evidence ---
<tool-data>
{body}
</tool-data>

--- section: summary-wrapper ---
<conversation-summary>
{content}
</conversation-summary>

