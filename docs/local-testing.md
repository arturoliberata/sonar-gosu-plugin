# Testing the plugin in a local SonarQube

## 1. Get SonarQube and SonarScanner

Download from sonarsource.com:

- **SonarQube Community Build** (zip) and unzip it to a short path, e.g. `C:\sonar\sonarqube`.
  Long paths can break its search engine on Windows.
- **SonarScanner CLI** (zip) and unzip it next to it.

## 2. Install the plugin

```bash
./mvnw package
```

Copy `target/sonar-gosu-plugin-<version>.jar` to `<SONARQUBE_HOME>/extensions/plugins/`.
Keep only one version of the plugin there, or SonarQube refuses to start.

## 3. Start SonarQube

Recent SonarQube versions need **Java 21**. On Windows the start script **ignores `JAVA_HOME`**:
it uses `java.exe` from `PATH`, unless `SONAR_JAVA_PATH` points to a Java executable.

Windows (PowerShell):

```powershell
$env:SONAR_JAVA_PATH = "C:\Program Files\Eclipse Adoptium\jdk-21...\bin\java.exe"
<SONARQUBE_HOME>\bin\windows-x86-64\StartSonar.bat
```

macOS / Linux:

```bash
<SONARQUBE_HOME>/bin/<your-os>/sonar.sh console
```

Wait for `SonarQube is operational`, open http://localhost:9000 and log in with `admin` / `admin`
(you will be asked to change the password). `logs/web.log` should contain `Deploy Gosu / <version>`.

## 4. Scan a project

Create a token under **My Account → Security**. In the root of the Gosu project, create
`sonar-project.properties`:

```properties
sonar.projectKey=my-gosu-project
sonar.projectName=My Gosu Project
sonar.sources=.
sonar.sourceEncoding=UTF-8
sonar.inclusions=**/*.gs,**/*.gsx,**/*.gsp,**/*.gst
```

Then run the scanner from that folder. Passing the token through the `SONAR_TOKEN` environment
variable keeps it out of your shell history:

```bash
export SONAR_TOKEN=<your token>          # PowerShell: $env:SONAR_TOKEN = "<your token>"
sonar-scanner -Dsonar.host.url=http://localhost:9000
```

The [`sample-project`](../sample-project) folder is ready to scan and triggers every rule.
The official [Gosu starter projects](https://gosu-lang.github.io/docs.html) are another good small test:
they should raise 9 `PrintStatement` issues.

## 5. After changing the plugin

Stop SonarQube, rebuild, copy the new JAR over the old one, and start SonarQube again.
