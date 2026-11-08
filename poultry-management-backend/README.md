# Poultry Management System: Get It Running on Windows (Beginner Guide)

> **Educational prototype** for the project *"Poultry Management System: A Case of Kenchic"*.
> It is **not** an official Kenchic system. The sample data is made up and labelled `DEMO`. There is no login, so please do not type in confidential information.

This guide takes you from **a Windows computer with nothing installed** to **the app running in your browser**. You do not need to have done anything like this before. Go slowly, do the steps in order, and tick off each "✅ Check" before moving on.

Repository: <https://github.com/devharnold/poultry-management-sys>

---

## What you are about to do (the big picture)

The app has **two parts** that run at the same time on your own computer:

| Part | What it is | Technology | Runs at |
|---|---|---|---|
| **Backend** (`poultry-management-backend`) | The "brain": stores data and applies the rules | Java 17 + Spring Boot + SQLite | `http://localhost:8080` |
| **Frontend** (`poultry-management-frontend`) | The website you click on | React + Vite | `http://localhost:5173` |

So you will use **two terminal windows**: one keeps the backend running, the other keeps the frontend running. You look at the app in your normal browser.

Steps: **1.** Install 3 programs → **2.** Download ("clone") the project → **3.** Start the backend → **4.** Start the frontend → **5.** Open it in the browser.

**Time needed:** about 30–45 minutes the first time (mostly downloads). **Internet is needed** the first time.

### Words you will meet

| Word | Plain meaning |
|---|---|
| **Git** | A program that downloads and tracks project files. |
| **GitHub** | A website that stores projects online. Our project lives there. |
| **Repository ("repo")** | The project folder stored on GitHub. |
| **Clone** | Download a full copy of the repo to your computer. |
| **Terminal / PowerShell** | A window where you type commands instead of clicking. |
| **JDK 17** | The Java toolkit needed to run the backend. |
| **Node.js / npm** | The toolkit needed to run the frontend. `npm` downloads its libraries. |
| **localhost** | "This computer". `localhost:5173` means "a page running on my own computer". |

---

## Part 1: Install the programs

### 1.1 Install Git

1. Open your browser and go to **<https://git-scm.com/download/win>**. The download should start by itself. If it does not, click **"Click here to download"**. Choose **"64-bit Git for Windows Setup"** (almost every modern computer is 64-bit).
2. Open the downloaded file (it looks like `Git-2.xx.x-64-bit.exe`). If Windows asks *"Do you want to allow this app to make changes?"*, click **Yes**.
3. The installer shows many screens. **You can click "Next" on every screen and leave all the default choices alone**, then click **Install**, then **Finish**. (If you are curious: the defaults are safe for this project.)
4. When it finishes, you can untick "View Release Notes" and click **Finish**.

### 1.2 Open PowerShell (your terminal)

1. Press the **Windows key** on your keyboard, type **`PowerShell`**, and press **Enter**.
   A window with a dark blue (or black) background opens. This is where you type commands.

> **Handy tips for the terminal**
> * Type a command and press **Enter** to run it.
> * To **paste**, **right-click** inside the window (or press `Ctrl+V`).
> * Press the **Up arrow** to bring back the previous command.
> * Press **Tab** while typing a folder name to auto-complete it.
> * **`Ctrl+C`** stops a program that is running.
> * Commands are shown in grey boxes below. Type (or paste) **only the command**, not any `#` comment after it.

### 1.3 Check that Git installed

In PowerShell type:

```powershell
git --version
```

✅ **Check:** you see something like `git version 2.47.0.windows.1`. (Your numbers may differ. That is fine.)

❌ If you see *"git is not recognized…"*: **close PowerShell completely and open a new one** (a new window is needed to notice the new program). Try again. If it still fails, restart the computer and try once more.

### 1.4 Tell Git who you are (one time, optional for just cloning)

Git likes to know your name. Replace the text inside the quotes with your own details:

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

Nothing visible happens when it works. That is normal.

### 1.5 Install Java (JDK 17)

The backend **needs Java version 17**. Other versions can cause confusing errors, so please use 17.

