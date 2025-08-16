# PowerShell Scripts for Health Assistant System

This folder contains PowerShell equivalents of the bash scripts in the `scripts` folder, designed to work on Windows systems.

## Prerequisites

1. **PowerShell 5.1 or later** (included with Windows 10/11)
2. **Java Development Kit (JDK 8 or later)**
3. **Maven** (for building and running the Java application)
4. **Ollama** (for the LLM backend)

## Installation

### Install Ollama on Windows
```powershell
# Option 1: Download from https://ollama.ai/download
# Option 2: Use winget (if available)
winget install Ollama.Ollama
```

## Script Descriptions

### `setup-ollama.ps1`
Sets up Ollama and downloads the LLaMA 3.2 model. Run this first before using the system.

```powershell
.\scripts-powershell\setup-ollama.ps1
```

### Service Start Scripts
Start individual nodes of the Health Assistant system:

```powershell
# Terminal 1 - General Medicine Node (HTTP: 8080, Akka: 2551)
.\scripts-powershell\start-general-medicine.ps1

# Terminal 2 - Pharmacy Node (HTTP: 8081, Akka: 2552)
.\scripts-powershell\start-pharmacy.ps1

# Terminal 3 - Radiology Node (HTTP: 8082, Akka: 2553)
.\scripts-powershell\start-radiology.ps1
```

### Testing Scripts

#### `test-system.ps1`
Runs automated tests against the Health Assistant system:

```powershell
.\scripts-powershell\test-system.ps1
```

#### `interactive-test.ps1`
Interactive testing script that demonstrates Akka communication patterns:

```powershell
.\scripts-powershell\interactive-test.ps1
```

#### `monitor-cluster.ps1`
Monitors the health of all cluster nodes and services:

```powershell
.\scripts-powershell\monitor-cluster.ps1
```

## Usage Instructions

### 1. Initial Setup
```powershell
# Run setup (only needed once)
.\scripts-powershell\setup-ollama.ps1
```

### 2. Start the System
Open 3 separate PowerShell terminals and run:

**Terminal 1:**
```powershell
cd "D:\Study\Projects\AI_infra_final_test"
.\scripts-powershell\start-general-medicine.ps1
```

**Terminal 2:**
```powershell
cd "D:\Study\Projects\AI_infra_final_test"
.\scripts-powershell\start-pharmacy.ps1
```

**Terminal 3:**
```powershell
cd "D:\Study\Projects\AI_infra_final_test"
.\scripts-powershell\start-radiology.ps1
```

### 3. Test the System
In a 4th terminal:
```powershell
cd "D:\Study\Projects\AI_infra_final_test"
.\scripts-powershell\test-system.ps1
```

### 4. Monitor the Cluster
```powershell
.\scripts-powershell\monitor-cluster.ps1
```

## PowerShell-Specific Features

These scripts include Windows-specific improvements:

- **Colored output** using `Write-Host` with `-ForegroundColor`
- **Proper error handling** with try-catch blocks
- **Windows networking commands** (netstat instead of lsof)
- **PowerShell-native JSON handling** with `ConvertTo-Json` and `ConvertFrom-Json`
- **REST API calls** using `Invoke-RestMethod`
- **Environment variables** set with `$env:VARIABLE_NAME`

## Troubleshooting

### Common Issues

1. **Execution Policy Error:**
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

2. **Maven Command Issues:**
   If you get "Unknown lifecycle phase" errors, try:
   ```powershell
   # Alternative method using cmd
   cmd /c 'mvn exec:java -Dexec.mainClass="com.healthassistant.HealthAssistantApp" -Dexec.args="general-medicine"'
   
   # Or use the alternative script
   .\scripts-powershell\start-general-medicine-alt.ps1
   ```

3. **Port Already in Use:**
   ```powershell
   # Check what's using a port
   netstat -ano | findstr :8080
   
   # Kill a process by PID
   taskkill /PID <PID> /F
   ```

4. **Ollama Not Found:**
   - Ensure Ollama is installed and in your PATH
   - Restart PowerShell after installation

5. **Maven Not Found:**
   - Install Maven and ensure it's in your PATH
   - Verify with: `mvn --version`

### Viewing Logs
```powershell
# View application logs (if log file exists)
Get-Content -Path "logs\health-assistant.log" -Wait

# View Maven output
# The output will be displayed in the terminal where you ran the start scripts
```

## Differences from Bash Scripts

- Uses `Write-Host` instead of `echo` for colored output
- Uses `Invoke-RestMethod` instead of `curl`
- Uses `netstat` instead of `lsof` for port checking
- Uses PowerShell variables (`$variable`) instead of bash variables
- Uses `Start-Sleep` instead of `sleep`
- Uses `$env:` for environment variables instead of `export`

## Security Notes

- These scripts use `Invoke-RestMethod` which is generally safe for localhost connections
- No external network calls are made except to localhost services
- Scripts only modify environment variables for the current session
