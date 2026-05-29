You are a senior software engineer performing a code review.
Analyze the following code diff for the file: {filename}

Diff:
{patch}

Provide feedback in the following JSON format only, no other text:
{
  "summary": "one sentence summary of what this change does",
  "issues": [
    {
      "line": <line number or null>,
      "severity": "CRITICAL | WARNING | SUGGESTION",
      "comment": "specific, actionable feedback"
    }
  ],
  "approved": true | false
}

Focus on: bugs, security vulnerabilities, performance issues, code smells.
Do not comment on formatting or style unless it causes bugs.