1. Go to **<https://adoptium.net/temurin/releases/>**.
2. Choose: **Operating System: Windows**, **Architecture: x64**, **Package Type: JDK**, **Version: 17**. Download the **`.msi`** installer.
3. Run it. When you reach the **"Custom Setup"** screen, click the small drive icon next to each of these and choose **"Will be installed on local hard drive"**:
   * **Add to PATH**
   * **Set JAVA_HOME variable**
   Then click **Next → Install → Finish**.
4. **Close PowerShell and open a new one**, then check:

```powershell
java -version
```

✅ **Check:** the first line mentions **`17`**, for example `openjdk version "17.0.13" …`.

❌ If it says **21** or another number, you have another Java installed that is winning. Ask for help, or uninstall the other Java from *Settings → Apps*, then open a new PowerShell and check again.

### 1.6 Install Node.js

1. Go to **<https://nodejs.org>** and click the button labelled **LTS** ("Long Term Support"). Download and run the installer.
2. Click **Next** on every screen (defaults are fine). If it offers *"Automatically install the necessary tools"* you can leave that **unticked**.
3. **Close PowerShell and open a new one**, then check:

```powershell
node --version
npm --version
```

✅ **Check:** `node --version` shows **v20.19 or higher** (for example `v22.12.0` or `v24.x`). `npm --version` shows a number.

❌ If you see an error like *"npm.ps1 cannot be loaded because running scripts is disabled on this system"*: type this once, answer **Y**, then retry:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

> **Shortcut for later (optional):** Windows 10/11 also has a tool called `winget` that can install all three programs with commands. The steps above work for everyone, so we use those.

---

## Part 2: Download (clone) the project

### 2.1 Choose where to put it

**Do not** put the project inside OneDrive, Documents or Desktop if they are synced by OneDrive. Syncing can lock the database file and cause strange errors. A plain folder such as **`C:\dev`** is best.

In PowerShell:

```powershell
cd C:\
mkdir dev
cd C:\dev
```

(If it says the folder already exists, that is fine. Just run the `cd C:\dev` line.)

### 2.2 Clone it

```powershell
git clone https://github.com/devharnold/poultry-management-sys.git
```

You will see lines such as `Cloning into 'poultry-management-sys'…` and a progress counter.

✅ **Check:** it ends without a red error. Now go into the folder and look inside:

```powershell
cd poultry-management-sys
dir
```

You should see these two folders (plus this `README.md`):

```
poultry-management-backend
poultry-management-frontend
```

❌ **"repository not found" or a login window appears:** the project may be **private**. Sign in to GitHub in the window that opens, using the GitHub account that was invited. If it still says *not found*, ask Henry to add your GitHub username as a **collaborator** on the repository.

❌ **You see different folder names** (for example everything is in one folder): tell Henry. These instructions assume the two folders above.

---

## Part 3: Start the backend (terminal window #1)

Stay in PowerShell and go into the backend folder:

```powershell
cd C:\dev\poultry-management-sys\poultry-management-backend
```

Start it:

```powershell
.\mvnw.cmd spring-boot:run
```

**What happens:** the very first time, it downloads Maven (a build tool) and then all the libraries the backend needs. This can take **5–10 minutes** and prints a lot of text. That is normal. **Do not close the window.**

✅ **Check:** after a while you see a line containing **`Started PoultryManagementApplication`**. The window then stays still. That means the backend is running.

Now confirm it really works: open your browser and visit

**<http://localhost:8080/api/health>**

✅ You should see text like `{"service":"poultry-management-backend",…,"status":"UP","database":"UP"}`.

> **Windows may show a popup** saying *"Windows Defender Firewall has blocked some features of Java"*. Everything here runs on your own computer, so either **Cancel** or **Allow** is fine.

> **Leave this window open and running.** If you close it, the backend stops.

---

## Part 4: Start the frontend (terminal window #2)

1. Open a **second, new PowerShell window** (Windows key → type `PowerShell` → Enter). Keep the first one running.
2. Go to the frontend folder:

```powershell
cd C:\dev\poultry-management-sys\poultry-management-frontend
```

3. Download its libraries (first time only, about 1–3 minutes):

```powershell
npm install
```

