# skript-variable-dump

[![](https://jitpack.io/v/goingoffskript/skript-variable-dump.svg)](https://jitpack.io/#goingoffskript/skript-variable-dump "Get maven artifacts on JitPack")
[![](https://img.shields.io/badge/License-MIT-blue)](./LICENSE "Project license: MIT")
[![](https://img.shields.io/badge/Java-8-orange)](# "This project targets Java 8")
[![](https://img.shields.io/badge/View-Javadocs-%234D7A97)](https://javadoc.jitpack.io/com/github/goingoffskript/skript-variable-dump/latest/javadoc/ "View javadocs")

Dump [Skript](https://github.com/SkriptLang/Skript/) variables to YAML.

## Instructions

Simply install this plugin on a server already running Skript and run:
**/skript-variable-dump**

All loaded variables will be dumped into a YAML file generated at:
`/plugins/Skript/dumps/`

That's all.

## Rationale

This tool exists so script authors who intend to rewrite their scripts
as plugins have an easy way to export their variable data and import it
however they see fit.

**Goals**

- Export Skript variables to platform-agnostic formats.
  - That way, data stored as variables is portable and accessible
    outside of Skript's opaque, effectively-proprietary storage format.
  - Use case: rewriting a script as a plugin and importing the
    script's legacy data.

**Non-goals**

- Importing previously dumped variables.
  - This tool is meant to be a one-way conversion to get Skript data
    elsewhere, and since the data itself *originates* in Skript, importing
    previous dumps is considered outside this project's scope.

## Example

📜 ➡️ 📑

- Variables: `/plugins/Skript/variables.csv`

```vb
# === Skript's variable storage ===
# Please do not modify this file manually!
#
# version: 2.5.3

numbers::1, string, 80036F6E65
numbers::2, string, 800374776F
numbers::3, string, 80057468726565
numbers::4, long, 0000000000000004
numbers::5, long, 0000000000000005
numbers::6, long, 0000000000000006
numbers::7, double, 401E000000000000
numbers::8, double, 4020666666666666
numbers::9, double, 4022333333333333
wait, timespan, 81066D696C6C6973040000000000000BB8
```

- Run: **/skript-variable-dump**
- Generates: `/plugins/Skript/dumps/skript-variables-dump.2021-05-12_1.yml`

```yaml
# Skript Variable Dump: 2021-05-12T16:53:30.636775
# Skript Version: 2.5.3

numbers:
  '1': one
  '2': two
  '3': three
  '4': 4
  '5': 5
  '6': 6
  '7': 7.5
  '8': 8.2
  '9': 9.1
wait:
  ==: ch.njol.skript.util.Timespan
  milliseconds: 3000
```

## Addons

Addons can register adapters for their data types with:

```java
import com.github.goingoffskript.skriptvariabledump.SkriptToYaml;

SkriptToYaml.adapts(ExampleType.class, (example, map) -> {
    map.put("thing", example.thing());
    map.put("amount", example.amount());
});
```

### Dependency

Get it from JitPack: https://jitpack.io/#goingoffskript/skript-variable-dump

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.goingoffskript</groupId>
        <artifactId>skript-variable-dump</artifactId>
        <version><!-- Released Version --></version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
