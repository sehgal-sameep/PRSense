# PRSense

> AI-powered pull request review for Azure DevOps — automatically analyzes code diffs and posts contextual review comments using OpenAI GPT models.

---

## What It Does

PRSense integrates with your Azure DevOps pipeline and reviews pull requests the moment they are opened or updated. It reads the code diff, understands the context, and posts inline comments that mirror what a senior engineer would flag: logic errors, security issues, style violations, missing edge-case handling, and more.

No more waiting on human reviewers for first-pass feedback. PRSense gives developers immediate, actionable insights so that human review time is spent on higher-level concerns.

---

## Features

- **Diff-aware analysis** — understands changed lines in context, not just raw text
- **Inline PR comments** — posts comments directly on the relevant lines in Azure DevOps
- **GPT-powered review** — leverages OpenAI models for nuanced, language-agnostic feedback
- **Configurable focus areas** — tune the review to flag security issues, performance hotspots, test coverage gaps, or coding standards
- **Pipeline integration** — runs as a step in your existing Azure DevOps CI/CD pipeline

---

## How It Works

```
Pull Request opened / updated
        │
        ▼
Azure DevOps webhook / pipeline trigger
        │
        ▼
PRSense fetches the diff via Azure DevOps REST API
        │
        ▼
Diff chunks sent to OpenAI GPT for analysis
        │
        ▼
Review comments posted back to the PR via Azure DevOps API
```

---

## Prerequisites

- Python 3.10+
- An Azure DevOps organization and project
- An [Azure DevOps Personal Access Token (PAT)](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate) with `Code (Read)` and `Pull Request Threads (Read & Write)` scopes
- An OpenAI API key

---

## Installation

```bash
git clone https://github.com/your-org/PRSense.git
cd PRSense
pip install -r requirements.txt
```

---

## Configuration

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `AZURE_DEVOPS_ORG` | Your Azure DevOps organization name |
| `AZURE_DEVOPS_PROJECT` | Target project name |
| `AZURE_DEVOPS_PAT` | Personal Access Token |
| `OPENAI_API_KEY` | Your OpenAI API key |
| `OPENAI_MODEL` | Model to use (e.g. `gpt-4o`) |
| `REVIEW_FOCUS` | Comma-separated focus areas: `security,performance,style` |

---

## Usage

### Run manually against a PR

```bash
python prsense.py --pr <PR_ID>
```

### Azure DevOps Pipeline

Add PRSense as a pipeline step in your `azure-pipelines.yml`:

```yaml
- script: python prsense.py --pr $(System.PullRequest.PullRequestId)
  displayName: 'PRSense AI Review'
  env:
    AZURE_DEVOPS_PAT: $(AZURE_DEVOPS_PAT)
    OPENAI_API_KEY: $(OPENAI_API_KEY)
  condition: eq(variables['Build.Reason'], 'PullRequest')
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push and open a pull request

Please open an issue first for major changes

---

## License

[MIT](LICENSE)