You may see some "deprecated" or "audit" warnings. Those are normal and do not stop it. It is done when you get your prompt (`PS C:\…>`) back.

4. Start it:

```powershell
npm run dev
```

✅ **Check:** you see something like:

```
  VITE v7.x.x  ready in 500 ms
  ➜  Local:   http://localhost:5173/
```

> **Leave this window open too.**

❌ If it says **port 5173 is already in use**, a previous copy is still running. Close the old PowerShell windows (or press `Ctrl+C` in them) and try again. Please don't use a different port, because the backend only accepts `http://localhost:5173`.

---

## Part 5: Use the app

Open your browser and go to **<http://localhost:5173>**. Type it exactly like that.

✅ At the top you should see **"Server connected"** (green dot). If it says **"Server unreachable"**, the backend (Part 3) is not running.

### A quick tour (5 minutes)

1. **Dashboard:** it is empty at first. Click **"Load synthetic demo data"**. This adds clearly-labelled **DEMO** farms, flocks, feed and alerts, so there is something to look at. It can only be loaded **once**.
2. **Flocks → DEMO-001:** shows birds alive, mortality history, and a **feed conversion ratio (FCR) of 1.63** with the formula. **DEMO-002** shows *"Not enough data yet"* because the app never guesses.
3. **Mortality:** choose a flock, enter a number of deaths, and press **Save mortality**.
4. **Feed:** see stock in **kilograms**. Try **Record usage**, then **Add stock**, then **History**.
5. **Alerts:** shows *high mortality* and *low feed stock* alerts. These are created by the app's own rules and are labelled **Prototype-generated**. Nothing is sent by SMS or email.
6. **Demo role** (top of the page): switching between Farm Personnel, Field Officer, Supervisor and Manager only changes which buttons you see. **It is not a real login or security.**

### Test the offline feature (2 minutes)

This is the part of the project that works when the connection drops:

