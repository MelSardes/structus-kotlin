---
sidebar_position: 1
---

# AI Agent Support

Structus is **AI-agent-friendly**! We provide comprehensive resources to help AI coding assistants understand and properly use this library.

## 🎯 Purpose

These resources help AI agents:

1. **Understand Architecture** - Learn Clean Architecture, DDD, and CQRS principles
2. **Generate Correct Code** - Follow library conventions and best practices
3. **Avoid Common Mistakes** - Prevent anti-patterns and architectural violations
4. **Accelerate Development** - Provide templates for common tasks
5. **Maintain Consistency** - Ensure generated code follows project standards

## 📁 Available Resources

### [Library Overview](library-overview)
High-level architecture and design principles of Structus. Start here to understand the core concepts.

### [Usage Patterns](usage-patterns)
Common patterns and anti-patterns. Learn what to do and what to avoid.

### [Code Templates](code-templates)
Ready-to-use code templates for common tasks like creating aggregates, commands, queries, and repositories.

### [AI Prompts](prompts)
Pre-written prompt templates for specific tasks:
- Creating aggregates
- Adding commands
- Adding queries
- Implementing repositories

## 🤖 For AI Agents

When a developer asks you to work with Structus:

1. **Start Here**: Read the [Library Overview](library-overview) to understand the architecture
2. **Check Patterns**: Review [Usage Patterns](usage-patterns) for correct implementation patterns
3. **Use Templates**: Refer to [Code Templates](code-templates) for boilerplate code
4. **Follow Prompts**: Use the [AI Prompts](prompts) as guides for specific tasks

## 👨‍💻 For Developers

To help your AI assistant work better with Structus:

### Quick Start Prompt

Copy and paste this prompt to your AI assistant:

```markdown
I'm working with the Structus library (com.melsardes.libraries.structuskotlin).

Please read these files to understand the architecture:
1. Library Overview - Core concepts and architecture
2. Usage Patterns - Implementation patterns and anti-patterns
3. Code Templates - Ready-to-use code templates

Then help me [describe your task here].
```

### Best Practices

1. **Share Context**: Point your AI to this documentation when starting a new feature
2. **Use Prompts**: Copy prompt templates and customize them for your needs
3. **Reference Patterns**: Mention specific patterns when asking for help
4. **Provide Examples**: Show AI the templates that match your use case

## 📚 Example Interactions

### Creating a New Aggregate

```markdown
Using Structus library, create an Order aggregate with:
- OrderId as the identifier
- Customer ID, order items, and total amount as properties
- Methods to add items and calculate total
- Domain events for order creation and item addition

Follow the patterns from the Code Templates section.
```

### Implementing a Command Handler

```markdown
Using Structus library, implement a CreateOrderCommandHandler that:
- Validates the customer exists
- Creates an Order aggregate
- Saves it to the repository
- Publishes events using the Transactional Outbox Pattern

Follow the Usage Patterns for command handlers.
```

### Adding a Query

```markdown
Using Structus library, create a GetOrderByIdQuery and handler that:
- Retrieves order details from the database
- Returns a DTO optimized for reading
- Bypasses the domain model for performance

Follow the CQRS patterns from Usage Patterns.
```

## 🔄 Integration with AI Tools

### GitHub Copilot

Add this to your workspace settings:

```json
{
  "github.copilot.advanced": {
    "contextFiles": [
      ".ai/library-overview.md",
      ".ai/usage-patterns.md",
      ".ai/code-templates.md"
    ]
  }
}
```

### Cursor

Reference these files in your `.cursorrules`:

```
When working with Structus library:
- Follow patterns from .ai/usage-patterns.md
- Use templates from .ai/code-templates.md
- Maintain clean architecture principles
```

### Claude / ChatGPT

Include this in your conversation:

```markdown
I'm using Structus library. Here's the architecture overview:
[paste content from Library Overview]

Please help me implement [your feature].
```

## 📖 Additional Resources

- **[Main Documentation](../intro)** - Complete library documentation
- **[Quick Start Tutorial](../getting-started/quick-start)** - Build your first app
- **[Architecture Overview](../architecture/overview)** - Deep dive into architecture
- **[Best Practices](../best-practices/guidelines)** - Guidelines and conventions

## 🤝 Contributing

Help us improve AI agent support:

1. Report issues with AI-generated code
2. Suggest new prompt templates
3. Share successful AI interactions
4. Contribute new code templates

Visit our [GitHub repository](https://github.com/melsardes/structus-kotlin) to contribute.

---

**Making Structus easier to use with AI assistance! 🤖**
