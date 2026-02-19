# Radiography CLI

Print the view hierarchy of any running Android app.

## Install

```bash
tar -xzf radiography-stoic-plugin-2.8-SNAPSHOT.tar.gz
export PATH="$PWD/radiography-stoic-plugin:$PATH"
```

Or install globally:

```bash
sudo tar -xzf radiography-stoic-plugin-2.8-SNAPSHOT.tar.gz -C /usr/local/bin --strip-components=1
```

## Usage

```bash
# Scan app
radiography com.example.myapp

# Include text content
radiography --pii com.example.myapp

# Scan only focused window
radiography --focused com.example.myapp

# Specific device
radiography -s emulator-5554 com.example.myapp
```

## Options

- `--pii` - Include text content
- `--focused` - Scan only focused window
- `-s SERIAL` - Use specific device
- `-d` - Use USB device
- `-e` - Use emulator

## Requirements

- [Stoic](https://github.com/block/stoic) installed
- Target app must be debuggable
- Android SDK 26+

---

Built on [Radiography](https://github.com/square/radiography) and [Stoic](https://github.com/block/stoic)