1. Open the **Mortality** page and wait for the flock list to appear.
2. Go to **terminal window #1** (the backend) and press **`Ctrl+C`**. If it asks *"Terminate batch job (Y/N)?"*, type **`Y`** and press Enter. Within about 10 seconds the top of the page says **"Server unreachable"**.
3. Record some deaths. The entry shows **"Pending sync"** and is saved **on your computer**. Reload the page (`F5`): the entry is still listed on the **Sync** page.
4. Start the backend again (in window #1, run `.\mvnw.cmd spring-boot:run` again). Within about 10–30 seconds the entry changes to **"Synced"** by itself, and the Mortality history shows it **exactly once** (no duplicates).

---

## Part 6: Stopping and starting again

**To stop:** in each of the two PowerShell windows press **`Ctrl+C`** (answer **Y** if asked), then close the windows. Your data is **kept** in a file, so it will still be there next time.

**To start again later** (no more downloading, so it is quick):

| Window | Commands |
|---|---|
| **#1 Backend** | `cd C:\dev\poultry-management-sys\poultry-management-backend` then `.\mvnw.cmd spring-boot:run` |
| **#2 Frontend** | `cd C:\dev\poultry-management-sys\poultry-management-frontend` then `npm run dev` |

Then open <http://localhost:5173>.

### Getting the latest version of the project

When Henry updates the code on GitHub, in PowerShell run:

```powershell
cd C:\dev\poultry-management-sys
git pull
```

Then, if the frontend's libraries changed, run `npm install` again inside `poultry-management-frontend`. Restart both parts.

### Starting again with an empty database

> ⚠️ This **permanently deletes all data**, including anything you typed in.

1. Stop the backend (`Ctrl+C`).
2. Run:

```powershell
cd C:\dev\poultry-management-sys\poultry-management-backend
Remove-Item data\poultry.db
```

3. Start the backend again. You get a fresh, empty database (and you can load the demo data again).

The database is the single file `backend\data\poultry.db`. It is **not** uploaded to GitHub.

---

## Part 7 (optional): Run the automatic tests

Frontend (in window #2's folder, no backend needed):

```powershell
cd C:\dev\poultry-management-sys\poultry-management-frontend
npm test
```

✅ Expect **28 tests passing**.

Backend (in a PowerShell window, from the backend folder):

```powershell
cd C:\dev\poultry-management-sys\poultry-management-backend
.\mvnw.cmd test
```

> **Honest status note:** the author could not run the backend's Maven tests or start the Spring app on the computer where it was written (no internet there). The code was checked in other ways, but **you may be the first person to run `.\mvnw.cmd test`**. If something fails, that is genuinely useful information. Copy the **first error line** and send it to Henry. Do not worry that you did something wrong.

---

## Troubleshooting (find your message)

| What you see | What to do |
|---|---|
| `git is not recognized` / `java is not recognized` / `node is not recognized` | **Close PowerShell and open a new one.** Programs installed while a window was open are not noticed until a new window is opened. Still failing? Restart the computer. |
| `java -version` does not say 17 | Install **JDK 17** (Part 1.5). Another Java version is taking priority. Open a new PowerShell and re-check. |
| `npm.ps1 cannot be loaded because running scripts is disabled` | Run `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned`, answer **Y**, retry. |
| `git clone` says *repository not found* or asks to log in | The repo is private. Sign in with the invited GitHub account, or ask Henry to add you as a collaborator. |
| `.\mvnw.cmd` is "not recognized" | You are in the wrong folder. Run `dir`. You must see `mvnw.cmd`. `cd` into `poultry-management-backend`. |
| Maven or dependency **download fails** / stays stuck | Check your internet (school or office networks sometimes block downloads), then run the command again. |
| `The JAVA_HOME environment variable is not defined correctly` | Reinstall JDK 17 and tick **"Set JAVA_HOME variable"** (Part 1.5), then open a new PowerShell. |
| `Port 8080 was already in use` | Another program (or an old copy of this backend) is using it. Close the old PowerShell windows and try again. |
| `Port 5173 is already in use` | An old frontend is still running. Close its window (or `Ctrl+C`) and try again. |
| Page says **"Server unreachable"** | The backend (window #1) is not running or has not finished starting. Wait for `Started PoultryManagementApplication`, and check <http://localhost:8080/api/health>. |
| Browser console shows a **CORS** error | Open the app at exactly `http://localhost:5173` (not `127.0.0.1` with a different port, and not a network address). |
| Blank white page | Press **F12**, click the **Console** tab, and read the first red message. Send it to Henry. |
| `database is locked` or `unable to open database file` | Make sure only one backend is running, and that the project is **not inside OneDrive** (move it to `C:\dev`). |
| "Demo data was already loaded" | That is correct. It loads only once. To start again, see **"Starting again with an empty database"**. |
| The sample data disappeared | You reset the database, or deleted `backend\data\poultry.db`. |

### What to send Henry when you are stuck

It helps a lot to send **(1)** the exact command you ran, **(2)** the **first red error line**, and **(3)** the output of these:

```powershell
git --version
java -version
node --version
```

A screenshot of the PowerShell window works too.

---

## Part 8 (optional): If you also want to change the project

You do **not** need this to run the app. If you edit files and want to save your work with Git:

```powershell
cd C:\dev\poultry-management-sys
git status                      # shows what you changed
git add .                       # prepares all changes
git commit -m "Describe what you changed"
git push                        # uploads (only works if you have permission)
```

A good free editor for this is **Visual Studio Code** (<https://code.visualstudio.com>). Then **File → Open Folder → `C:\dev\poultry-management-sys`**.
If `git push` is refused, you may need to be added as a collaborator, or to work on your own copy ("fork") of the repository.

---

## Good to know (limitations of this prototype)

* **No login.** The "Demo role" picker only changes which buttons show. It does not protect anything. Don't expose the app to the internet or store real confidential data.
* Alerts, thresholds (the 0.50 % daily mortality alert level is a placeholder) and indicators like FCR are **unvalidated** and must be reviewed by qualified poultry professionals before any real use.
* Only **mortality** and **feed usage** work offline. Other actions need the backend.
* Mortality and feed records cannot be edited or deleted. Records are corrected by adding new ones, or by resetting the database.
* This is a learning prototype. It has not been tested with real users, and no usability score is claimed.

More technical detail (API list, formulas, how offline sync works) is in the README inside each folder:
`poultry-management-backend\README.md` and `poultry-management-frontend\README.md`.