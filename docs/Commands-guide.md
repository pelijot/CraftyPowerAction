# Commands Guide

This document outlines the available commands for the CraftyPowerAction plugin for Velocity, which allows server
administrators to manage Minecraft servers through in-game commands.

## Command Overview

All CraftyPowerAction commands use the base command:

```
/craftypoweraction
```

For convenience, the plugin also supports the shortened alias:

```
/cpa
```

## Permission Requirements

All commands require the following permission:

```
craftypoweraction.use
```

## Available Commands

### Reload Configuration

Reloads the plugin's configuration from disk.

```
/craftypoweraction reload
```

**Aliases:** `/cpa reload`

**Description:**  
This command reloads the plugin's configuration file, allowing you to apply changes without restarting the proxy server.
Any modifications to the configuration file will take effect immediately after running this command.

**Notes:**

- This command only reloads the plugin's configuration, not Velocity's configuration.
- If you've added new servers to `velocity.toml`, you'll need to reload Velocity's configuration separately.

---

### Shutdown Empty Servers

Manually shuts down servers with no players.

```
/craftypoweraction clear [delay=0]
```

**Aliases:** `/cpa clear [delay=0]`

**Parameters:**

- `delay` (optional): Time in seconds to wait before shutting down servers. Defaults to 0 if not specified.

**Description:**  
This command checks the player count on all configured servers and sends stop signals to any empty servers after the
specified delay. This is useful for manually freeing up resources when servers are not in use.

**Examples:**

- `/cpa clear` - Immediately shut down all empty servers
- `/cpa clear 30` - Shut down all servers after a 30-second delay if they're still empty

---

### Run Diagnostic Checks

Validates the plugin's configuration and performs diagnostic checks.

```
/craftypoweraction doctor
```

**Aliases:** `/cpa doctor`

**Description:**  
The `doctor` command is a troubleshooting tool that performs a series of diagnostic checks on your
CraftyPowerAction setup.
Running this command can help identify and resolve potential issues with your configuration.
