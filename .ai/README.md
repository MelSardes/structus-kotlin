<div align="center">
  <img src="../structus-logo.svg" alt="Structus Logo" width="150"/>
  
  # AI Agent Support for Structus
</div>

This directory contains resources specifically designed to help AI coding assistants (like GitHub Copilot, Cursor, Claude, ChatGPT, etc.) understand and properly use the Structus library.

## 📁 Directory Structure

```
.ai/
├── README.md                    # This file
├── library-overview.md          # High-level architecture and design principles
├── usage-patterns.md            # Common patterns and anti-patterns
├── code-templates.md            # Ready-to-use code templates
├── integration-guide.md         # Framework integration examples
├── troubleshooting.md           # Common issues and solutions
├── metadata/                    # Machine-readable metadata
│   ├── components.json          # Component catalog
│   ├── patterns.json            # Design patterns
│   └── dependencies.json        # Dependency information
└── prompts/                     # AI prompt templates
    ├── create-aggregate.md      # Creating aggregates
    ├── add-command.md           # Adding commands
    ├── add-query.md             # Adding queries
    ├── implement-repository.md  # Implementing repositories
    └── add-event.md             # Adding domain events
```

## 🎯 Purpose

These resources help AI agents:

1. **Understand Architecture**: Learn Clean Architecture, DDD, and CQRS principles
2. **Generate Correct Code**: Follow library conventions and best practices
3. **Avoid Common Mistakes**: Prevent anti-patterns and architectural violations
4. **Accelerate Development**: Provide templates for common tasks
5. **Maintain Consistency**: Ensure generated code follows project standards

## 🤖 For AI Agents

When a developer asks you to work with Structus:

1. **Start Here**: Read `library-overview.md` to understand the architecture
2. **Check Patterns**: Review `usage-patterns.md` for correct implementation patterns
3. **Use Templates**: Refer to `code-templates.md` for boilerplate code
4. **Follow Prompts**: Use files in `prompts/` as guides for specific tasks
5. **Verify**: Check `troubleshooting.md` if something doesn't work

## 👨‍💻 For Developers

To help your AI assistant work better with Structus:

1. **Share Context**: Point your AI to this directory when starting a new feature
2. **Use Prompts**: Copy prompt templates from `prompts/` and customize them
3. **Reference Patterns**: Mention specific patterns from `usage-patterns.md`
4. **Provide Examples**: Show AI the templates from `code-templates.md`

## 📚 Quick Start for AI Agents

```markdown
I'm working with the Structus library. Please read the following files to understand the architecture:
1. .ai/library-overview.md - Core concepts
2. .ai/usage-patterns.md - Implementation patterns
3. .ai/code-templates.md - Code templates

Then help me [describe your task here].
```

## 🔄 Keeping Context Updated

This directory should be updated when:
- New architectural patterns are introduced
- Common issues are discovered
- New integration examples are added
- Library API changes significantly

## 📖 Additional Resources

- [Main Documentation](../README.md)
- [Getting Started Guide](../GETTING_STARTED.md)
- [API Reference](../wiki/Reference/API-Reference.md)
- [Best Practices](../wiki/Best%20Practices/Best-Practices.md)

---

**Note**: This is a living directory. Contributions to improve AI agent understanding are welcome!
