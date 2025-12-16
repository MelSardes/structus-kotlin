---
sidebar_position: 1
---

# Interactive Demo

This interactive playground allows you to experiment with Structus code snippets directly in your browser. Try modifying the examples to see how Structus works in different scenarios.

:::note
The interactive code editor is loaded from an external service and requires JavaScript to be enabled.
:::

## Basic Example

<div class="codeplay-container">
<iframe class="codeplay-frame" src="https://play.kotlinlang.org/embed?short=owDmUgGSj&theme=darcula" allowfullscreen></iframe>
</div>

This example demonstrates the basic usage of Structus with an Order aggregate root, a command, and a command handler. You can modify the code to see how different patterns work.

## CQRS Example

<div class="codeplay-container">
<iframe class="codeplay-frame" src="https://play.kotlinlang.org/embed?short=AQWtouhtf&theme=darcula" allowfullscreen></iframe>
</div>

This example shows a more complete CQRS implementation with commands, queries, and handlers.

## Event Sourcing Example

<div class="codeplay-container">
<iframe class="codeplay-frame" src="https://play.kotlinlang.org/embed?short=PolftBTRw&theme=darcula" allowfullscreen></iframe>
</div>

This example demonstrates event sourcing with Structus, reconstructing an aggregate from its event history.

## Creating Your Own Playground

To create your own playground:

1. Go to [Kotlin Playground](https://play.kotlinlang.org/)
2. Add the Structus dependency using the Gradle tab:

```kotlin
dependencies {
    implementation("com.github.melsardes:structus-kotlin:0.1.0")
}
```

3. Write your Structus code
4. Share your playground using the "Share" button

## Embedding Guidelines

If you want to embed the Kotlin Playground in your own website or blog post, you can use the following HTML:

```html
<iframe 
  src="https://play.kotlinlang.org/embed?short=YOUR_SHORT_CODE&theme=darcula" 
  width="100%" 
  height="500px" 
  allowfullscreen
></iframe>
```

Replace `YOUR_SHORT_CODE` with the short code of your playground.

## Interactive Workshop

For a more guided experience, try our [Interactive Workshop](/docs/playground/interactive-workshop) which walks you through building a complete application with Structus step by step.

## Custom CSS

You can customize the appearance of the embedded playground by adding custom CSS to your website:

```css
.codeplay-container {
  margin: 2rem 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.codeplay-frame {
  width: 100%;
  height: 500px;
  border: none;
}
```

## Next Steps

Now that you've experimented with Structus in the playground, check out our [Code Snippets](code-snippets) to see more examples you can use in your own projects.